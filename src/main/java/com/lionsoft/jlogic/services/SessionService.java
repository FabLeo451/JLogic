package com.lionsoft.jlogic;

import java.util.*;
import java.util.stream.*;
import java.lang.*;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SessionService {

    static Logger logger = LoggerFactory.getLogger(SessionService.class);

    private static SessionService instance = null;

    private static ReadWriteLock rwLock = new ReentrantReadWriteLock();
    //private static List<Session> list = new ArrayList<Session>(); // DEPRECATED

    // Current sessions
    private static Map<String, HttpSession> sessions;

    // Current requests (table of processes)
    private static Map<Long, Request> top;

    // All historical requests for statistics
    private static List<Request> requests = new ArrayList<Request>();

    private SessionService() {
        sessions = new HashMap<String, HttpSession>();
        top = new HashMap<Long, Request>();
    }

    public static SessionService getInstance() {
        if (instance == null) {
            instance = new SessionService();
        }
        return instance;
    }

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

    public static List<Request> getTOP() {
        List<Request> list = new ArrayList<Request>();

        for (Map.Entry<Long, Request> entry : top.entrySet()) {
            Long id = entry.getKey();
            Request r = (Request)entry.getValue();
            
            try {
                list.add(r);
            } catch (IllegalStateException e) {
                // Can occur on already invalidated sessions
            }
        }

        return list;
    }

    public static HttpSession getSession(String id) {
        return sessions.get(id);
    }

    public static void addRequest(HttpServletRequest request) {
        Lock writeLock = rwLock.writeLock();
        writeLock.lock();

        try {
            addSession(request);

            Request r = new Request(request);
            
            logger.trace(r.toString());
            
            // Add to TOP
            top.put(r.getThreadId(), r);
            
            // Add to history
            requests.add(r);
        } finally {
            writeLock.unlock();
        }
    }

    public static void addSession(HttpServletRequest request) {
        HttpSession httpSession = request.getSession(false);

        if (httpSession != null) {
            httpSession.setAttribute("remoteAddress", request.getRemoteAddr());

            if (!sessions.containsKey(httpSession.getId())) {
                // New session
                sessions.put(httpSession.getId(), httpSession);
                httpSession.setAttribute("user", request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Unknown");
            }
        }
    }

    public static void deleteSession(String id) {
        Lock writeLock = rwLock.writeLock();
        writeLock.lock();

        try {
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

    public void setCurrentActive(boolean active) {
        Request r = top.get(Thread.currentThread().getId());
        r.setStatus(active ? RequestStatus.ACTIVE : RequestStatus.IDLE);
    }

    public void deleteCurrentRequest() {
        top.remove(Thread.currentThread().getId());
    }

    public void setActive(HttpServletRequest request, boolean active) {
        HttpSession httpSession = request.getSession(false);

        if (httpSession != null) {
            httpSession.setAttribute("status", active ? "ACTIVE" : "IDLE");
        }
    }

    public void completed(HttpServletRequest request) {
        /*HttpSession httpSession = request.getSession(false);

        if (httpSession != null)
          httpSession.setAttribute("status", "IDLE");*/
          
        Request r = top.get(Thread.currentThread().getId());
        r.setStatus(RequestStatus.IDLE);
    }

    public void setProgramUnit(HttpServletRequest request, String pu) {
        /*HttpSession httpSession = request.getSession(false);

        if (httpSession != null) {
          httpSession.setAttribute("programUnit", pu);
        }*/
        Request r = top.get(Thread.currentThread().getId());
        r.setProgramUnit(pu);
    }

    public boolean isWebApplication(HttpServletRequest request) {
        HttpSession httpSession = request.getSession(false);
        boolean web = false;

        if (httpSession != null)
            web = httpSession.getAttribute("webApplication") != null ? (Boolean) httpSession.getAttribute("webApplication") : false;

        return(web);
    }
    
    /**
     * Search request by thread id
     */
    public Request getRequestById(Long id) {
        return(top.get(id));
    }
    
    /**
     * Search request by client id
     */
    public Request getRequestByClientId(String id) {
        for (Map.Entry<Long, Request> entry : top.entrySet()) {
            Long threadId = entry.getKey();
            Request r = (Request)entry.getValue();
            
            if (r.getClientId() != null && r.getClientId().equals(id))
                return(r);
        }
        
        return null;
    }
    
    /**
     * Return current session
     */
    public HttpSession getCurrentSession() {
        return(getSession(getCurrentRequest().getSessionId()));
    }
    
    /**
     * Return current request
     */
    public Request getCurrentRequest() {
        return(top.get(Thread.currentThread().getId()));
    }
    
    /**
     * Interrupts the thread of the given request
     */
    public boolean stop(Request r) {
        if (r == null)
            return false;
            
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getId() == r.getThreadId()) {
                t.interrupt();
                return true;
            }
        }
        
        return false;
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
