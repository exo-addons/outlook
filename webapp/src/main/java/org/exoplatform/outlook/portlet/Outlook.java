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
package org.exoplatform.outlook.portlet;

import juzu.*;
import juzu.request.HttpContext;
import juzu.request.RequestContext;
import juzu.template.TemplateExecutionException;
import org.exoplatform.commons.juzu.ajax.Ajax;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.outlook.*;
import org.exoplatform.outlook.common.ResourceBundleSerializer;
import org.exoplatform.outlook.jcr.ContentLink;
import org.exoplatform.outlook.jcr.File;
import org.exoplatform.outlook.jcr.Folder;
import org.exoplatform.outlook.jcr.LinkResource;
import org.exoplatform.outlook.security.OutlookTokenService;
import org.exoplatform.outlook.web.RequestUtils;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.User;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.web.login.LoginServlet;
import org.exoplatform.web.login.LogoutControl;
import org.exoplatform.web.security.GateInToken;
import org.exoplatform.web.security.security.AbstractTokenService;
import org.exoplatform.web.security.security.CookieTokenService;
import org.exoplatform.wiki.mow.api.Page;
import org.gatein.wci.ServletContainer;
import org.gatein.wci.ServletContainerFactory;
import org.gatein.wci.security.Credentials;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.portlet.PortletPreferences;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Juzu controller for Outlook read pane app.<br>
 * <p>
 * Created by The eXo Platform SAS<br>
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: Outlook.java 00000 May 17, 2016 pnedonosko $
 */
@SessionScoped
@PermitAll
public class Outlook {

    /**
     * .
     */
    public final static String SOURCE_ID_ALL_SPACES = "*";

    /**
     * The Constant SOURCE_ID_PERSONAL.
     */
    public final static String SOURCE_ID_PERSONAL = "PERSONAL_DOCUMENTS";

    /**
     * .
     */
    private final static String GMT_TIME_ZONE_ID = "GMT";

    /**
     * .
     */
    private final static String COOKIE_DATE_FORMAT_STRING = "EEE, dd-MMM-yyyy HH:mm:ss z";

    /**
     * .
     */
    private final static String NAME_VALUE_DELIMITER = "=";

    /**
     * .
     */
    private final static String ATTRIBUTE_DELIMITER = "; ";

    /**
     * .
     */
    private final static String COOKIE_HEADER_NAME = "Set-Cookie";

    /**
     * .
     */
    private final static String PATH_ATTRIBUTE_NAME = "Path";

    /**
     * .
     */
    private final static String EXPIRES_ATTRIBUTE_NAME = "Expires";

    /**
     * .
     */
    private final static String MAXAGE_ATTRIBUTE_NAME = "Max-Age";

    /**
     * .
     */
    private final static String DOMAIN_ATTRIBUTE_NAME = "Domain";

