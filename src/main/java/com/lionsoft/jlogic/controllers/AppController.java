package com.lionsoft.jlogic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.ui.Model;
import org.springframework.boot.info.BuildProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.locks.*;
import java.util.Formatter;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import java.util.Optional;
import java.lang.Runtime;
import java.text.DecimalFormat;

//import java.lang.management.ManagementFactory;
//import java.lang.management.MemoryMXBean;

class Menu {
  public String href;
  public String label;

  public Menu (String href, String label) {
    this.href = href;
    this.label = label;
  }
};

@Controller
public class AppController {
  Logger logger = LoggerFactory.getLogger(AppController.class);

  @Autowired
  BuildProperties buildProperties;

	@Autowired
	private UserRepository userRepository;

  @Autowired
  ProgramService programService;

  @Autowired
  SessionService sessionService;

	@RequestMapping("/home")
	public String home(HttpServletRequest request, Model model) {
    Session session = sessionService.getSession(request);

    if (session != null)
      session.setWebApplication(true);

		model.addAttribute("name", buildProperties.getName());
		model.addAttribute("version", buildProperties.getVersion());
		model.addAttribute("buildTime", buildProperties.getTime().toString());

		// JRE info
		model.addAttribute("java_class_path", System.getProperty("java.class.path"));
		model.addAttribute("java_home", System.getProperty("java.home"));
		model.addAttribute("java_vendor", System.getProperty("java.vendor"));
		model.addAttribute("java_vendor_url", System.getProperty("java.vendor.url"));
		model.addAttribute("java_version", System.getProperty("java.version"));

		// OS info
		model.addAttribute("os", System.getProperty("os.name")+" "+System.getProperty("os.version")+" "+System.getProperty("os.arch"));

    try {
      InetAddress addr;
      addr = InetAddress.getLocalHost();
      model.addAttribute("nodeName", addr.getHostName());
    } catch (UnknownHostException ex) {
        logger.error("Hostname can not be resolved");
    }

	  return "home";
	}

	@RequestMapping("/sidebar")
	public String sidebar(Model model) {
		model.addAttribute("name", buildProperties.getName());
		model.addAttribute("version", buildProperties.getVersion());

    try {
      InetAddress addr;
      addr = InetAddress.getLocalHost();
      model.addAttribute("nodeName", addr.getHostName());
    } catch (UnknownHostException ex) {
        logger.error("Hostname can not be resolved");
    }
    /*
    List<Menu> items = new ArrayList<Menu>();
    items.add(new Menu("/home",            "<i class=\"icon i-home\" style=\"color:lightslategray; min-width: 1.5em;\"></i> Home"));
    items.add(new Menu("/bp",              "<i class=\"icon i-project-diagram\" style=\"color:lightslategray; min-width: 1.5em;\"></i> Blueprints"));
    items.add(new Menu("/apipanel",        "<i class=\"icon i-cube\" style=\"color:lightslategray; min-width: 1.5em;\"></i> APIs"));
    items.add(new Menu("/edit-properties", "<i class=\"icon i-sliders-h\" style=\"color:lightslategray; min-width: 1.5em;\"></i> Properties"));
    items.add(new Menu("/sessions",        "<i class=\"icon i-list\" style=\"color:lightslategray; min-width: 1.5em;\"></i> Sessions"));
    items.add(new Menu("/stats",           "<i class=\"icon i-chart-bar\" style=\"color:lightslategray; min-width: 1.5em;\"></i> Analytics"));
    items.add(new Menu("/users",           "<i class=\"icon i-users\" style=\"color:lightslategray; min-width: 1.5em;\"></i> Users"));

    model.addAttribute("items", items);
    */
	  return "sidebar";
	}

	@RequestMapping("/favicon")
	public String favicon() { return "favicon.ico";	}

	@RequestMapping("/login")
	public String login(HttpServletRequest request, Model model) {
    return "login";
  }

	@RequestMapping("/logout")
	public String logout(HttpServletRequest request, HttpServletResponse response) {
	  logger.info("Logging out "+(request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Unknown")+" "+request.getSession().getId());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            logger.info("Logged out "+(request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Unknown")+" "+request.getSession().getId());
            sessionService.deleteSession(request);
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
	  return "redirect:/login?logout";
	}

