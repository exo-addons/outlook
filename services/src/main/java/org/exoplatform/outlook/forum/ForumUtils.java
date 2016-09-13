
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
import org.exoplatform.outlook.OutlookServiceImpl;
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

  public static final String CATEGORY       = "category".intern();

  public static final String FORUM          = "forum".intern();

  public static final String TOPIC          = "topic".intern();

  public static final String POST           = "post".intern();

  public static final String TAG            = "Tag".intern();

  public static final String COMMA          = ",".intern();

  public static final String SLASH          = "/".intern();

  public static final String EMPTY_STR      = "".intern();

  public static final String SPACE_GROUP_ID = SpaceUtils.SPACE_GROUP.replace(SLASH, EMPTY_STR);

  public static final int    MAXSIGNATURE   = 300;

  public static final int    MAXTITLE       = 100;

  public static final long   MAXMESSAGE     = 10000;

  protected static final Log LOG            = ExoLogger.getLogger(ForumUtils.class);

  /**
   * 
   */
  private ForumUtils() {
  }

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

  public static boolean isEmpty(String str) {
    if (str == null || str.trim().length() == 0)
      return true;
    else
      return false;
  }

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

  public static boolean isArrayEmpty(String[] strs) {
    if (strs == null || strs.length == 0 || (strs.length == 1 && strs[0].trim().length() <= 0))
      return true;
    return false;
  }

  public static String[] arraysMerge(String[] strs1, String[] strs2) {
    if (isArrayEmpty(strs1))
      return strs2;
    if (isArrayEmpty(strs2))
      return strs1;
    Set<String> set = new HashSet<String>(Arrays.asList(strs1));
    set.addAll(Arrays.asList(strs2));
    return set.toArray(new String[set.size()]);
  }

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
