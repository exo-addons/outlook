/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.outlook.portlet;

import juzu.Path;
import juzu.Resource;
import juzu.Response;
import juzu.SessionScoped;
import juzu.View;
import juzu.request.RequestContext;

import org.exoplatform.commons.juzu.ajax.Ajax;
import org.exoplatform.outlook.BadParameterException;
import org.exoplatform.outlook.OutlookEmail;
import org.exoplatform.outlook.OutlookMessage;
import org.exoplatform.outlook.OutlookService;
import org.exoplatform.outlook.OutlookSpace;
import org.exoplatform.outlook.OutlookUser;
import org.exoplatform.outlook.common.ResourceBundleSerializer;
import org.exoplatform.outlook.jcr.File;
import org.exoplatform.outlook.jcr.Folder;
import org.exoplatform.outlook.security.OutlookTokenService;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.web.security.security.CookieTokenService;
import org.gatein.wci.ServletContainer;
import org.gatein.wci.ServletContainerFactory;
import org.gatein.wci.security.Credentials;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.portlet.PortletPreferences;
import javax.servlet.http.Cookie;

/**
 * Juzu controller for Outlook read pane app.<br>
 * 
 * Created by The eXo Platform SAS<br>
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: Outlook.java 00000 May 17, 2016 pnedonosko $
 * 
 */
@SessionScoped
@PermitAll
public class Outlook {

  /** . */
  private final static String GMT_TIME_ZONE_ID          = "GMT";

  /** . */
  private final static String COOKIE_DATE_FORMAT_STRING = "EEE, dd-MMM-yyyy HH:mm:ss z";

  /** . */
  private final static String NAME_VALUE_DELIMITER      = "=";

  /** . */
  private final static String ATTRIBUTE_DELIMITER       = "; ";

  /** . */
  private final static String COOKIE_HEADER_NAME        = "Set-Cookie";

  /** . */
  private final static String PATH_ATTRIBUTE_NAME       = "Path";

  /** . */
  private final static String EXPIRES_ATTRIBUTE_NAME    = "Expires";

  /** . */
  private final static String MAXAGE_ATTRIBUTE_NAME     = "Max-Age";

  /** . */
  private final static String DOMAIN_ATTRIBUTE_NAME     = "Domain";

  private static final Log    LOG                       = ExoLogger.getLogger(Outlook.class);

  public class Status extends ActivityStatus {

    protected Status(String userTitle, String spaceName, String link) {
      super(userTitle, spaceName, link);
    }

    public String getConvertedToUserActivityMessage() {
      String msg = i18n.getString("Outlook.convertedToUserActivity");
      return msg.replace("{SPACE_NAME}", spaceName);
    }

  }

  @Inject
  Provider<PortletPreferences>                              preferences;

  @Inject
  OutlookService                                            outlook;

  @Inject
  CookieTokenService                                        rememberMeTokens;

  @Inject
  OutlookTokenService                                       outlookTokens;

  @Inject
  @Path("index.gtmpl")
  org.exoplatform.outlook.portlet.templates.index           index;

  @Inject
  @Path("saveAttachment.gtmpl")
  org.exoplatform.outlook.portlet.templates.saveAttachment  saveAttachment;

  @Inject
  @Path("savedAttachment.gtmpl")
  org.exoplatform.outlook.portlet.templates.savedAttachment savedAttachment;

  @Inject
  @Path("folders.gtmpl")
  org.exoplatform.outlook.portlet.templates.folders         folders;

  @Inject
  @Path("addFolderDialog.gtmpl")
  org.exoplatform.outlook.portlet.templates.addFolderDialog addFolderDialog;

  @Inject
  @Path("addAttachment.gtmpl")
  org.exoplatform.outlook.portlet.templates.addAttachment   addAttachment;

  @Inject
  @Path("postStatus.gtmpl")
  org.exoplatform.outlook.portlet.templates.postStatus      postStatus;

  @Inject
  @Path("startDiscussion.gtmpl")
  org.exoplatform.outlook.portlet.templates.startDiscussion startDiscussion;

  @Inject
  @Path("search.gtmpl")
  org.exoplatform.outlook.portlet.templates.search          search;

  @Inject
  @Path("userInfo.gtmpl")
  org.exoplatform.outlook.portlet.templates.userInfo        userInfo;

