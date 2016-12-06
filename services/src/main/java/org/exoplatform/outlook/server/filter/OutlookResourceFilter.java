
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

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.web.AbstractFilter;
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
 * Filter updates request to outlook resources with proper HTTP headers (for caching etc.).<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookResourceFilter.java 00000 Dec 6, 2016 pnedonosko $
 * 
 */
public class OutlookResourceFilter extends AbstractFilter {

  /** The Constant LOG. */
  protected static final Logger LOG        = LoggerFactory.getLogger(OutlookResourceFilter.class);

  /** The Constant METHOD_GET. */
  protected static final String METHOD_GET = "GET";

  /**
   * {@inheritDoc}
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                            ServletException {

    if (!PropertyManager.isDevelopping()) {
      HttpServletRequest httpReq = (HttpServletRequest) request;
      HttpServletResponse httpRes = (HttpServletResponse) response;

      String method = httpReq.getMethod();
      if (method != null && METHOD_GET.equals(method)) {
        // FYI Filter configuration already have a mapping to the path, no need type check
        // String contentType = httpReq.getContentType();
        // if (contentType.startsWith("application/javascript") || contentType.startsWith("text/css")
        // || contentType.startsWith("text/javascript")) {
        // TODO || contentType.startsWith("image/") ?
        Package myPackage = getClass().getPackage();
        if (myPackage != null) {
          String ver = myPackage.getImplementationVersion();
          if (ver != null) {
            if (ver.indexOf("Beta") > 0 || ver.indexOf("RC") > 0 || ver.indexOf("M") > 0) {
              // if it's Beta/RC/Milestone version, use 1 hour cache
              httpRes.setHeader("Cache-Control", "max-age=3600,s-maxage=3600");
            } else if (ver.endsWith("SNAPSHOT")) {
              // 20min cache for development deployments (demo server etc.)
              httpRes.setHeader("Cache-Control", "max-age=1200,s-maxage=1200");
            }
            // }
          }
        }
      }
    }
    chain.doFilter(request, response);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void destroy() {
    // nothing
  }

}
