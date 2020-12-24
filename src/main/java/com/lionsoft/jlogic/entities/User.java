package com.lionsoft.jlogic;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

@Entity
public class User implements UserDetails {
  
    public static final int ROLE_SET_ADMIN = 0;
    public static final int ROLE_SET_EDITOR = 1;
    public static final int ROLE_SET_VIEWER = 2;
    public static final int ROLE_SET_USER = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String username;
    private String password;
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private boolean enabled;

    private String firstName;
    private String lastName;
    private Date creationTime;
    private boolean reserved;
    private int roleSetId; // Defines a set of authorities
  
    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.EAGER)
    private List<Role> roles; // Single authorities

    public User() {
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialsNonExpired = true;
        this.enabled = true;
        creationTime = new Date();
        this.reserved = false;
    }
    
    public User(String username, String password, String firstName, String lastName/*, int roleId*/) {
      this();
      
      this.username = username;
      this.password = password;
      this.firstName = firstName;
      this.lastName = lastName;
      //this.roleId = roleId;
    }
  
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public void setLocked(boolean locked) {
        accountNonLocked = !locked;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void grantAuthority(Role authority) {
        if (roles == null) 
          roles = new ArrayList<>();
          
        if (!roles.contains(authority))
          roles.add(authority);
    }

    public void grantAuthority(Role[] authorities) {
        for (Role r : authorities)
          grantAuthority(r);
    }

    @Override
    public List<GrantedAuthority> getAuthorities(){
        List<GrantedAuthority> authorities = new ArrayList<>();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role.toString())));
        return authorities;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String x) {
        firstName = x;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String x) {
        lastName = x;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setReserved(boolean flag) {
        reserved = flag;
    }

    public boolean getReserved() {
        return reserved;
    }

    public void setRoleSet(int id) {
        roleSetId = id;

        switch (id) {
          case ROLE_SET_ADMIN:
            grantAuthority(Role.ADMIN);
            grantAuthority(Role.EDITOR);
            grantAuthority(Role.VIEWER);
            grantAuthority(Role.USER);
            break;
            
          case ROLE_SET_EDITOR:
            grantAuthority(Role.EDITOR);
            grantAuthority(Role.VIEWER);
            grantAuthority(Role.USER);
            break;
            
          case ROLE_SET_VIEWER:
            grantAuthority(Role.VIEWER);
            break;
            
          case ROLE_SET_USER:
            grantAuthority(Role.USER);
            break;
            
          default:
            grantAuthority(Role.VIEWER);
            break;
        }
    }

    public int getRoleSet() {
        return roleSetId;
    }

}
