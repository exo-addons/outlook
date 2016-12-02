
/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.outlook.server.filter;

import org.exoplatform.outlook.security.OutlookTokenService;
import org.exoplatform.outlook.web.RequestUtils;
import org.exoplatform.web.filter.Filter;
import org.exoplatform.web.security.security.AbstractTokenService;
import org.exoplatform.web.security.security.CookieTokenService;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.wci.ServletContainer;
import org.gatein.wci.ServletContainerFactory;
import org.gatein.wci.security.Credentials;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

/**
 * Filter for /portal/intranet/outlook path. When eXo user not authenticated it check if can restore user
 * credentials from Outlook token store (using 'remembermeoutlook' cookie) and if cannot, and request
 * contains parameter _host_Info (from Outlook server), then this filter escapes the request query
 * parameters to avoid failures in LoginServlet (to parse initialURI).
 * <br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookRememberMeFilter.java 00000 Jul 1, 2016 pnedonosko $
 * 
 */
public class OutlookRememberMeFilter implements Filter {

  /** The Constant LOG. */
  protected static final Logger LOG                  = LoggerFactory.getLogger(OutlookRememberMeFilter.class);

  /** The Constant INITIAL_COOKIE_VALUE. */
  public static final String    INITIAL_COOKIE_VALUE = "_init_me";

  /**
   * The Class FixedQueryRequest.
   */
  public static class FixedQueryRequest extends HttpServletRequestWrapper {

    /** The fixed query. */
    final String fixedQuery;

    /**
     * Instantiates a new fixed query request.
     *
     * @param request the request
     * @param fixedQuery the fixed query
     */
    protected FixedQueryRequest(HttpServletRequest request, String fixedQuery) {
      super(request);
      this.fixedQuery = fixedQuery;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getQueryString() {
      return fixedQuery;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                            ServletException {
    HttpServletRequest httpReq = (HttpServletRequest) request;
    HttpServletResponse httpRes = (HttpServletResponse) response;

    String uri = httpReq.getRequestURI();

    String rememberMeOutlook = RequestUtils.getCookie(httpReq, OutlookTokenService.COOKIE_NAME);
    if (httpReq.getRemoteUser() == null) {
      // Check if we have initialized 'remembermeoutlook' cookie and if have, try restore user credentials
      // from token store
      if (rememberMeOutlook != null) {
        if (!INITIAL_COOKIE_VALUE.equals(rememberMeOutlook)) {
          OutlookTokenService outlookTokens = AbstractTokenService.getInstance(OutlookTokenService.class);
          Credentials credentials = outlookTokens.validateToken(rememberMeOutlook, false);
          if (credentials != null) {
            try {
              ServletContainer servletContainer = ServletContainerFactory.getServletContainer();
              servletContainer.login(httpReq, httpRes, credentials);
              if (LOG.isDebugEnabled()) {
                LOG.debug("Cookie " + OutlookTokenService.COOKIE_NAME + " restored user (" + credentials.getUsername()
                    + ") credentials for " + uri);
              }
              LOG.info("Restored user (" + credentials.getUsername() + ") credentials for " + uri);
              // redirect to this resource again to let portal filters do the work properly (e.g. set
              // conversation state and session provider)
              httpRes.sendRedirect(new StringBuilder(uri).append('?').append(httpReq.getQueryString()).toString());
              return;
            } catch (Exception e) {
              // Could not authenticate
              LOG.warn("Cannot restore user credentials from " + OutlookTokenService.COOKIE_NAME + " cookie value '"
                  + rememberMeOutlook + "' for " + uri + ". " + e.getMessage());
            }
          }
        } else if (LOG.isDebugEnabled()) {
          LOG.debug("Cookie " + OutlookTokenService.COOKIE_NAME + " found with initial value but remote user not set for "
              + uri);
        }
      }
    } else {
      // Check if we have 'remembermeoutlook' cookie with init request - if yes, and if have 'rememberme'
      // cookie, then save credentials in token store
      if (rememberMeOutlook != null && INITIAL_COOKIE_VALUE.equals(rememberMeOutlook)) {
        String rememberMe = RequestUtils.getCookie(httpReq, "rememberme");
        CookieTokenService rememberMeTokens = AbstractTokenService.getInstance(CookieTokenService.class);
        Credentials credentials = rememberMeTokens.validateToken(rememberMe, false);
        if (credentials != null) {
          OutlookTokenService outlookTokens = AbstractTokenService.getInstance(OutlookTokenService.class);
          rememberMeOutlook = outlookTokens.createToken(credentials);

          // Set initialized token cookie if we authenticated successfully
          Cookie cookie = new Cookie(OutlookTokenService.COOKIE_NAME, rememberMeOutlook);
          cookie.setPath(uri);
          cookie.setMaxAge((int) outlookTokens.getValidityTime());
          httpRes.addCookie(cookie);

          if (LOG.isDebugEnabled()) {
            LOG.debug("Cookie " + OutlookTokenService.COOKIE_NAME + " initialized with user (" + credentials.getUsername()
                + ") credentials for " + uri);
          }
        }
      } // else, cookie already initialized
    }

    if (httpReq.getRemoteUser() == null) {
      // Clear token cookie if we did not authenticated
      Cookie cookie = new Cookie(OutlookTokenService.COOKIE_NAME, "");
      cookie.setPath(uri);
      cookie.setMaxAge(0);
      httpRes.addCookie(cookie);

      // If user still not authenticated, we escape query in URL like the following one
      // /portal/intranet/outlook?et=&_host_Info=Outlook|Web|16.01|en-US|376949ab-551f-da42-aa10-448870701915|
      // then redirect to fixed URL.

      String query = httpReq.getQueryString();

      if (query != null) {
        if (query.indexOf("_host_Info") > 0) {
          StringBuilder fixedQuery = new StringBuilder();
          for (String qp : query.split("&")) {
            if (fixedQuery.length() > 0) {
              fixedQuery.append('&');
            }
            int qpdi = qp.indexOf("=");
            if (qpdi > 0) {
              fixedQuery.append(qp.substring(0, ++qpdi));
              if (qpdi < qp.length()) {
                fixedQuery.append(URLEncoder.encode(qp.substring(qpdi), "ISO-8859-1"));
              } // else - empty parameter
            } else {
              // parameter w/o value goes as-is
              fixedQuery.append(qp);
            }
          }
          // wrapping request w/ fixed query string
          httpReq = new FixedQueryRequest(httpReq, fixedQuery.toString());
        }
      }
    }

    chain.doFilter(httpReq, httpRes);
  }

}