  @Inject
  @Path("convertToStatus.gtmpl")
  org.exoplatform.outlook.portlet.templates.convertToStatus convertToStatus;

  @Inject
  @Path("convertedStatus.gtmpl")
  org.exoplatform.outlook.portlet.templates.convertedStatus convertedStatus;

  @Inject
  @Path("convertToWiki.gtmpl")
  org.exoplatform.outlook.portlet.templates.convertToWiki   convertToWiki;

  @Inject
  @Path("convertToForum.gtmpl")
  org.exoplatform.outlook.portlet.templates.convertToForum  convertToForum;

  @Inject
  @Path("home.gtmpl")
  org.exoplatform.outlook.portlet.templates.home            home;

  @Inject
  @Path("error.gtmpl")
  org.exoplatform.outlook.portlet.templates.error           error;

  @Inject
  @Path("errorMessage.gtmpl")
  org.exoplatform.outlook.portlet.templates.errorMessage    errorMessage;

  @Inject
  @Path("convertForm.gtmpl")
  org.exoplatform.outlook.portlet.templates.convertForm     convertForm;

  @Inject
  ResourceBundle                                            i18n;

  @Inject
  ResourceBundleSerializer                                  i18nJSON;

  DateFormat                                                dateFormat    = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  private final Map<String, MenuItem>                       allMenuItems  = new LinkedHashMap<String, MenuItem>();

  private final Set<MenuItem>                               rootMenuItems = new LinkedHashSet<MenuItem>();

  public Outlook() {
    addRootMenuItem(new MenuItem("home"));
    addRootMenuItem(new MenuItem("saveAttachment"));
    addRootMenuItem(new MenuItem("addAttachment"));

    MenuItem convertTo = new MenuItem("convertTo");
    convertTo.addSubmenu("convertToStatus");
    convertTo.addSubmenu("convertToWiki");
    convertTo.addSubmenu("convertToForum");
    addRootMenuItem(convertTo);

    MenuItem create = new MenuItem("create");
    create.addSubmenu("postStatus");
    create.addSubmenu("startDiscussion");
    addRootMenuItem(create);

    addRootMenuItem(new MenuItem("search"));
    addRootMenuItem(new MenuItem("userInfo"));
  }

  private void addRootMenuItem(MenuItem item) {
    rootMenuItems.add(item);
    addMenuItem(item);
  }

  private void addMenuItem(MenuItem item) {
    allMenuItems.put(item.getName(), item);
    if (item.hasSubmenu()) {
      for (MenuItem sm : item.getSubmenu()) {
        addMenuItem(sm);
      }
    }
  }

  @View
  public Response index(String command, RequestContext resourceContext) {
    Collection<MenuItem> menu = new ArrayList<MenuItem>();
    if (command == null) {
      // XXX we cannot access request parameters via Juzu's request when running in Portlet bridge
      // Request request = Request.getCurrent();
      // Map<String, RequestParameter> parameters = resourceContext.getParameters();
      // RequestParameter cparam = parameters.get("command");
      // thus we rely on Portal request
      command = requestCommand();
    }

    if (command == null) {
      // if nothing specified - show all menu tabs
      for (MenuItem m : rootMenuItems) {
        menu.add(userMenuItem(m));
      }
    } else {
      // show only requested menu tabs
      for (String mn : command.split(",")) {
        MenuItem m = allMenuItems.get(mn);
        if (m != null) {
          menu.add(userMenuItem(m));
        } else {
          LOG.warn("Skipping not defined menu name '" + mn + "' for user '"
              + resourceContext.getSecurityContext().getRemoteUser() + "'");
        }
      }
    }

    try {
      return index.with().menu(menu).messages(i18nJSON.toJSON(i18n)).ok();
    } catch (Throwable e) {
      LOG.error("Portlet error: " + e.getMessage(), e);
      return error(e.getMessage());
    }
  }

  @View
  public Response error(String message) {
    return error.with().message(message).ok();
  }

  // ********** Home page **********

  @Ajax
  @Resource
  public Response homeForm() {
    try {
      return home.ok();
    } catch (Throwable e) {
      LOG.error("Error showing Home page", e);
      return errorMessage(e.getMessage(), 500);
    }
  }

  // *********** Save Attachment command **********

  @Ajax
  @Resource
  public Response saveAttachmentForm() {
    try {
      return saveAttachment.with().spaces(outlook.getUserSpaces()).ok();
    } catch (Throwable e) {
      LOG.error("Error showing save attachments form", e);
      return errorMessage(e.getMessage(), 500);
    }
  }

