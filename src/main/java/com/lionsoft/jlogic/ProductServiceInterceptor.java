package com.lionsoft.jlogic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.beans.factory.annotation.Autowired;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.*;

@Component
public class ProductServiceInterceptor implements HandlerInterceptor {

    @Autowired
    SessionService sessionService;

    Logger logger = LoggerFactory.getLogger(ProductServiceInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //logger.info("Request: " + request.getRemoteAddr() + " " + request.getMethod() + " " + request.getRequestURI());
        sessionService.addRequest(request);
        sessionService.setCurrentActive(true);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        //logger.info("postHandle: "+Thread.currentThread().getId()+" "+request.getRequestURI());
        sessionService.completed(request);
    }

    @Override
    public void afterCompletion (HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) throws Exception {
        //logger.info("Response: " + response.getStatus() + " " + request.getMethod() + " " + request.getRemoteAddr() + " " + request.getRequestURI());
        //if (request.getSession(false) != null)
        //  logger.info("afterCompletion: "+request.getSession(false).getId()+" "+request.getRequestURI());
        //logger.info("afterCompletion: "+Thread.currentThread().getId()+" "+request.getRequestURI());

        sessionService.deleteCurrentRequest();

        HttpSession httpSession = request.getSession(false);

        if (httpSession != null) {
            boolean web = httpSession.getAttribute("webApplication") != null ? (Boolean) httpSession.getAttribute("webApplication") : false;

            if (!web)
                sessionService.deleteSession(request);
        }
    }
}
