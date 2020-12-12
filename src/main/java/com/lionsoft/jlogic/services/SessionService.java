package com.lionsoft.jlogic;

import java.util.*;
import java.util.stream.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.locks.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpMethod;
//import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.context.SecurityContextHolder;
import org.apache.commons.lang3.time.DateUtils;
import eu.bitwalker.useragentutils.*;

@Service
public class SessionService {

  //@Autowired
  //private SessionRegistry sessionRegistry;

  private static SessionService instance = null;

  private static ReadWriteLock rwLock = new ReentrantReadWriteLock();
  //private static List<Session> list = new ArrayList<Session>(); // DEPRECATED
  private static Map<String, HttpSession> sessions; // NEW

  // Register all request for statistics
  private static List<Request> requests = new ArrayList<Request>();

  private SessionService() {
    sessions = new HashMap<String, HttpSession>();
  }

  public static SessionService getInstance() {
      if (instance == null) {
          instance = new SessionService();
      }
      return instance;
  }

  /*public static List<Session> getList() { // DEPRECATED
    return list;
  }*/
/*
  public static List<Object> getSessions_deprecated() {
    List<Object> list = new ArrayList<Object>();

    for (Map.Entry<String, HttpSession> entry : sessions.entrySet()) {
      String id = entry.getKey();
      HttpSession s = (HttpSession)entry.getValue();

      try {
        Map<String, Object> info = new HashMap<String, Object>();
        info.put("id", id);
        info.put("status", s.getAttribute("status"));
        info.put("creationTime", new Date(s.getCreationTime()));
        info.put("user", s.getAttribute("user"));
        info.put("agent", s.getAttribute("agent"));
        info.put("webApplication", s.getAttribute("webApplication") != null ? s.getAttribute("webApplication") : false);
        info.put("programUnit", s.getAttribute("programUnit"));
        info.put("remoteAddress", s.getAttribute("remoteAddress"));
        list.add(info);
      } catch (IllegalStateException e) {
        // Can occur on already invalidated sessions
      }
    }

    return list;
  }
*/
  public static List<Session> getSessions() {
    List<Session> list = new ArrayList<Session>();

    for (Map.Entry<String, HttpSession> entry : sessions.entrySet()) {
      String id = entry.getKey();
      HttpSession hs = (HttpSession)entry.getValue();
      Session s = new Session(hs);

      try {
        list.add(s);
      } catch (IllegalStateException e) {
        // Can occur on already invalidated sessions
      }
    }

    return list;
  }

  public static void addRequest(HttpServletRequest request) {
    Lock writeLock = rwLock.writeLock();
    writeLock.lock();

    try {
      requests.add(new Request(request));
    } finally {
      writeLock.unlock();
    }
  }

