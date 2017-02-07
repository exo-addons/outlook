
/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
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
package org.exoplatform.outlook.rest;

import org.exoplatform.services.rest.resource.ResourceContainer;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: RESTServiceBase.java 00000 Feb 6, 2017 pnedonosko $
 */
abstract class RESTServiceBase implements ResourceContainer {

  /**
   * Gets the client ip addr.
   *
   * @param request the request
   * @return the client ip addr
   */
  protected String getClientIpAddr(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (isValidName(ip)) {
      return ip;
    }
    ip = request.getHeader("Proxy-Client-IP");
    if (isValidName(ip)) {
      return ip;
    }
    ip = request.getHeader("WL-Proxy-Client-IP");
    if (isValidName(ip)) {
      return ip;
    }
    ip = request.getHeader("HTTP_CLIENT_IP");
    if (isValidName(ip)) {
      return ip;
    }
    ip = request.getHeader("HTTP_X_FORWARDED_FOR");
    if (isValidName(ip)) {
      return ip;
    }
    ip = request.getHeader("HTTP_X_FORWARDED");
    if (isValidName(ip)) {
      return ip;
    }
    ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
    if (isValidName(ip)) {
      return ip;
    }
    // http://stackoverflow.com/questions/1634782/what-is-the-most-accurate-way-to-retrieve-a-users-correct-ip-address-in-php
    ip = request.getHeader("HTTP_FORWARDED_FOR");
    if (isValidName(ip)) {
      return ip;
    }
    ip = request.getHeader("HTTP_FORWARDED");
    if (isValidName(ip)) {
      return ip;
    }
    ip = request.getHeader("REMOTE_ADDR");
    if (isValidName(ip)) {
      return ip;
    }
    // last chance to get it from Servlet request
    ip = request.getRemoteAddr();
    if (isValidName(ip)) {
      return ip;
    }
    return null;
  }

  /**
   * Gets the client host.
   *
   * @param request the request
   * @return the client host
   */
  protected String getClientHost(HttpServletRequest request) {
    String host = request.getHeader("X-Forwarded-Host");
    if (isValidName(host)) {
      return host;
    }
    host = request.getRemoteHost();
    if (isValidName(host)) {
      return host;
    }
    return null;
  }

  /**
   * Checks if is valid name.
   *
   * @param hostName the host name
   * @return true, if is valid name
   */
  protected boolean isValidName(String hostName) {
    if (hostName != null && hostName.length() > 0 && !"unknown".equalsIgnoreCase(hostName)) {
      return true;
    }
    return false;
  }

}
