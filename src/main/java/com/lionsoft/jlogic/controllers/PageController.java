package com.lionsoft.jlogic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.ui.Model;
import org.springframework.boot.info.BuildProperties;
import org.springframework.beans.factory.annotation.Value;

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
import javax.servlet.http.HttpSession;

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
public class PageController {
    Logger logger = LoggerFactory.getLogger(PageController.class);

    @Autowired
    BuildProperties buildProperties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    ProgramService programService;

    @Autowired
    APIService APIService;

    @Autowired
    SessionService sessionService;

    @Autowired
    CatalogService catalogService;

    public String COPYRIGHT = "Copyright (c) 2020,2021 Fabio Leone";

    @Value("${maven.home}")
    String MAVEN_HOME;

    @RequestMapping("/home")
    public String home(HttpServletRequest request, Model model) {

        model.addAttribute("name", buildProperties.getName());
        model.addAttribute("version", buildProperties.getVersion());
        model.addAttribute("buildTime", buildProperties.getTime().toString());
        model.addAttribute("copyright", COPYRIGHT);

        // JRE info
        model.addAttribute("java_class_path", System.getProperty("java.class.path"));
        model.addAttribute("java_home", System.getProperty("java.home"));
        model.addAttribute("java_vendor", System.getProperty("java.vendor"));
        model.addAttribute("java_vendor_url", System.getProperty("java.vendor.url"));
        model.addAttribute("java_version", System.getProperty("java.version"));
        model.addAttribute("maven_home", MAVEN_HOME);

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
        model.addAttribute("name", buildProperties.getName());
        model.addAttribute("version", buildProperties.getVersion());
        return "login";
    }

    @RequestMapping("/perform_login")
    public String perform_login(HttpServletRequest request, Model model) {
        HttpSession httpSession = request.getSession(false);

        if (httpSession != null) {
          httpSession.setAttribute("webApplication", true);

          String username = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Unknown";
          //logger.info(username+" logged in ("+httpSession.getId()+")");
          logger.info("Log in "+(new Session(httpSession).toString()));
        }

        //logger.warn("Redirecting to /home");

        return "redirect:/home";
    }
/*
    @RequestMapping("/expired")
    public String expired(HttpServletRequest request, Model model) {
        logger.warn("Session expired: "+request.getSession(false).getId());
        sessionService.deleteSession(request);
        return "redirect:/login?expired";
    }
    */
/*
	@PostMapping("/logout")
	public String perform_logout(HttpServletRequest request, HttpServletResponse response) {
        String username = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Unknown";
        logger.info(username+" requested log out");
        \/*
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            logger.info("Logged out "+(request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Unknown")+" "+request.getSession().getId());
            sessionService.deleteSession(request);
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }*\/

        return "redirect:/login?logout";
	}
*/

	@RequestMapping("/view-programs")
	public String getBlueprints(Model model,
                                @RequestParam(value = "element", required=false) String element) {
        String resource = "view-programs";

        model.addAttribute("programs", programService.findAll());
        model.addAttribute("byName", Comparator.comparing(ProgramEntity::getName));

        if (element != null)
          resource += " :: #"+element;

        //System.out.println("resource = " + resource);

        return resource;
    }

    /**
     * Edit program
     */
	@RequestMapping("/program/{id}/edit")
    public String editProgram(HttpServletRequest request,
                              Model model,
                              @PathVariable("id") String id,
                              @RequestParam(value = "element", required=false) String element) {
        String resource = "edit-program";
        Optional<ProgramEntity> program = programService.findById(id);

        if (!program.isPresent())
            return "bp";

        /*
         * Clone blueprint list to avoid
         * org.hibernate.LazyInitializationException: failed to lazily initialize a collection, could not initialize proxy - no Session
         */
        List<BlueprintEntity> blueprints = new ArrayList<BlueprintEntity>(program.get().getBlueprints());

        //System.out.println("Program " + program.get().getName() + " has " + blueprints.size() + " blueprints");

        model.addAttribute("title", "Edit program");
        model.addAttribute("program", program.get());
        model.addAttribute("blueprints", blueprints);
        model.addAttribute("creating", false);
        model.addAttribute("updating", true);
        model.addAttribute("byName", Comparator.comparing(BlueprintEntity::getName));

        if (element != null)
          resource += " :: #"+element;

        //System.out.println("resource = " + resource);

        return resource;
	}