	@RequestMapping("/bp")
	public String getBlueprints(Model model) { return "bp";	}

	@RequestMapping("/apipanel")
	public String getApiPanel() { return "api";	}

	@RequestMapping("/blueprint/{id}/edit")
	public String editBlueprint(@PathVariable("id") String id) { return "edit"; }

	@RequestMapping("/edit-properties")
	public String getGlobalProperties(Model model) {
	  model.addAttribute("title", "Properties");
	  return "edit-properties";
	}

	@RequestMapping("/program/{id}/edit-properties")
	public String getProgramProperties(Model model, @PathVariable("id") String id) {
	  String page = "edit-properties";
	  Optional<ProgramEntity> program = programService.findById(id);

	  if (program.isPresent()) {
	    model.addAttribute("title", "Properties of program "+program.get().getName());
	  } else {
	    page = "not-found";
	  }

	  return page;
	}

	@RequestMapping("/view-sessions")
	public String getSessions() { return "view-sessions";	}

	@RequestMapping("/metrics")
	public String viewMetrics(Model model) {
	  long mb = 1024L * 1024L;


	  Long totalMemory = Runtime.getRuntime().totalMemory();
	  Long freeMemory = Runtime.getRuntime().freeMemory();

	  /*
	  MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    Long totalMemory = (double)memoryMXBean.getHeapMemoryUsage().getMax();
    Long freeMemory = (double)memoryMXBean.getHeapMemoryUsage().getUsed();*/

	  double pctUsedMemory = ((double)(totalMemory - freeMemory)/totalMemory) * 100;

	  System.out.println(totalMemory);
	  System.out.println(freeMemory);
	  System.out.println(pctUsedMemory);

	  //DecimalFormat decimalFormat = new DecimalFormat("###.##");
	  model.addAttribute("totalMemory", String.format("%.2f MB", (double) totalMemory / mb));
	  model.addAttribute("freeMemory", String.format("%.2f MB", (double) freeMemory / mb));
	  model.addAttribute("pctUsedMemory", String.format("%.2f%%", pctUsedMemory));

	  return "view-metrics";
	}

	@RequestMapping("/view-users")
	public String editUsers() { return "view-users";	}

	@RequestMapping("/user/create")
	public String createUser(Model model) {
	  model.addAttribute("title", "Create user");
	  model.addAttribute("id", 0);
	  model.addAttribute("creating", true);
	  model.addAttribute("updating", false);
	  return "edit-user";
	}

	@RequestMapping("/user/edit")
	public String editUser(HttpServletRequest request, Model model) {

	  Optional<User> user = userRepository.findByUsername(request.getUserPrincipal().getName());

	  model.addAttribute("title", "Edit user");
	  model.addAttribute("username", user.get().getUsername());
	  model.addAttribute("firstName", user.get().getFirstName());
	  model.addAttribute("lastName", user.get().getLastName());
	  model.addAttribute("roleSet", user.get().getRoleSet());
	  model.addAttribute("id", "-1");
	  model.addAttribute("creating", false);
	  model.addAttribute("updating", true);
	  model.addAttribute("updating_current_user", true);
	  return "edit-user";
	}

	@RequestMapping("/user/{username}/edit")
	public String editUser(Model model, @PathVariable("username") String username/*, @RequestParam(value = "username", defaultValue = "0") String username*/) {

	  Optional<User> user = userRepository.findByUsername(username);

	  model.addAttribute("title", "Edit user");
	  model.addAttribute("username", user.get().getUsername());
	  model.addAttribute("firstName", user.get().getFirstName());
	  model.addAttribute("lastName", user.get().getLastName());
	  model.addAttribute("roleSet", user.get().getRoleSet());
	  model.addAttribute("id", user.get().getId());
	  model.addAttribute("creating", false);
	  model.addAttribute("updating", true);
	  return "edit-user";
	}

	@RequestMapping("/change-password")
	public String changePassword(HttpServletRequest request, Model model) {

	  Optional<User> user = userRepository.findByUsername(request.getUserPrincipal().getName());

	  model.addAttribute("title", "Change password of user "+user.get().getUsername());
	  model.addAttribute("username", user.get().getUsername());
	  model.addAttribute("id", "-1");
	  return "change-password";
	}

}