    /**
     * The Constant LOG.
     */
    private static final Log LOG = ExoLogger.getLogger(Outlook.class);
    /**
     * The all menu items.
     */
    private final Map<String, MenuItem> allMenuItems = new LinkedHashMap<String, MenuItem>();
    /**
     * The root menu items.
     */
    private final Set<MenuItem> rootMenuItems = new LinkedHashSet<MenuItem>();
    /**
     * The preferences.
     */
    @Inject
    Provider<PortletPreferences> preferences;
    /**
     * The outlook.
     */
    @Inject
    OutlookService outlook;
    /**
     * The remember me tokens.
     */
    @Inject
    CookieTokenService rememberMeTokens;
    /**
     * The outlook tokens.
     */
    @Inject
    OutlookTokenService outlookTokens;
    /**
     * The content link.
     */
    @Inject
    ContentLink contentLink;
    /**
     * The index.
     */
    @Inject
    @Path("index.gtmpl")
    org.exoplatform.outlook.portlet.templates.index index;
    /**
     * The save attachment.
     */
    @Inject
    @Path("saveAttachment.gtmpl")
    org.exoplatform.outlook.portlet.templates.saveAttachment saveAttachment;
    /**
     * The saved attachment.
     */
    @Inject
    @Path("savedAttachment.gtmpl")
    org.exoplatform.outlook.portlet.templates.savedAttachment savedAttachment;
    /**
     * The folders.
     */
    @Inject
    @Path("folders.gtmpl")
    org.exoplatform.outlook.portlet.templates.folders folders;
    /**
     * The files explorer.
     */
    @Inject
    @Path("filesExplorer.gtmpl")
    org.exoplatform.outlook.portlet.templates.filesExplorer filesExplorer;
    /**
     * The files search.
     */
    @Inject
    @Path("filesSearch.gtmpl")
    org.exoplatform.outlook.portlet.templates.filesSearch filesSearch;
    /**
     * The add folder dialog.
     */
    @Inject
    @Path("addFolderDialog.gtmpl")
    org.exoplatform.outlook.portlet.templates.addFolderDialog addFolderDialog;
    /**
     * The add attachment.
     */
    @Inject
    @Path("addAttachment.gtmpl")
    org.exoplatform.outlook.portlet.templates.addAttachment addAttachment;
    /**
     * The post status.
     */
    @Inject
    @Path("postStatus.gtmpl")
    org.exoplatform.outlook.portlet.templates.postStatus postStatus;
    /**
     * The posted status.
     */
    @Inject
    @Path("postedStatus.gtmpl")
    org.exoplatform.outlook.portlet.templates.postedStatus postedStatus;
    /**
     * The start discussion.
     */
    @Inject
    @Path("startDiscussion.gtmpl")
    org.exoplatform.outlook.portlet.templates.startDiscussion startDiscussion;
    /**
     * The started discussion.
     */
    @Inject
    @Path("startedDiscussion.gtmpl")
    org.exoplatform.outlook.portlet.templates.startedDiscussion startedDiscussion;
    /**
     * The unified search.
     */
    @Inject
    @Path("unifiedSearch.gtmpl")
    org.exoplatform.outlook.portlet.templates.unifiedSearch unifiedSearch;
    /**
     * The user info.
     */
    @Inject
    @Path("userInfo.gtmpl")
    org.exoplatform.outlook.portlet.templates.userInfo userInfo;
    /**
     * The user info.
     */
    @Inject
    @Path("userInfoactivity.gtmpl")
    org.exoplatform.outlook.portlet.templates.userInfoactivity userInfoactivity;
    /**
     * The convert to status.
     */
    @Inject
    @Path("convertToStatus.gtmpl")
    org.exoplatform.outlook.portlet.templates.convertToStatus convertToStatus;
    /**
     * The converted status.
     */
    @Inject
    @Path("convertedStatus.gtmpl")
    org.exoplatform.outlook.portlet.templates.convertedStatus convertedStatus;
    /**
     * The convert to wiki.
     */
    @Inject
    @Path("convertToWiki.gtmpl")
    org.exoplatform.outlook.portlet.templates.convertToWiki convertToWiki;
    /**
     * The converted wiki.
     */
    @Inject
    @Path("convertedWiki.gtmpl")
    org.exoplatform.outlook.portlet.templates.convertedWiki convertedWiki;
    /**
     * The convert to forum.
     */
    @Inject
    @Path("convertToForum.gtmpl")
    org.exoplatform.outlook.portlet.templates.convertToForum convertToForum;
    /**
     * The converted forum.
     */
    @Inject
    @Path("convertedForum.gtmpl")
    org.exoplatform.outlook.portlet.templates.convertedForum convertedForum;
    @Inject
    @Path("spacesDropdown.gtmpl")
    org.exoplatform.outlook.portlet.templates.spacesDropdown spaces;
    /**
     * The home.
     */
    @Inject
    @Path("home.gtmpl")
    org.exoplatform.outlook.portlet.templates.home home;
    /**
     * The error.
     */
    @Inject
    @Path("error.gtmpl")
    org.exoplatform.outlook.portlet.templates.error error;
    /**
     * The i 18 n.
     */
    @Inject
    ResourceBundle i18n;
    /**
     * The i 18 n JSON.
     */
    @Inject
    ResourceBundleSerializer i18nJSON;

    /**
     * Instantiates a new outlook.
     */
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