	@RequestMapping("/apipanel")
	public String getApiPanel(Model model,
                              @RequestParam(value = "id", required=false) String id) {
        String resource = "view-apis";

        model.addAttribute("apis", APIService.findAll());
        model.addAttribute("byName", Comparator.comparing(APIEntity::getName));

        if (id != null)
          resource += " :: #"+id;

        //System.out.println("resource = " + resource);

        return resource;
    }

    /**
     * Create API
     */
	@RequestMapping("/mapping/create")
	public String createApi(HttpServletRequest request, Model model) {
        Optional<User> user = userRepository.findByUsername(request.getUserPrincipal().getName());

        model.addAttribute("title", "Create API");
        model.addAttribute("api", new APIEntity(null, "New API", true));
        model.addAttribute("creating", true);
        model.addAttribute("updating", false);
        model.addAttribute("programs", catalogService.getPrograms());
        return "edit-api";
	}

  /**
   * Update API
   */
	@RequestMapping("/mapping/{id}/edit")
	public String editApi(HttpServletRequest request, Model model, @PathVariable("id") String id) {
        Optional<APIEntity> api = APIService.findById(id);

        if (!api.isPresent())
            return "/apipanel";

        model.addAttribute("title", "Edit API");
        //model.addAttribute("id", id);
        //model.addAttribute("name", api.get().getName());
        model.addAttribute("api", api.get());
        model.addAttribute("program", api.get().getBlueprint().getProgram());
        model.addAttribute("blueprint", api.get().getBlueprint());
        model.addAttribute("creating", false);
        model.addAttribute("updating", true);
        model.addAttribute("programs", catalogService.getPrograms());
        model.addAttribute("blueprints", api.get().getBlueprint().getProgram().getBlueprints());

        return "edit-api";
	}

    /**
     * View api log
     */
	@RequestMapping("/mapping/{id}/view-log")
	public String viewLog(HttpServletRequest request, Model model, @PathVariable("id") String id) {

        Optional<APIEntity> api = APIService.findById(id);

        if (!api.isPresent())
            return "/apipanel";

        model.addAttribute("title", "Log");
        model.addAttribute("api", api.get());
        model.addAttribute("log", APIService.tail(api.get(), 30));
        return "view-log";
	}

    /**
     * Edit blueprint
     */
    @RequestMapping("/blueprint/{id}/edit")
    public String editBlueprint(Model model, @PathVariable("id") String id) {
        model.addAttribute("name", buildProperties.getName());
        model.addAttribute("version", buildProperties.getVersion());
        model.addAttribute("copyright", COPYRIGHT);
        model.addAttribute("clientId", UUID.randomUUID().toString());

        return "edit";
    }

    /**
     * Edit properties
     */
    @RequestMapping("/edit-properties")
    public String getGlobalProperties(Model model) {
        model.addAttribute("globals", true);
        return "edit-properties";
    }

    @RequestMapping("/program/{id}/edit-properties")
    public String getProgramProperties(Model model, @PathVariable("id") String id) {
        String page = "edit-properties";
        Optional<ProgramEntity> program = programService.findById(id);

        if (program.isPresent()) {
            model.addAttribute("globals", false);
            model.addAttribute("program", program.get());
        } else {
            page = "not-found";
        }

        return page;
    }

    @RequestMapping("/view-top")
    public String getSessions(Model model, @RequestParam(value = "element", required=false) String element) {
        String resource = "view-top";

        //model.addAttribute("sessions", sessionService.getSessions());
        //model.addAttribute("byLoginTime", Comparator.comparing(Session::getLoginTime));
        model.addAttribute("top", sessionService.getTOP());
        model.addAttribute("byTimestamp", Comparator.comparing(Request::getTimestamp));

        if (element != null)
          resource += " :: #"+element;

        //System.out.println("resource = " + resource);

        return resource;
    }
/*
    @RequestMapping("/view-sessions-upd")
    public String getSessionsUpd(Model model) {
    model.addAttribute("sessions", sessionService.getSessions());
    model.addAttribute("byLoginTime", Comparator.comparing(Session::getLoginTime));
    return "view-sessions :: #sessions";
    }*/

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

        /*System.out.println(totalMemory);
        System.out.println(freeMemory);
        System.out.println(pctUsedMemory);*/

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