  public static void addSession(HttpServletRequest request) {
    HttpSession httpSession = request.getSession(false);

    if (httpSession != null) {
      Lock writeLock = rwLock.writeLock();
      writeLock.lock();

      try {
        // DEPRECATED ///////////////////////////////
        /*Session session = findById(httpSession.getId());

        if (session == null)
          list.add(new Session(request));
        else
          session.update(request);
*/
        // NEW ///////////////////////////////

        /*Enumeration<String> names = httpSession.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            Object value = httpSession.getAttribute(name);
            System.out.println(name+" = "+value);
        }*/

        httpSession.setAttribute("remoteAddress", request.getRemoteAddr());

        if (!sessions.containsKey(httpSession.getId())) {
          sessions.put(httpSession.getId(), httpSession);
          httpSession.setAttribute("user", request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Unknown");
          httpSession.setAttribute("status", "IDLE");
          httpSession.setAttribute("programUnit", "");

          String agent = request.getHeader("User-Agent");

          UserAgent userAgent = UserAgent.parseUserAgentString(agent);

          if (userAgent != null) {
            Browser browser = userAgent.getBrowser();
            BrowserType type = browser.getBrowserType();

            if (type != BrowserType.TOOL) {
              String bwrVersion = "?";
              Version v = userAgent.getBrowserVersion();
              if (v != null)
                bwrVersion = userAgent.getBrowserVersion().getMajorVersion();

              String bwrName = browser.getName();

              OperatingSystem os = userAgent.getOperatingSystem();
              String osName = os.getName();
              //String deType = os.getDeviceType().getName();

              agent = bwrName+"/"+bwrVersion+" "+osName;
            }
          }

          httpSession.setAttribute("agent", agent);

        } else
          httpSession.setAttribute("status", "ACTIVE");
      } finally {
        writeLock.unlock();
      }
    }
  }

  public static void deleteSession(String id) {
    Lock writeLock = rwLock.writeLock();
    writeLock.lock();

    try {
      // DEPRECATED ///////////////////////////////
      /*Session session = findById(id);
      //System.out.println("Deleting session "+httpSession.getId());

      if (session != null)
        list.remove(session);*/
      // NEW ///////////////////////////////
      sessions.remove(id);
    } finally {
      writeLock.unlock();
    }
  }

  public static void deleteSession(HttpServletRequest request) {
    HttpSession httpSession = request.getSession(false);

    if (httpSession != null) {
      deleteSession(httpSession.getId());

      // Invalidate session
      //HttpSession session = request.getSession(false);
      //System.out.println("Invalidating "+httpSession.getId());
      SecurityContextHolder.clearContext();
      httpSession.invalidate();
    }
  }

  public void setActive(HttpServletRequest request, boolean active) {
    HttpSession httpSession = request.getSession(false);

    if (httpSession != null) {
      httpSession.setAttribute("status", active ? "ACTIVE" : "IDLE");
    }
  }
/*
  public Session getSession(HttpServletRequest request) {
    Session session = null;

    try {
      HttpSession httpSession = request.getSession(false);

      if (httpSession != null)
        session = findById(httpSession.getId());
    }
    catch (IllegalStateException e) {}

    return session;
  }*/

  public void completed(HttpServletRequest request) {
    // DEPRECATED ///////////////////////////////
    /*try {
      Session session = getSession(request);

      if (session != null) {
        if (request.getUserPrincipal() != null)
          session.setStatus(Session.IDLE);
      }
    }
    catch (IllegalStateException e) {}*/

    // NEW ///////////////////////////////
    HttpSession httpSession = request.getSession(false);

    if (httpSession != null)
      httpSession.setAttribute("status", "IDLE");
  }

  /*public static void setStatus(HttpServletRequest request, int status) {
    try {
      HttpSession httpSession = request.getSession(false);

      if (httpSession != null) {
        Session session = findById(httpSession.getId());

        if (session != null) {
            session.setStatus(status);
        }
      }
    }
    catch (IllegalStateException e) {}
  }*/
/*
  public static Session findById(String id) {
    for (Session s : list) {
      if (s.getId() != null) {
        if (s.getId().equals(id))
          return s;
      }
    }

    return null;
  }*/

  public void setProgramUnit(HttpServletRequest request, String pu) {
    HttpSession httpSession = request.getSession(false);

    if (httpSession != null) {
      httpSession.setAttribute("programUnit", pu);
    }
  }

  public boolean isWebApplication(HttpServletRequest request) {
    HttpSession httpSession = request.getSession(false);
    boolean web = false;

    if (httpSession != null)
      web = httpSession.getAttribute("webApplication") != null ? (Boolean) httpSession.getAttribute("webApplication") : false;

    return(web);
  }

  public static Map<String, Long> getStats(Date from, Date to) {
    //System.out.println("From: "+from);
    //System.out.println("To: "+to);

    List<Request> rangeList = new ArrayList<Request>();

    for (Request r: requests) {
      if (!from.after(r.getTimestamp()) && !to.before(r.getTimestamp()))
        rangeList.add(r);
    }

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:00");

    Map<String, Long> counted = rangeList.stream()
        .collect(Collectors.groupingBy(r->sdf.format(r.getTimestamp()), Collectors.counting()));

    Date d = from;

    while (d.before(to)) {
      String key = sdf.format(d);

      if (!counted.containsKey(key))
        counted.put(key, 0L);

      d = DateUtils.addMinutes(d, 1);
    }

    Map<String, Long> sortedMap = new TreeMap<String, Long>(counted);

    return sortedMap;
  }
}