        // TODO features for 1.1+ version
        // addRootMenuItem(new MenuItem("search"));
        addRootMenuItem(new MenuItem("userInfo"));
    }

    /**
     * Builds the cookie value.
     *
     * @param name   the name
     * @param value  the value
     * @param domain the domain
     * @param path   the path
     * @param maxAge the max age
     * @return the string
     */
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
     * @param date the date to format
     * @return an HTTP 1.0/1.1 Cookie compatible date string (GMT-based).
     */
    private static String toCookieDate(Date date) {
        TimeZone tz = TimeZone.getTimeZone(GMT_TIME_ZONE_ID);
        DateFormat fmt = new SimpleDateFormat(COOKIE_DATE_FORMAT_STRING, Locale.US);
        fmt.setTimeZone(tz);
        return fmt.format(date);
    }

    /**
     * Adds the root menu item.
     *
     * @param item the item
     */
    private void addRootMenuItem(MenuItem item) {
        rootMenuItems.add(item);
        addMenuItem(item);
    }

    /**
     * Adds the menu item.
     *
     * @param item the item
     */
    private void addMenuItem(MenuItem item) {
        allMenuItems.put(item.getName(), item);
        if (item.hasSubmenu()) {
            for (MenuItem sm : item.getSubmenu()) {
                addMenuItem(sm);
            }
        }
    }

    /**
     * Index.
     *
     * @param command         the command
     * @param resourceContext the resource context
     * @return the response
     */
    @View
    public Response index(String command, RequestContext resourceContext) {
        Collection<MenuItem> menu = new ArrayList<MenuItem>();
        if (command == null) {
            // XXX we cannot access request parameters via Juzu's request when running in Portlet bridge
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

    /**
     * Error.
     *
     * @param message the message
     * @return the response
     */
    @View
    public Response error(String message) {
        return error.with().message(message).ok();
    }

    /**
     * Logout.
     *
     * @return the response
     */
    @Ajax
    @Resource
    public Response logout() {
        try {
            // remove add-in and portal cookies and session state
            fullLogout();
            return Response.ok()
                    .content("{\"logout\":\"success\", \"loginLink\":\"/outlook/login\"}")
                    .withMimeType("application/json");
        } catch (Throwable e) {
            LOG.error("Error doing logout", e);
            return errorMessage(e.getMessage(), 500);
        }
    }

    // ********** Logout *************

    /**
     * Home form.
     *
     * @return the response
     */
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

    // ********** Home page **********

    /**
     * Save attachment form.
     *
     * @return the response
     */
    @Ajax
    @Resource
    public Response saveAttachmentForm() {
        try {
            return saveAttachment.with().spaces(outlook.getUserSpaces()).ok();
        } catch (AccessException e) {
            return errorMessage(e.getMessage(), 403);
        } catch (TemplateExecutionException e) {
            if (e.getCause() instanceof AccessException) {
                return errorMessage(e.getMessage(), 403);
            } else {
                LOG.error("Error rendering save attachments form", e);
                return errorMessage(e.getMessage(), 500);
            }
        } catch (Throwable e) {
            LOG.error("Error showing save attachments form", e);
            return errorMessage(e.getMessage(), 500);
        }
    }

    // *********** Save Attachment command **********

    /**
     * Folders.
     *
     * @param groupId the group id
     * @param path    the path
     * @return the response
     */
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

    /**
     * Adds the folder dialog.
     *
     * @return the response
     */
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

    /**
     * Adds the folder.
     *
     * @param groupId the group id
     * @param path    the path
     * @param name    the name
     * @return the response
     */
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

    /**
     * Save attachment.
     *
     * @param groupId         the group id
     * @param path            the path
     * @param comment         the comment
     * @param ewsUrl          the ews url
     * @param userEmail       the user email
     * @param userName        the user name
     * @param messageId       the message id
     * @param attachmentToken the attachment token
     * @param attachmentIds   the attachment ids
     * @param context         the context
     * @return the response
     */
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

    /**
     * Adds the attachment form.
     *
     * @return the response
     */
    @Ajax
    @Resource
    public Response addAttachmentForm() {
        try {
            List<AttachmentSource> sources = new ArrayList<AttachmentSource>();
            sources.add(new AttachmentSource(SOURCE_ID_ALL_SPACES, i18n.getString("Outlook.allSpaces")));
            Folder userFolder = outlook.getUserDocuments().getRootFolder();
            sources.add(new AttachmentSource(SOURCE_ID_PERSONAL,
                    i18n.getString("Outlook.personalDocuments"),
                    userFolder.getFullPath(),
                    userFolder.getPathLabel()));
            for (OutlookSpace space : outlook.getUserSpaces()) {
                Folder spaceFolder = space.getRootFolder();
                sources.add(new AttachmentSource(space.getGroupId(),
                        space.getTitle(),
                        spaceFolder.getFullPath(),
                        spaceFolder.getPathLabel()));
            }
            return addAttachment.with().sources(sources).ok();
        } catch (AccessException e) {
            return errorMessage(e.getMessage(), 403);
        } catch (TemplateExecutionException e) {
            if (e.getCause() instanceof AccessException) {
                return errorMessage(e.getMessage(), 403);
            } else {
                LOG.error("Error rendering add attachments form", e);
                return errorMessage(e.getMessage(), 500);
            }
        } catch (Throwable e) {
            LOG.error("Error showing add attachments form", e);
            return errorMessage(e.getMessage(), 500);
        }
    }

    // *************** Attach Document command ***********

    /**
     * Explore files.
     *
     * @param sourceId the source id
     * @param path     the path
     * @return the response
     */
    @Ajax
    @Resource
    public Response exploreFiles(String sourceId, String path) {
        try {
            Folder folder;
            if (SOURCE_ID_PERSONAL.equals(sourceId)) {
                // gather last used from user's documents
                folder = outlook.getUserDocuments().getFolder(path);
            } else if (SOURCE_ID_ALL_SPACES.equals(sourceId)) {
                return errorMessage("Source not explorable", 400);
            } else {
                // Find space by groupId (we assume it is)
                OutlookSpace space = outlook.getSpace(sourceId);
                folder = space.getFolder(path);
            }
            return filesExplorer.with().folder(folder).ok();
        } catch (BadParameterException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error reading filesExplorer " + path + ". " + e.getMessage());
            }
            return errorMessage(e.getMessage(), 400);
        } catch (Throwable e) {
            LOG.error("Error reading filesExplorer " + path, e);
            return errorMessage(e.getMessage(), 500);
        }
    }

    /**
     * Search files.
     *
     * @param sourceId the source id
     * @param text     the text
     * @return the response
     */
    @Ajax
    @Resource
    public Response searchFiles(String sourceId, String text) {
        if (sourceId != null) {
            try {
                // OutlookUser user = outlook.getUser(userEmail, userName, ewsUrl);
                Collection<File> res;
                if (SOURCE_ID_ALL_SPACES.equals(sourceId)) {
                    // search in last used filesExplorer ordered by access/modification date first
                    res = outlook.getUserDocuments().findAllLastDocuments(text);
                } else if (SOURCE_ID_PERSONAL.equals(sourceId)) {
                    // search in last used from user's documents
                    res = outlook.getUserDocuments().findLastDocuments(text);
                } else {
                    // Find space by groupId (we assume it is)
                    OutlookSpace space = outlook.getSpace(sourceId);
                    // search in space documents
                    res = space.findLastDocuments(text);
                }
                return filesSearch.with().files(res).ok();
            } catch (Throwable e) {
                LOG.error("Error searching filesExplorer in " + sourceId, e);
                return errorMessage(e.getMessage(), 500);
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Null or zero-length source ID to searhc filesExplorer");
            }
            return errorMessage("Source ID required", 400);
        }
    }

    /**
     * File link.
     *
     * @param nodePath the node path
     * @param context  the context
     * @return the response
     */
    @Ajax
    @Resource
    public Response fileLink(String nodePath, RequestContext context) {
        if (nodePath != null) {
            try {
                StringBuilder prefix = new StringBuilder();
                String scheme = context.getHttpContext().getScheme();
                if (scheme != null) {
                    prefix.append(scheme);
                } else {
                    prefix.append(scheme = "http");
                }
                prefix.append("://");
                prefix.append(context.getHttpContext().getServerName());
                int port = context.getHttpContext().getServerPort();
                if (port >= 0 && port != 80 && port != 443) {
                    prefix.append(':');
                    prefix.append(port);
                }

                String userId = context.getSecurityContext().getRemoteUser();

                LinkResource res = contentLink.createUrl(userId, nodePath, prefix.toString());

                return Response.ok()
                        .content("{\"link\":\"" + res.getLink() + "\", \"name\":\"" + res.getName() + "\"}")
                        .withMimeType("application/json");
            } catch (Throwable e) {
                LOG.error("Error creating link for node " + nodePath, e);
                return errorMessage(e.getMessage(), 500);
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Null or zero-length nodePath");
            }
            return errorMessage("Node path required", 400);
        }
    }

    /**
     * Post status form.
     *
     * @return the response
     */
    @Ajax
    @Resource
    public Response postStatusForm() {
        try {
            return postStatus.with().spaces(outlook.getUserSpaces()).ok();
        } catch (Throwable e) {
            LOG.error("Error showing status post form", e);
            return errorMessage(e.getMessage(), 500);
        }
    }

    // *************** Post UserStatus command ***********

    /**
     * Post status.
     *
     * @param groupId   the group id
     * @param message   the message
     * @param userName  the user name
     * @param userEmail the user email
     * @param context   the context
     * @return the response
     */
    @Ajax
    @Resource
    public Response postStatus(String groupId, String message, String userName, String userEmail, RequestContext context) {
        try {

            OutlookUser user = outlook.getUser(userEmail, userName, null);

            if (groupId != null && groupId.length() > 0) {// new String(body.getBytes(""), "utf-8")
                // space activity requested
                OutlookSpace space = outlook.getSpace(groupId);
                if (space != null) {
                    ExoSocialActivity activity = space.postActivity(user, message);
                    return convertedStatus.with().status(new UserStatus(null, space.getTitle(), activity.getPermaLink())).ok();
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
                return convertedStatus.with().status(new UserStatus(user.getLocalUser(), null, activity.getPermaLink())).ok();
            }
        } catch (Throwable e) {
            LOG.error("Error converting message to activity status for " + userEmail, e);
            return errorMessage(e.getMessage(), 500);
        }
    }

    /**
     * Start discussion form.
     *
     * @return the response
     */
    @Ajax
    @Resource
    public Response startDiscussionForm() {
        try {
            return startDiscussion.with().spaces(outlook.getUserSpaces()).ok();
        } catch (Throwable e) {
            LOG.error("Error showing discussion start form", e);
            return errorMessage(e.getMessage(), 500);
        }
    }

    // *************** Start Discussion command ***********

    /**
     * Start discussion.
     *
     * @param groupId   the group id
     * @param name      the name
     * @param text      the text
     * @param userName  the user name
     * @param userEmail the user email
     * @param context   the context
     * @return the response
     */
    @Ajax
    @Resource
    public Response startDiscussion(String groupId,
                                    String name,
                                    String text,
                                    String userName,
                                    String userEmail,
                                    RequestContext context) {
        try {
            OutlookUser user = outlook.getUser(userEmail, userName, null);
            if (groupId != null && groupId.length() > 0) {
                // space forum requested
                OutlookSpace space = outlook.getSpace(groupId);
                if (space != null) {
                    Topic topic = space.addForumTopic(user, name, text);
                    return startedDiscussion.with()
                            .topic(new UserForumTopic(topic.getId(),
                                    topic.getTopicName(),
                                    topic.getLink(),
                                    space.getTitle()))
                            .ok();
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Error starting discussion: space not found " + groupId + ". OutlookUser " + userEmail);
                    }
                    return errorMessage("Error starting discussion: space not found " + groupId, 404);
                }
            } else {
                return errorMessage("Error starting discussion: space not selected", 400);
            }
        } catch (Throwable e) {
            LOG.error("Error starting discussion for " + userEmail, e);
            return errorMessage(e.getMessage(), 500);
        }
    }

    /**
     * Search form.
     *
     * @param httpContext the http context
     * @return the response
     */
    @Ajax
    @Resource
    public Response searchForm(HttpContext httpContext) {
        try {
            StringBuilder link = new StringBuilder();
            link.append("outlook/quicksearch");
            return unifiedSearch.with().searchLink(link.toString()).ok();
        } catch (Throwable e) {
            LOG.error("Error showing unified search form", e);
            return errorMessage(e.getMessage(), 500);
        }
    }

    // *************** Search command ***********

    /**
     * Search.
     *
     * @param sourceId the source id
     * @param text     the text
     * @return the response
     */
    @Ajax
    @Resource
    @Deprecated
    public Response search(String sourceId, String text) {
        if (sourceId != null) {
            try {
                Collection<File> res;
                if (SOURCE_ID_ALL_SPACES.equals(sourceId)) {
                    // TODO search in last used filesExplorer ordered by access/modification date first
                    res = outlook.getUserDocuments().findAllLastDocuments(text);
                } else if (SOURCE_ID_PERSONAL.equals(sourceId)) {
                    // TODO search in last used from user's documents
                    res = outlook.getUserDocuments().findLastDocuments(text);
                } else {
                    // Find space by groupId (we assume it is)
                    OutlookSpace space = outlook.getSpace(sourceId);
                    // search in space documents
                    res = space.findLastDocuments(text);
                }
                return filesSearch.with().files(res).ok();
            } catch (Throwable e) {
                LOG.error("Error searching filesExplorer in " + sourceId, e);
                return errorMessage(e.getMessage(), 500);
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Null or zero-length source ID to searhc filesExplorer");
            }
            return errorMessage("Source ID required", 400);
        }
    }

    /**
     * User info form.
     *
     * @return the response
     */
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

    // *************** OutlookUser info command ***********

    /**
     * Convert to status form.
     *
     * @return the response
     */
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

    // *************** Convert To UserStatus command ***********

    /**
     * User info response.
     *
     * @param byEmail Array of emails to search in eXoplatform
     * @return the response
     */
    @Ajax
    @Resource
    public Response userInfo(String byEmail,RequestContext context) {
        String nameOwner = context.getSecurityContext().getRemoteUser();
        Map<String, List<String>> userInfo = new HashMap<String, List<String>>();
        Map<String, Map<String, String>> usersInfoMap = new HashMap<>();
        Map<String, ExoSocialActivity> exoSocialActivityMap = new HashMap<>();
        List<User> usersToDisplay = new LinkedList<>();
        List<Profile> profilesToDisplay = new LinkedList<>();
        Map<String, Profile> profileToRelationship = new HashMap<>();
        List<String> idActivity = null;
        List<String> profileRelationshipName = null;
        try {
            if (byEmail != null) {
                String[] allEmails = byEmail.split(",");
                for (String email : allEmails) {
                    ListAccess<User> allExoUser = outlook.getUserByEmail(email);
                    for (User user : allExoUser.load(0, allExoUser.getSize())) {
                        idActivity = new LinkedList<>();
                        profileRelationshipName = new LinkedList<>();
                        if (user.getEmail().equals(email.toLowerCase())) {
                            String foundUserName = user.getUserName();
                            OutlookUser exoUser = outlook.getUser(user.getEmail(), foundUserName, null);
                            Profile exoOwnerProfile = exoUser.getProfileForName(nameOwner);
                            if (!user.getEmail().equals(exoOwnerProfile.getProperty("email"))) {
                                Profile exoUserProfile = exoUser.getProfileForName(foundUserName);
                                List<Relationship> userGetRelationships = exoUser.getRelationships(foundUserName);
                                profilesToDisplay.add(exoUserProfile);
                                usersToDisplay.add(user);
                                usersInfoMap.put(foundUserName, outlook.getUserInfoMap(foundUserName));
                                for (Relationship relationship : userGetRelationships.subList(0, Math.min(userGetRelationships.size(), 20))) {
                                    if (relationship.getReceiver().getProfile().getProperty("username").toString().equals(foundUserName)) {
                                        profileToRelationship.put(relationship.getSender().getProfile().getProperty("username").toString(), relationship.getSender().getProfile());
                                        profileRelationshipName.add(relationship.getSender().getProfile().getProperty("username").toString());
                                    } else if (relationship.getSender().getProfile().getProperty("username").toString().equals(foundUserName)) {
                                        profileToRelationship.put(relationship.getReceiver().getProfile().getProperty("username").toString(), relationship.getReceiver().getProfile());
                                        profileRelationshipName.add(relationship.getReceiver().getProfile().getProperty("username").toString());
                                    }
                                }
                                userInfo.put(foundUserName + "relationship", profileRelationshipName);
                                RealtimeListAccess<ExoSocialActivity> activity = exoUser.getActivity(foundUserName);
                                if (activity.getSize() > 0) {
                                    List<ExoSocialActivity> exoSocialActivityList = activity.loadAsList(0, 20);
                                    if (exoSocialActivityList != null) {
                                        for (ExoSocialActivity exoSocialActivity : exoSocialActivityList) {
                                            idActivity.add(exoSocialActivity.getId());
                                            exoSocialActivityMap.put(exoSocialActivity.getId(), exoSocialActivity);
                                        }
                                    }
                                }
                                userInfo.put(foundUserName + "idActivity", idActivity);
                            }
                        }
                    }
                }
            }
            return userInfoactivity.with()
                    .exoSocialActivityMap(exoSocialActivityMap)
                    .usersToDisplay(usersToDisplay)
                    .usersInfoMap(usersInfoMap)
                    .userInfo(userInfo)
                    .profilesToDisplay(profilesToDisplay)
                    .profileToRelationship(profileToRelationship)
                    .nameOwner(nameOwner)
                    .ok();
        } catch (Exception e) {
            LOG.error("Error showing User Info by email " +byEmail , e);
            return errorMessage(e.getMessage(), 500);
        }
    }


        /**
     * Convert to status.
     *
     * @param groupId   the group id
     * @param messageId the message id
     * @param title     the user message to the activity
     * @param subject   the subject
     * @param body      the body
     * @param created   the created
     * @param modified  the modified
     * @param userName  the user name
     * @param userEmail the user email
     * @param fromName  the from name
     * @param fromEmail the from email
     * @param context   the context
     * @return the response
     */
    @Ajax
    @Resource
    public Response convertToStatus(String groupId,
                                    String messageId,
                                    String title,
                                    String subject,
                                    String body,
                                    String created,
                                    String modified,
                                    String userName,
                                    String userEmail,
                                    String fromName,
                                    String fromEmail,
                                    RequestContext context) {
        try {
            OutlookUser user = outlook.getUser(userEmail, userName, null);
            OutlookMessage message = message(user, messageId, fromEmail, fromName, created, modified, title, subject, body);

            if (groupId != null && groupId.length() > 0) {
                // space activity requested
                OutlookSpace space = outlook.getSpace(groupId);
                if (space != null) {
                    ExoSocialActivity activity = space.postActivity(message);
                    return convertedStatus.with().status(new UserStatus(null, space.getTitle(), activity.getPermaLink())).ok();
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
                return convertedStatus.with().status(new UserStatus(user.getLocalUser(), null, activity.getPermaLink())).ok();
            }
        } catch (Throwable e) {
            LOG.error("Error converting message to activity status for " + userEmail, e);
            return errorMessage(e.getMessage(), 500);
        }
    }

    /**
     * Gets the message.
     *
     * @param ewsUrl       the ews url
     * @param userEmail    the user email
     * @param userName     the user name
     * @param messageId    the message id
     * @param messageToken the message token
     * @return the message
     */
    @Ajax
    @Resource
    public Response getMessage(String ewsUrl, String userEmail, String userName, String messageId, String messageToken) {

        if (ewsUrl != null && userEmail != null && messageId != null && messageToken != null) {
            try {
                OutlookUser user = outlook.getUser(userEmail, userName, ewsUrl);
                OutlookMessage message = outlook.getMessage(user, messageId, messageToken);
                return Response.ok(message.getBody())
                        .withHeader("X-MessageBodyContentType", message.getType())
                        .withCharset(Charset.forName("UTF-8"));
            } catch (BadParameterException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Error reading message " + messageId + ". " + e.getMessage());
                }
                return errorMessage(e.getMessage(), 400);
            } catch (Throwable e) {
                LOG.error("Error reading message " + messageId, e);
                return errorMessage(e.getMessage(), 500);
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Null or zero-length message ID or user parameters to read message");
            }
            return errorMessage("Message ID and user parameters required", 400);
        }
    }

    /**
     * Convert to wiki form.
     *
     * @return the response
     */
    @Ajax
    @Resource
    public Response convertToWikiForm() {
        try {
            return convertToWiki.with().spaces(outlook.getUserSpaces()).ok();
        } catch (Throwable e) {
            LOG.error("Error showing conversion to wiki form", e);
            return errorMessage(e.getMessage(), 500);
        }
    }

    // *************** Convert To Wiki command ***********

    /**
     * Convert to wiki.
     *
     * @param groupId   the group id
     * @param messageId the message id
     * @param subject   the subject
     * @param body      the body
     * @param created   the created
     * @param modified  the modified
     * @param userName  the user name
     * @param userEmail the user email
     * @param fromName  the from name
     * @param fromEmail the from email
     * @param context   the context
     * @return the response
     */
    @Ajax
    @Resource
    public Response convertToWiki(String groupId,
                                  String messageId,
                                  String subject,
                                  String body,
                                  String created,
                                  String modified,
                                  String userName,
                                  String userEmail,
                                  String fromName,
                                  String fromEmail,
                                  RequestContext context) {
        try {
            OutlookUser user = outlook.getUser(userEmail, userName, null);
            OutlookMessage message = message(user, messageId, fromEmail, fromName, created, modified, null, subject, body);

            if (groupId != null && groupId.length() > 0) {
                // space wiki requested
                OutlookSpace space = outlook.getSpace(groupId);
                if (space != null) {
                    Page page = space.addWikiPage(message);
                    return convertedWiki.with()
                            .page(new UserWikiPage(page.getId(), page.getTitle(), page.getUrl(), space.getTitle()))
                            .ok();
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Error converting message to wiki page: space not found " + groupId + ". OutlookUser " + userEmail);
                    }
                    return errorMessage("Error converting message to wiki page: space not found " + groupId, 404);
                }
            } else {
                // user portal wiki requested
                Page page = user.addWikiPage(message);
                return convertedWiki.with().page(new UserWikiPage(page.getId(), page.getTitle(), page.getUrl())).ok();
            }
        } catch (Throwable e) {
            LOG.error("Error converting message to wiki page for " + userEmail, e);
            return errorMessage(e.getMessage(), 500);
        }
    }

    /**
     * Convert to forum form.
     *
     * @return the response
     */
    @Ajax
    @Resource
    public Response convertToForumForm() {
        try {
            return convertToForum.with().spaces(outlook.getUserSpaces()).ok();
        } catch (Throwable e) {
            LOG.error("Error showing conversion to forum form", e);
            return errorMessage(e.getMessage(), 500);
        }
    }

    // *************** Convert To Forum command ***********

    /**
     * Convert to forum.
     *
     * @param groupId   the group id
     * @param messageId the message id
     * @param subject   the subject
     * @param body      the body
     * @param created   the created
     * @param modified  the modified
     * @param userName  the user name
     * @param userEmail the user email
     * @param fromName  the from name
     * @param fromEmail the from email
     * @param context   the context
     * @return the response
     */
    @Ajax
    @Resource
    public Response convertToForum(String groupId,
                                   String messageId,
                                   String subject,
                                   String body,
                                   String created,
                                   String modified,
                                   String userName,
                                   String userEmail,
                                   String fromName,
                                   String fromEmail,
                                   RequestContext context) {
        try {
            OutlookUser user = outlook.getUser(userEmail, userName, null);
            OutlookMessage message = message(user, messageId, fromEmail, fromName, created, modified, null, subject, body);

            if (groupId != null && groupId.length() > 0) {
                // space forum requested
                OutlookSpace space = outlook.getSpace(groupId);
                if (space != null) {
                    Topic topic = space.addForumTopic(message);
                    return convertedForum.with()
                            .topic(new UserForumTopic(topic.getId(),
                                    topic.getTopicName(),
                                    topic.getLink(),
                                    space.getTitle()))
                            .ok();
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Error converting message to forum post: space not found " + groupId + ". OutlookUser " + userEmail);
                    }
                    return errorMessage("Error converting message to forum post: space not found " + groupId, 404);
                }
            } else {
                return errorMessage("Error creating forum topic: space not selected", 400);
            }
        } catch (Throwable e) {
            LOG.error("Error converting message to forum post for " + userEmail, e);
            return errorMessage(e.getMessage(), 500);
        }
    }

    /**
     * Rememberme.
     *
     * @param context the context
     * @return the response
     * @throws IOException Signals that an I/O exception has occurred.
     */
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

    // *************** Post-Login *************

    /**
     * User spaces.
     *
     * @return the response
     */
    @Ajax
    @Resource
    public Response userSpaces() {
        try {
            return spaces.with().set("spaces", outlook.getUserSpaces()).ok();
        } catch (Throwable e) {
            LOG.error("Error getting user spaces", e);
            return errorMessage(e.getMessage(), 500);
        }
    }

    /**
     * Error message.
     *
     * @param text   the text
     * @param status the status
     * @return the response
     */
    Response errorMessage(String text, int status) {
        return Response.content(status, text != null ? text : "");
        // return errorMessage.with().message(text != null ? text : "").status(status);
    }

    // ***************** internals *****************

    /**
     * User menu item.
     *
     * @param item the item
     * @return the menu item
     */
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

    /**
     * Full logout.
     */
    void fullLogout() {
        // XXX repeating logic of UIPortal.LogoutActionListener
        PortalRequestContext prContext = Util.getPortalRequestContext();
        HttpServletRequest req = prContext.getRequest();
        HttpServletResponse res = prContext.getResponse();

        // Delete the token from JCR
        String token = RequestUtils.getCookie(req, LoginServlet.COOKIE_NAME); // getTokenCookie(req)
        if (token != null) {
            AbstractTokenService<GateInToken, String> tokenService = AbstractTokenService.getInstance(CookieTokenService.class);
            tokenService.deleteToken(token);
        }
        token = LoginServlet.getOauthRememberMeTokenCookie(req);
        if (token != null) {
            AbstractTokenService<GateInToken, String> tokenService = AbstractTokenService.getInstance(CookieTokenService.class);
            tokenService.deleteToken(token);
        }

        LogoutControl.wantLogout();
        Cookie cookie = new Cookie(LoginServlet.COOKIE_NAME, "");
        cookie.setPath(req.getContextPath());
        cookie.setMaxAge(0);
        res.addCookie(cookie);

        Cookie oauthCookie = new Cookie(LoginServlet.OAUTH_COOKIE_NAME, "");
        oauthCookie.setPath(req.getContextPath());
        oauthCookie.setMaxAge(0);
        res.addCookie(oauthCookie);

        // **********
        // Outlook add-in logout (cookies)
        String rememberMeOutlook = RequestUtils.getCookie(req, OutlookTokenService.COOKIE_NAME);
        if (rememberMeOutlook != null) {
            OutlookTokenService outlookTokens = AbstractTokenService.getInstance(OutlookTokenService.class);
            outlookTokens.deleteToken(rememberMeOutlook);
        }
        Cookie rememberMeOutlookCookie = new Cookie(OutlookTokenService.COOKIE_NAME, "");
        rememberMeOutlookCookie.setPath(req.getRequestURI());
        rememberMeOutlookCookie.setMaxAge(0);
        res.addCookie(rememberMeOutlookCookie);
    }

    /**
     * Message.
     *
     * @param user      the user
     * @param messageId the message id
     * @param fromEmail the from email
     * @param fromName  the from name
     * @param created   the created
     * @param modified  the modified
     * @param title     the title
     * @param subject   the subject
     * @param body      the body
     * @return the outlook message
     * @throws OutlookException the outlook exception
     * @throws ParseException   the parse exception
     */
    private OutlookMessage message(OutlookUser user,
                                   String messageId,
                                   String fromEmail,
                                   String fromName,
                                   String created,
                                   String modified,
                                   String title,
                                   String subject,
                                   String body) throws OutlookException, ParseException {
        OutlookEmail from = outlook.getAddress(fromEmail, fromName);
        Calendar createdDate = Calendar.getInstance();
        createdDate.setTime(OutlookMessage.DATE_FORMAT.parse(created));
        Calendar modifiedDate = Calendar.getInstance();
        modifiedDate.setTime(OutlookMessage.DATE_FORMAT.parse(modified));
        OutlookMessage message = outlook.buildMessage(messageId,
                user,
                from,
                null,
                createdDate,
                modifiedDate,
                title,
                subject,
                body);
        return message;
    }

    /**
     * Request command.
     *
     * @return the string
     */
    private String requestCommand() {
        PortalRequestContext portalRequest = Util.getPortalRequestContext();
        if (portalRequest != null) {
            // try portal HTTP request for provider id
            return portalRequest.getRequestParameter("command");
        }
        return null;
    }

    /**
     * The Class UserStatus.
     */
    public class UserStatus extends ActivityStatus {

        /**
         * Instantiates a new user status.
         *
         * @param userTitle the user title
         * @param spaceName the space name
         * @param link      the link
         */
        protected UserStatus(String userTitle, String spaceName, String link) {
            super(userTitle, spaceName, link);
        }

        /**
         * Gets the converted to space activity.
         *
         * @return the converted to space activity
         */
        public String getConvertedToSpaceActivity() {
            String msg = i18n.getString("Outlook.convertedToSpaceActivity");
            return msg.replace("{SPACE_NAME}", spaceName);
        }
    }

    /**
     * The Class UserWikiPage.
     */
    public class UserWikiPage extends WikiPage {

        /**
         * The space name.
         */
        final String spaceName;

        /**
         * Instantiates a new user wiki page.
         *
         * @param id        the id
         * @param title     the title
         * @param link      the link
         * @param spaceName the space name
         */
        protected UserWikiPage(String id, String title, String link, String spaceName) {
            super(id, title, link);
            this.spaceName = spaceName;
        }

        /**
         * Instantiates a new user wiki page.
         *
         * @param id    the id
         * @param title the title
         * @param link  the link
         */
        protected UserWikiPage(String id, String title, String link) {
            this(id, title, link, null);
        }

        /**
         * Gets the converted to space wiki.
         *
         * @return the converted to space wiki
         */
        public String getConvertedToSpaceWiki() {
            String msg = i18n.getString("Outlook.convertedToSpaceWiki");
            return msg.replace("{SPACE_NAME}", spaceName);
        }

        /**
         * Checks if is in space.
         *
         * @return the isSpace
         */
        public boolean isInSpace() {
            return spaceName != null;
        }
    }

    /**
     * The Class UserForumTopic.
     */
    public class UserForumTopic extends ForumTopic {

        /**
         * The space name.
         */
        final String spaceName;

        /**
         * Instantiates a new user forum topic.
         *
         * @param id        the id
         * @param title     the title
         * @param link      the link
         * @param spaceName the space name
         */
        protected UserForumTopic(String id, String title, String link, String spaceName) {
            super(id, title, link);
            this.spaceName = spaceName;
        }

        /**
         * Instantiates a new user forum topic.
         *
         * @param id    the id
         * @param title the title
         * @param link  the link
         */
        protected UserForumTopic(String id, String title, String link) {
            this(id, title, link, null);
        }

        /**
         * Gets the converted to space forum.
         *
         * @return the converted to space forum
         */
        public String getConvertedToSpaceForum() {
            String msg = i18n.getString("Outlook.convertedToSpaceForum");
            return msg.replace("{SPACE_NAME}", spaceName);
        }

        /**
         * Gets the started topic in space forum.
         *
         * @return the started topic in space forum
         */
        public String getStartedTopicInSpaceForum() {
            String msg = i18n.getString("Outlook.startedTopicInSpaceForum");
            return msg.replace("{SPACE_NAME}", spaceName);
        }

        /**
         * Checks if is in space.
         *
         * @return the isSpace
         */
        public boolean isInSpace() {
            return spaceName != null;
        }
    }
}
