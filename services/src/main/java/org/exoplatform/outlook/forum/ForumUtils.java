
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
package org.exoplatform.outlook.forum;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.webui.application.WebuiRequestContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

/**
 * Copy of required methods of Forum's ForumUtils class from the webapp artifact.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ForumUtils.java 00000 Sep 9, 2016 pnedonosko $
 * 
 */
public class ForumUtils {

  /** The Constant CATEGORY. */
  public static final String CATEGORY       = "category".intern();

  /** The Constant FORUM. */
  public static final String FORUM          = "forum".intern();

  /** The Constant TOPIC. */
  public static final String TOPIC          = "topic".intern();

  /** The Constant POST. */
  public static final String POST           = "post".intern();

  /** The Constant TAG. */
  public static final String TAG            = "Tag".intern();

  /** The Constant COMMA. */
  public static final String COMMA          = ",".intern();

  /** The Constant SLASH. */
  public static final String SLASH          = "/".intern();

  /** The Constant EMPTY_STR. */
  public static final String EMPTY_STR      = "".intern();

  /** The Constant SPACE_GROUP_ID. */
  public static final String SPACE_GROUP_ID = SpaceUtils.SPACE_GROUP.replace(SLASH, EMPTY_STR);

  /** The Constant MAXSIGNATURE. */
  public static final int    MAXSIGNATURE   = 300;

  /** The Constant MAXTITLE. */
  public static final int    MAXTITLE       = 100;

  /** The Constant MAXMESSAGE. */
  public static final long   MAXMESSAGE     = 10000;

  /** The Constant LOG. */
  protected static final Log LOG            = ExoLogger.getLogger(ForumUtils.class);

  /**
   * Instantiates a new forum utils.
   */
  private ForumUtils() {
  }

  /**
   * Gets the censored keyword.
   *
   * @param stringKey the string key
   * @return the censored keyword
   */
  public static String[] getCensoredKeyword(String stringKey) {
    if (!isEmpty(stringKey)) {
      String str = EMPTY_STR;
      while (!stringKey.equals(str)) {
        str = stringKey;
        stringKey = stringKey.toLowerCase()
                             .replaceAll(";", COMMA)
                             .replaceAll(COMMA + " ", COMMA)
                             .replaceAll(" " + COMMA, COMMA)
                             .replaceAll(COMMA + COMMA, COMMA);
        if (stringKey.indexOf(COMMA) == 0) {
          stringKey = stringKey.replaceFirst(COMMA, EMPTY_STR);
        }
      }
      return stringKey.trim().split(COMMA);
    }
    return new String[] {};
  }

  /**
   * Checks if is empty.
   *
   * @param str the str
   * @return true, if is empty
   */
  public static boolean isEmpty(String str) {
    if (str == null || str.trim().length() == 0)
      return true;
    else
      return false;
  }

  /**
   * Builds the forum link.
   *
   * @param url the url
   * @param type the type
   * @param id the id
   * @return the string
   */
  private static String buildForumLink(String url, String type, String id) {
    StringBuilder link = new StringBuilder(url);
    if (!isEmpty(type) && !isEmpty(id)) {
      if (link.lastIndexOf(SLASH) == (link.length() - 1))
        link.append(type);
      else
        link.append(SLASH).append(type);
      if (!id.equals(Utils.FORUM_SERVICE))
        link.append(SLASH).append(id);
    }
    return link.toString();
  }

  /**
   * Created forum link.
   *
   * @param type the type
   * @param id the id
   * @param isPrivate the is private
   * @return the string
   */
  public static String createdForumLink(String type, String id, boolean isPrivate) {
    try {
      PortalRequestContext portalContext = Util.getPortalRequestContext();
      String fullUrl = ((HttpServletRequest) portalContext.getRequest()).getRequestURL().toString();
      String host = fullUrl.substring(0, fullUrl.indexOf(SLASH, 8));
      return new StringBuffer(host).append(createdSubForumLink(type, id, isPrivate)).toString();
    } catch (Exception e) {
      return id;
    }
  }

