package com.lionsoft.jlogic;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    static Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    PasswordEncoder passwordEncoder;

	@Autowired
	private UserRepository userRepository;
 
    @Value("${maxSessions}")
    private int maxSessions;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
        http.headers().frameOptions().disable();

        http
            .csrf().disable()
            .httpBasic().and()
                .authorizeRequests()
                    .antMatchers("/css/**", "/js/**", "/webfonts/*", "/img/**").permitAll()
                    .antMatchers("/home", "/sessions").hasAuthority("VIEWER")
                    .antMatchers("/users").hasAuthority("ADMIN")
                    // Users
                    .antMatchers("/users", "/user/create", "/user/*/edit").hasAuthority("ADMIN")
                    .antMatchers("/user/edit").authenticated()
                    .antMatchers(HttpMethod.POST, "/user").hasAuthority("ADMIN")
                    .antMatchers(HttpMethod.DELETE, "/user/**").hasAuthority("ADMIN")
                    .antMatchers(HttpMethod.PUT, "/me").authenticated()
                    .antMatchers(HttpMethod.PUT, "/user/*").hasAuthority("ADMIN")
                    .antMatchers(HttpMethod.PUT, "/user/*").hasAuthority("ADMIN")

                    .antMatchers(HttpMethod.POST, "/program/**", "/properties/**").hasAuthority("EDITOR")
                    .antMatchers(HttpMethod.PUT, "/properties/**").hasAuthority("EDITOR")
                    .anyRequest().authenticated()
                    .and()
                    .formLogin().loginPage("/login").defaultSuccessUrl("/perform_login").permitAll()
                    .and()
                    .logout().invalidateHttpSession(true)/*.logoutUrl("/logout")*/.logoutSuccessUrl("/login?logout")
                             .deleteCookies("JSESSIONID")
                             .addLogoutHandler(logoutHandler())
                             /*.logoutSuccessHandler(logoutSuccessHandler()\/*new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK)*\/)*/
                             .permitAll();

        logger.info("Max number of sessions: "+maxSessions);

        http
            .sessionManagement()
            .maximumSessions(maxSessions)
            .maxSessionsPreventsLogin(true)
            .sessionRegistry(sessionRegistry())
            //.expiredUrl("/login?expired");
            .expiredUrl("/expired");
        /*
        http.sessionManagement()
          .expiredUrl("/login?expired")
        .invalidSessionUrl("/login?expired");*/
	}

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {

    /*
		  auth
			  .inMemoryAuthentication()
			    .passwordEncoder(passwordEncoder)
				  .withUser("admin").password(passwordEncoder.encode("admin")).roles("VIEWER", "EDITOR", "USER", "ADMIN");
	  */

		Optional<User> user = userRepository.findByUsername("admin");

		if (!user.isPresent()) {
		  logger.warn("Creating default admin user...");

		  User userAdmin = new User("admin", null, "Administrator", null);
		  //userAdmin.setUsername("admin");
		  userAdmin.setPassword(new BCryptPasswordEncoder().encode("admin"));
		  userAdmin.setReserved(true);
		  /*
		  userAdmin.grantAuthority(Role.ADMIN);
		  userAdmin.grantAuthority(Role.EDITOR);
		  userAdmin.grantAuthority(Role.VIEWER);
		  userAdmin.grantAuthority(Role.USER);
		  */
		  //userAdmin.grantAuthority(User.ROLES_ADMIN);
		  userAdmin.setRoleSet(User.ROLE_SET_ADMIN);
		  userRepository.save(userAdmin);
		}

	  auth.authenticationProvider(authenticationProvider());
	}

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder);
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }
    
    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public static ServletListenerRegistrationBean httpSessionEventPublisher() {	//(5)
        return new ServletListenerRegistrationBean(new HttpSessionEventPublisher());
    }

    @Bean
    public LogoutHandler logoutHandler() {
        return new CustomLogoutHandler();
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return new CustomLogoutSuccessHandler();
    }
}
