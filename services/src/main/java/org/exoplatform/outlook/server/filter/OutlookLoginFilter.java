
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

import org.exoplatform.web.filter.Filter;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Filter to catch login requests to outlook site and redirect to dedicated login page.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookLoginFilter.java 00000 Jul 1, 2016 pnedonosko $
 * 
 */
public class OutlookLoginFilter implements Filter {

  /** The Constant LOG. */
  protected static final Logger LOG                 = LoggerFactory.getLogger(OutlookLoginFilter.class);

  /** The Constant OUTLOOK_LOGIN. */
  public static final String    OUTLOOK_LOGIN       = "/outlook/login";

  /** The Constant OUTLOOK_INITIAL_URI. */
  public static final String    OUTLOOK_INITIAL_URI = "initialURI=%2Fportal%2Fintranet%2Foutlook";

  /**
   * {@inheritDoc}
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                            ServletException {
    HttpServletRequest httpReq = (HttpServletRequest) request;
    HttpServletResponse httpRes = (HttpServletResponse) response;
    String query = httpReq.getQueryString();

    if (query != null && query.startsWith(OUTLOOK_INITIAL_URI)) {
      // redirect to right login page
      httpRes.sendRedirect(new StringBuilder(OUTLOOK_LOGIN).append('?').append(query).toString());
    } else {
      chain.doFilter(request, response);
    }
  }

}
