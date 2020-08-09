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

@Service
public class SessionService {

  //@Autowired
  //private SessionRegistry sessionRegistry;

  private static SessionService instance = null;

  private static ReadWriteLock rwLock = new ReentrantReadWriteLock();
  private static List<Session> list = new ArrayList<Session>();

  // Register all request for statistics
  private static List<Request> requests = new ArrayList<Request>();

  private SessionService() {

  }

  public static SessionService getInstance() {
      if (instance == null) {
          instance = new SessionService();
      }
      return instance;
  }

  public static List<Session> getList() {
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
        Session session = findById(httpSession.getId());

        if (session == null)
          list.add(new Session(request));
        else
          session.update(request);
      } finally {
        writeLock.unlock();
      }
    }
  }

  public static void deleteSession(String id) {
    Lock writeLock = rwLock.writeLock();
    writeLock.lock();

    try {
      Session session = findById(id);
      //System.out.println("Deleting session "+httpSession.getId());

      if (session != null)
        list.remove(session);
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
      SecurityContextHolder.clearContext();
      if (httpSession != null) {
          httpSession.invalidate();
      }
    }
  }

  public Session getSession(HttpServletRequest request) {
    Session session = null;

    try {
      HttpSession httpSession = request.getSession(false);

      if (httpSession != null)
        session = findById(httpSession.getId());
    }
    catch (IllegalStateException e) {}

    return session;
  }

  public void completed(HttpServletRequest request) {
    try {
      Session session = getSession(request);

      if (session != null) {
        if (request.getUserPrincipal() != null)
          session.setStatus(Session.IDLE);
        else
          deleteSession(request);
      }
    }
    catch (IllegalStateException e) {}
  }

  public static void setStatus(HttpServletRequest request, int status) {
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
  }

  public static Session findById(String id) {
    for (Session s : list) {
      if (s.getId() != null) {
        if (s.getId().equals(id))
          return s;
      }
    }

    return null;
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
