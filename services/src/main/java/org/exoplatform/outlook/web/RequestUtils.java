
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
package org.exoplatform.outlook.web;

import org.apache.commons.lang3.time.FastDateFormat;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: RequestUtils.java 00000 Sep 7, 2016 pnedonosko $
 */
public class RequestUtils {

  /** The Constant EXPIRES_DATE_FORMAT. */
  public static final FastDateFormat EXPIRES_DATE_FORMAT            = FastDateFormat.getInstance("EEE, dd MMM yyyy HH:mm:ss zzz",
                                                                                                 TimeZone.getTimeZone("GMT"));

  /** The Constant SAME_SITE_NONE_ATTRIBUTE_VALUE */
  public static final String         SAME_SITE_NONE_ATTRIBUTE_VALUE = "None";

  /**
   * Instantiates a new request utils.
   */
  private RequestUtils() {
  }

  /**
   * Read cookie value from the request or returns null.
   *
   * @param req the incoming request
   * @param name {@link String}
   * @return the token
   */
  public static String getCookie(HttpServletRequest req, String name) {
    String val = null;
    Cookie[] cookies = req.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (name.equals(cookie.getName())) {
          val = cookie.getValue();
          if (val != null) {
            break;
          }
        }
      }
    }
    return val;
  }

  /**
   * Adds cookie to http servlet response.
   *
   * @param response the response
   * @param cookie the cookie
   * @param sameSite the same site
   */
  public static void addCookie(HttpServletResponse response, Cookie cookie, String sameSite) {

    StringBuilder c = new StringBuilder(64 + cookie.getValue().length());

    c.append(cookie.getName());
    c.append('=');
    c.append(cookie.getValue());

    appendToCookie(c, "domain", cookie.getDomain());
    appendToCookie(c, "path", cookie.getPath());
    appendToCookie(c, "SameSite", sameSite);

    if (cookie.getSecure()) {
      c.append("; secure");
    }
    if (cookie.isHttpOnly()) {
      c.append("; HttpOnly");
    }
    if (cookie.getMaxAge() >= 0) {
      appendToCookie(c, "Expires", getExpires(cookie.getMaxAge()));
    }

    response.addHeader("Set-Cookie", c.toString());
  }

  private static String getExpires(int maxAge) {
    if (maxAge < 0) {
      return "";
    }
    Calendar expireDate = Calendar.getInstance();
    expireDate.setTime(new Date());
    expireDate.add(Calendar.SECOND, maxAge);

    return EXPIRES_DATE_FORMAT.format(expireDate);
  }

  private static void appendToCookie(StringBuilder cookie, String key, String value) {
    if (key == null || value == null || key.trim().equals("") || value.trim().equals("")) {
      return;
    }

    cookie.append("; ");
    cookie.append(key);
    cookie.append('=');
    cookie.append(value);
  }

}
