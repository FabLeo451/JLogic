package com.lionsoft.jlogic;

import org.springframework.beans.factory.annotation.Autowired;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import java.io.IOException;
import javax.servlet.ServletException;
import org.springframework.security.core.Authentication;

public class CustomLogoutSuccessHandler extends
  SimpleUrlLogoutSuccessHandler implements LogoutSuccessHandler {

    @Autowired
    private SessionService sessionService;

    @Override
    public void onLogoutSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication)
      throws IOException, ServletException {

        logger.info("Logged out "+(request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Unknown")+" "+request.getSession().getId());
        sessionService.deleteSession(request.getSession().getId());
        response.sendRedirect("/login?logout");
        //super.onLogoutSuccess(request, response, authentication);
    }
}
