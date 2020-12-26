package com.lionsoft.jlogic;

import java.util.*;
import java.lang.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import java.security.Principal;
import eu.bitwalker.useragentutils.*;

public class Request {

    private Date timestamp;
    private String user = null;
    private String agent = null;
    private String requestURI = null;
    private String remoteAddress = null;
    private RequestStatus status = RequestStatus.IDLE;
    private String programUnit;
    private String clientId;

    private String sessionId;
    private long threadId;

    public Request(HttpServletRequest request) {
        HttpSession session = request.getSession();

        threadId = Thread.currentThread().getId();
        sessionId = session.getId();
        timestamp = new Date(session.getLastAccessedTime());
        requestURI = request.getRequestURI();
        update(request);
    }

    public void update(HttpServletRequest request) {
        HttpSession session = request.getSession();

        user = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Unknown";
        agent = request.getHeader("User-Agent");

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

        remoteAddress = request.getRemoteAddr();
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getUser() {
        return user;
    }

    public String getAgent() {
        return agent;
    }

    public String getRequestURI() {
        return requestURI;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getThreadId() {
        return threadId;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getProgramUnit() {
        return programUnit;
    }

    public void setProgramUnit(String pu) {
        this.programUnit = pu;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String id) {
        this.clientId = id;
    }
    
    public String toString() {
        return("Request [id="+threadId+" remoteAddress="+remoteAddress+" user="+user+" uri="+requestURI+"]");
    }
}