  @Ajax
  @Resource
  public Response folders(String groupId, String path) {
    try {
      return folders.with().folder(outlook.getSpace(groupId).getFolder(path)).ok();
    } catch (BadParameterException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Error reading folder " + path + ". " + e.getMessage());
      }
      return errorMessage(e.getMessage(), 400);
    } catch (Throwable e) {
      LOG.error("Error reading folder " + path, e);
      return errorMessage(e.getMessage(), 500);
    }
  }

  @Ajax
  @Resource
  public Response addFolderDialog() {
    try {
      return addFolderDialog.ok();
    } catch (Throwable e) {
      LOG.error("Error showing add folder dialog", e);
      return errorMessage(e.getMessage(), 500);
    }
  }

  @Ajax
  @Resource
  public Response addFolder(String groupId, String path, String name) {
    if (name != null && name.length() > 0) {
      try {
        Folder folder = outlook.getSpace(groupId).getFolder(path);
        folder.addSubfolder(name);
        return folders.with().folder(folder).ok();
      } catch (BadParameterException e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Error adding folder " + path + "/" + name + ". " + e.getMessage());
        }
        return errorMessage(e.getMessage(), 400);
      } catch (Throwable e) {
        LOG.error("Error adding folder " + path + "/" + name, e);
        return errorMessage(e.getMessage(), 500);
      }
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Null or zero-length folder name to add in " + path);
      }
      return errorMessage("Folder name required", 400);
    }
  }

  @Ajax
  @Resource
  public Response saveAttachment(String groupId,
                                 String path,
                                 String comment,
                                 String ewsUrl,
                                 String userEmail,
                                 String userName,
                                 String messageId,
                                 String attachmentToken,
                                 String attachmentIds,
                                 RequestContext context) {
    if (groupId != null && path != null && ewsUrl != null && userEmail != null && messageId != null
        && attachmentToken != null && attachmentIds != null) {
      try {
        OutlookSpace space = outlook.getSpace(groupId);
        if (space != null) {
          // Remove empty attachments in the array
          List<String> attachments = new ArrayList<String>();
          for (String aid : attachmentIds.split(",")) {
            aid = aid.trim();
            if (aid.length() > 0) {
              attachments.add(aid);
            }
          }
          if (attachments.size() > 0) {
            Folder folder = space.getFolder(path);
            OutlookUser user = outlook.getUser(userEmail, userName, ewsUrl);
            List<File> files = outlook.saveAttachment(space,
                                                      folder,
                                                      user,
                                                      comment,
                                                      messageId,
                                                      attachmentToken,
                                                      attachments.toArray(new String[attachments.size()]));
            return savedAttachment.with().files(files).ok();
          } else {
            // TODO return client error?
            return savedAttachment.with().files(Collections.emptyList()).ok();
          }
        } else {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Error saving attachment: space not found " + groupId + ". OutlookUser " + userEmail);
          }
          return errorMessage("Error saving attachment: space not found " + groupId, 404);
        }
      } catch (Throwable e) {
        LOG.error("Error saving attachment in the space " + groupId + ": " + e.getMessage(), e);
        return errorMessage("Error saving attachment in the space. Please contact your administrator.", 500);
      }
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Error in saving attachment request: spaceName='" + groupId + "' path=" + path + " ewsUrl='" + ewsUrl
            + "' userEmail='" + userEmail + "' messageId='" + messageId + "' attachmentToken(size)='"
            + (attachmentToken != null ? attachmentToken.length() : "null") + "' attachmentIds='" + attachmentIds + "'");
      }
      return errorMessage("Error in saving attachment request. Please reload the page.", 400);
    }
  }

  // *************** Attach Document command ***********

  @Ajax
  @Resource
  public Response addAttachmentForm() {
    try {
      return addAttachment.with().spaces(outlook.getUserSpaces()).ok();
    } catch (Throwable e) {
      LOG.error("Error showing add attachments form", e);
      return errorMessage(e.getMessage(), 500);
    }
  }

  // *************** Post Status command ***********

  @Ajax
  @Resource
  public Response postStatusForm() {
    try {
      return postStatus.ok();
    } catch (Throwable e) {
      LOG.error("Error showing status post form", e);
      return errorMessage(e.getMessage(), 500);
    }
  }

  // *************** Start Discussion command ***********

  @Ajax
  @Resource
  public Response startDiscussionForm() {
    try {
      return startDiscussion.ok();
    } catch (Throwable e) {
      LOG.error("Error showing discussion start form", e);
      return errorMessage(e.getMessage(), 500);
    }
  }

  // *************** Search command ***********

  @Ajax
  @Resource
  public Response searchForm() {
    try {
      return search.ok();
    } catch (Throwable e) {
      LOG.error("Error showing search form", e);
      return errorMessage(e.getMessage(), 500);
    }
  }

  // *************** OutlookUser info command ***********

  @Ajax
  @Resource
  public Response userInfoForm() {
    try {
      return userInfo.ok();
    } catch (Throwable e) {
      LOG.error("Error showing search form", e);
      return errorMessage(e.getMessage(), 500);
    }
  }

  // *************** Convert To Status command ***********

  @Ajax
  @Resource
  public Response convertToStatusForm() {
    try {
      return convertToStatus.with().spaces(outlook.getUserSpaces()).ok();
    } catch (Throwable e) {
      LOG.error("Error showing conversion to status form", e);
      return errorMessage(e.getMessage(), 500);
    }
  }

  @Ajax
  @Resource
  public Response convertToStatus(String groupId,
                                  String messageId,
                                  String subject,
                                  String body,
                                  String created,
                                  String modified,
                                  String userName,
                                  String userEmail,
                                  String fromName,
                                  String fromEmail) {
    try {
      OutlookUser user = outlook.getUser(userEmail, userName, null);
      OutlookEmail from = outlook.getAddress(fromEmail, fromName);
      Calendar createdDate = Calendar.getInstance();
      createdDate.setTime(dateFormat.parse(created));
      Calendar modifiedDate = Calendar.getInstance();
      modifiedDate.setTime(dateFormat.parse(modified));
      OutlookMessage message = outlook.getMessage(messageId, user, from, null, createdDate, modifiedDate, subject, body);

      if (groupId != null && groupId.length() > 0) {
        // space activity requested
        OutlookSpace space = outlook.getSpace(groupId);
        if (space != null) {
          ExoSocialActivity activity = space.postActivity(message);
          return convertedStatus.with().status(new Status(null, space.getGroupId(), activity.getPermaLink())).ok();
        } else {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Error converting message to activity status : space not found " + groupId + ". OutlookUser "
                + userEmail);
          }
          return errorMessage("Error converting message to activity status : space not found " + groupId, 404);
        }
      } else {
        // user activity requested
        ExoSocialActivity activity = user.postActivity(message);
        return convertedStatus.with().status(new Status(user.getLocalUser(), null, activity.getPermaLink())).ok();
      }
    } catch (Throwable e) {
      LOG.error("Error converting message to activity status for " + userEmail, e);
      return errorMessage(e.getMessage(), 500);
    }
  }

  // *************** Convert To Wiki command ***********

  @Ajax
  @Resource
  public Response convertToWikiForm() {
    try {
      return convertToWiki.ok();
    } catch (Throwable e) {
      LOG.error("Error showing conversion to wiki form", e);
      return errorMessage(e.getMessage(), 500);
    }
  }

  // *************** Convert To Forum command ***********

  @Ajax
  @Resource
  public Response convertToForumForm() {
    try {
      return convertToForum.ok();
    } catch (Throwable e) {
      LOG.error("Error showing conversion to forum form", e);
      return errorMessage(e.getMessage(), 500);
    }
  }

  // *************** Post-Login *************

  @Ajax
  @Resource
  public Response rememberme(RequestContext context) throws IOException {
    // we assume portal's LoginServlet with forced rememberme already worked here
    // and we will save a dedicated cookie for long period (1 year)

    String userName = context.getSecurityContext().getRemoteUser();

    String rememberMe = null;
    for (Cookie c : context.getHttpContext().getCookies()) {
      if (c.getName().equals("rememberme")) {
        rememberMe = c.getValue();
      }
    }

    if (rememberMe != null) {
      Credentials credentials = rememberMeTokens.validateToken(rememberMe, false);
      if (credentials != null) {
        try {
          PortalRequestContext portalRequest = Util.getPortalRequestContext();
          if (portalRequest != null) {

            String rememberMeOutlook = outlookTokens.createToken(credentials);
            if (LOG.isDebugEnabled()) {
              LOG.debug("Found a remembermeoutlook request parameter, created a persistent token " + rememberMeOutlook
                  + " for it and set it up in the next response");
            }

            ServletContainer servletContainer = ServletContainerFactory.getServletContainer();
            servletContainer.login(portalRequest.getRequest(), portalRequest.getResponse(), credentials);

            // set cookie "remembermeoutlook"
            String cookie = buildCookieValue(OutlookTokenService.COOKIE_NAME,
                                             rememberMeOutlook,
                                             portalRequest.getRequest().getServerName(),
                                             portalRequest.getPortalContextPath(),
                                             (int) outlookTokens.getValidityTime());

            return Response.ok().withHeader(COOKIE_HEADER_NAME, cookie);
          } else {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Error saving user: portal request not found for " + userName);
            }
            return errorMessage("Error saving user: portal request not found", 400);
          }
        } catch (Exception e) {
          // Could not authenticate
          if (LOG.isDebugEnabled()) {
            LOG.debug("Error saving user " + userName + ". " + e.getMessage(), e);
          }
          return errorMessage("Error saving user " + userName + ". " + e.getMessage(), 500);
        }
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug("OutlookUser not authenticated " + userName);
        }
        return errorMessage("Not authenticated", 401);
      }
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("RememberMe token not found for " + userName);
      }
      return errorMessage("Authentication not complete", 401);
    }
  }

  // ***************** internals *****************

  Response errorMessage(String text, int status) {
    return Response.content(status, text != null ? text : "");
    // return errorMessage.with().message(text != null ? text : "").status(status);
  }

  MenuItem userMenuItem(MenuItem item) {
    MenuItem userItem = item.clone();
    userItem.setTitle(i18n.getString(new StringBuilder("Outlook.command.").append(userItem.name).toString()));
    if (userItem.hasSubmenu()) {
      for (MenuItem sm : userItem.getSubmenu()) {
        sm.setTitle(i18n.getString(new StringBuilder("Outlook.command.").append(sm.name).toString()));
      }
    }
    return userItem;
  }

  private String requestCommand() {
    PortalRequestContext portalRequest = Util.getPortalRequestContext();
    if (portalRequest != null) {
      // try portal HTTP request for provider id
      return portalRequest.getRequestParameter("command");
    }
    return null;
  }

  private static String buildCookieValue(String name, String value, String domain, String path, int maxAge) {
    StringBuilder sb = new StringBuilder(name).append(NAME_VALUE_DELIMITER);
    if (value != null && !value.isEmpty()) {
      sb.append(value);
    }
    if (domain != null && !domain.isEmpty()) {
      sb.append(ATTRIBUTE_DELIMITER);
      sb.append(DOMAIN_ATTRIBUTE_NAME).append(NAME_VALUE_DELIMITER).append(domain);
    }
    if (path != null && !path.isEmpty()) {
      sb.append(ATTRIBUTE_DELIMITER);
      sb.append(PATH_ATTRIBUTE_NAME).append(NAME_VALUE_DELIMITER).append(path);
    }

    if (maxAge >= 0) {
      sb.append(ATTRIBUTE_DELIMITER);
      sb.append(MAXAGE_ATTRIBUTE_NAME).append(NAME_VALUE_DELIMITER).append(maxAge);
      sb.append(ATTRIBUTE_DELIMITER);
      // Value is in seconds. So take 'now' and add that many seconds, and
      // that's our expiration date:
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.SECOND, maxAge);
      Date expires = cal.getTime();
      String formatted = toCookieDate(expires);
      sb.append(EXPIRES_ATTRIBUTE_NAME).append(NAME_VALUE_DELIMITER).append(formatted);
    }
    return sb.toString();
  }

  /**
   * Formats a date into a cookie date compatible string (Netscape's
   * specification).
   * 
   * @param date
   *          the date to format
   * @return an HTTP 1.0/1.1 Cookie compatible date string (GMT-based).
   */
  private static String toCookieDate(Date date) {
    TimeZone tz = TimeZone.getTimeZone(GMT_TIME_ZONE_ID);
    DateFormat fmt = new SimpleDateFormat(COOKIE_DATE_FORMAT_STRING, Locale.US);
    fmt.setTimeZone(tz);
    return fmt.format(date);
  }
}