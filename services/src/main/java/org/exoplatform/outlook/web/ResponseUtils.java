package org.exoplatform.outlook.web;

import org.apache.commons.lang3.time.FastDateFormat;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class ResponseUtils {

  /** The Constant EXPIRES_DATE_FORMAT. */
  public static final FastDateFormat EXPIRES_DATE_FORMAT            = FastDateFormat.getInstance("EEE, dd MMM yyyy HH:mm:ss zzz",
                                                                                                 TimeZone.getTimeZone("GMT"));

  /** The Constant SAME_SITE_NONE_ATTRIBUTE_VALUE */
  public static final String         SAME_SITE_NONE_ATTRIBUTE_VALUE = "None";

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
