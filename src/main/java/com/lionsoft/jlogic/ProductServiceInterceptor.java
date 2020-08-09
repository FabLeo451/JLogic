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

@Component
public class ProductServiceInterceptor implements HandlerInterceptor {

  @Autowired
  SessionService sessionService;

  Logger logger = LoggerFactory.getLogger(ProductServiceInterceptor.class);

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    //logger.info("Request: " + request.getRemoteAddr() + " " + request.getMethod() + " " + request.getRequestURI());
    /*
    SessionsUtils sessionUtils = SessionsUtils.getInstance();
    sessionUtils.addSession(request);
    sessionUtils.addRequest(request);
    */
    sessionService.addSession(request);
    sessionService.addRequest(request);

    return true;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    //System.out.println("Post Handle method is Calling");

    //SessionsUtils sessionUtils = SessionsUtils.getInstance();
    sessionService.completed(request);
  }

  @Override
  public void afterCompletion (HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) throws Exception {
    //logger.info("Response: " + response.getStatus() + " " + request.getMethod() + " " + request.getRemoteAddr() + " " + request.getRequestURI());
    Session session = sessionService.getSession(request);

    if (session != null && !session.getWebApplication()) {
      logger.info("Deleting session " + session.getId());
      sessionService.deleteSession(request);
    }
  }
}