  /**
   * Created sub forum link.
   *
   * @param type the type
   * @param id the id
   * @param isPrivate the is private
   * @return the string
   */
  public static String createdSubForumLink(String type, String id, boolean isPrivate) {
    try {
      String containerName = CommonsUtils.getService(ExoContainerContext.class).getPortalContainerName();
      String pageNodeSelected = Util.getUIPortal().getSelectedUserNode().getURI();
      PortalRequestContext portalContext = Util.getPortalRequestContext();
      return buildLink(portalContext.getPortalURI(), containerName, pageNodeSelected, type, id, isPrivate);
    } catch (Exception e) {
      return id;
    }
  }

  /**
   * Builds the link.
   *
   * @param portalURI the portal URI
   * @param containerName the container name
   * @param selectedNode the selected node
   * @param type the type
   * @param id the id
   * @param isPrivate the is private
   * @return the string
   */
  public static String buildLink(String portalURI,
                                 String containerName,
                                 String selectedNode,
                                 String type,
                                 String id,
                                 boolean isPrivate) {
    StringBuilder sb = new StringBuilder();
    portalURI = portalURI.concat(selectedNode).concat(SLASH);
    if (!isPrivate) {
      sb.append(buildForumLink(portalURI, type, id));
    } else {
      String host = portalURI.substring(0, portalURI.indexOf(containerName) - 1);
      sb.append(host)
        .append(SLASH)
        .append(containerName)
        .append(SLASH)
        .append("login?&initialURI=")
        .append(buildForumLink(portalURI.replaceFirst(host, EMPTY_STR), type, id))
        .toString();
    }
    return sb.toString();
  }

  /**
   * Split for forum.
   *
   * @param str the str
   * @return the string[]
   */
  public static String[] splitForForum(String str) {
    if (!isEmpty(str)) {
      str = StringUtils.remove(str, " ");
      if (str.contains(COMMA)) {
        str = str.replaceAll(";", COMMA);
        return str.trim().split(COMMA);
      } else {
        str = str.replaceAll(COMMA, ";");
        return str.trim().split(";");
      }
    } else
      return new String[] { EMPTY_STR };
  }

  /**
   * Checks if is array empty.
   *
   * @param strs the strs
   * @return true, if is array empty
   */
  public static boolean isArrayEmpty(String[] strs) {
    if (strs == null || strs.length == 0 || (strs.length == 1 && strs[0].trim().length() <= 0))
      return true;
    return false;
  }

  /**
   * Arrays merge.
   *
   * @param strs1 the strs 1
   * @param strs2 the strs 2
   * @return the string[]
   */
  public static String[] arraysMerge(String[] strs1, String[] strs2) {
    if (isArrayEmpty(strs1))
      return strs2;
    if (isArrayEmpty(strs2))
      return strs1;
    Set<String> set = new HashSet<String>(Arrays.asList(strs1));
    set.addAll(Arrays.asList(strs2));
    return set.toArray(new String[set.size()]);
  }

  /**
   * Gets the default mail.
   *
   * @return the default mail
   */
  public static MessageBuilder getDefaultMail() {
    MessageBuilder messageBuilder = new MessageBuilder();
    try {
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      ResourceBundle res = context.getApplicationResourceBundle();
      messageBuilder.setContent(res.getString("UINotificationForm.label.notifyEmailContentDefault"));
      String header = res.getString("UINotificationForm.label.notifyEmailHeaderSubjectDefault");
      messageBuilder.setHeaderSubject((isEmpty(header)) ? EMPTY_STR : header);

      messageBuilder.setTypes(res.getString("UIForumPortlet.label.category"),
                              res.getString("UIForumPortlet.label.forum"),
                              res.getString("UIForumPortlet.label.topic"),
                              res.getString("UIForumPortlet.label.post"));
    } catch (Exception e) {
      LOG.warn("Failed to get resource bundle for Forum default content email notification !", e);
    }
    return messageBuilder;
  }

}
