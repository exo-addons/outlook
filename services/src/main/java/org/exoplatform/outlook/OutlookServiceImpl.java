/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.outlook;

import com.ibm.icu.text.Transliterator;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.exoplatform.commons.utils.ActivityTypeUtils;
import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.outlook.jcr.File;
import org.exoplatform.outlook.jcr.Folder;
import org.exoplatform.outlook.jcr.HierarchyNode;
import org.exoplatform.outlook.jcr.NodeFinder;
import org.exoplatform.outlook.mail.MailAPI;
import org.exoplatform.outlook.mail.MailServerException;
import org.exoplatform.outlook.social.OutlookAttachmentActivity;
import org.exoplatform.outlook.social.OutlookMessageActivity;
import org.exoplatform.portal.Constants;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserProfileHandler;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.webui.Utils;
import org.exoplatform.web.security.security.CookieTokenService;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.ws.frameworks.json.value.JsonValue;
import org.picocontainer.Startable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessControlException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.servlet.http.HttpServletRequest;

/**
 * Service implementing {@link OutlookService} and {@link Startable}.<br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: AnaplanServiceImpl.java 00000 Mar 3, 2016 pnedonosko $
 */
public class OutlookServiceImpl implements OutlookService, Startable {

  public static final String            MAILSERVER_URL         = "mailserver-url";

  protected static final String         EXO_DATETIME           = "exo:datetime";

  protected static final String         EXO_MODIFY             = "exo:modify";

  protected static final String         EXO_RSSENABLE          = "exo:rss-enable";

  protected static final String         EXO_OWNEABLE           = "exo:owneable";

  protected static final String         EXO_PRIVILEGEABLE      = "exo:privilegeable";

  protected static final String         OUTLOOK_MESSAGES_TITLE = "Outlook Messages";

  protected static final String         OUTLOOK_MESSAGES_NAME  = "outlook-messages";

  protected static final String         UPLAODS_FOLDER_TITLE   = "Uploads";

  protected static final String[]       READER_PERMISSION      = new String[] { PermissionType.READ };

  protected static final String[]       MANAGER_PERMISSION     = new String[] { PermissionType.READ, PermissionType.REMOVE };

  protected static final Log            LOG                    = ExoLogger.getLogger(OutlookServiceImpl.class);

  protected static final Random         RANDOM                 = new Random();

  protected static final Transliterator accentsConverter       = Transliterator.getInstance("Latin; NFD; [:Nonspacing Mark:] Remove; NFC;");

  protected class UserFolder extends Folder {

    protected UserFolder(Folder parent, Node node) throws RepositoryException, OutlookException {
      super(parent, node);
    }

    protected UserFolder(String rootPath, Node node) throws RepositoryException, OutlookException {
      super(rootPath, node);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Folder addSubfolder(String name) throws RepositoryException, OutlookException {
      final Node parent = getNode();
      Node subfolderNode = addFolder(parent, name, true);
      Folder subfolder = newFolder(this, subfolderNode);
      parent.save();
      Set<Folder> subfolders = this.subfolders.get();
      if (subfolders != null) {
        subfolders.add(subfolder);
      }
      return subfolder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Locale userLocale() {
      return currentUserLocale();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Folder newFolder(Folder parent, Node node) throws RepositoryException, OutlookException {
      Folder folder = new UserFolder(parent, node);
      // initDocumentLink(space, folder);
      return folder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Folder newFolder(String rootPath, Node node) throws RepositoryException, OutlookException {
      Folder folder = new UserFolder(rootPath, node);
      // initDocumentLink(space, folder);
      return folder;
    }
  }

  protected class UserFile extends File {

    protected UserFile(Folder parent, Node node) throws RepositoryException, OutlookException {
      super(parent, node);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Locale userLocale() {
      return currentUserLocale();
    }
  }

  protected class UserImpl extends OutlookUser {

    protected final IdentityManager socialIdentityManager;

    protected final ActivityManager socialActivityManager;

    protected UserImpl(String email, String displayName, String userName) {
      super(email, displayName, userName);
      this.socialIdentityManager = socialIdentityManager();
      this.socialActivityManager = socialActivityManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExoSocialActivity postActivity(OutlookMessage message) throws Exception {
      // save text to user documents
      Node userDocs = userDocumentsNode(localUser);
      Node messagesFolder = messagesFolder(userDocs, localUser, "member:/platform/users");
      Node messageFile = addMessageFile(messagesFolder, message);
      setPermissions(messageFile, localUser, "member:/platform/users");
      messagesFolder.save();

      // post activity to user status stream
      // Identity userIdentity =
      // socialIdentityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
      // this.userName,
      // true);
      // ExoSocialActivity activity = new ExoSocialActivityImpl(userIdentity.getId(),
      // //UIDocActivityBuilder.ACTIVITY_TYPE,
      // "files:spaces",
      // title);
      // Map<String, String> templateParams = new HashMap<String, String>();
      // templateParams.put(UIDocActivity.WORKSPACE, messagesFolder.getSession().getWorkspace().getName());
      // templateParams.put(UIDocActivity.REPOSITORY,
      // jcrService.getCurrentRepository().getConfiguration().getName());
      // templateParams.put(UIDocActivity.MESSAGE, title);
      // templateParams.put("MESSAGE", title);
      // //
      // templateParams.put(UIDocActivity.DOCLINK,"/portal/rest/jcr/repository/collaboration/Users/t___/th___/tho___/thomas/Private/Documents/samir.pdf,
      // // DOCNAME=samir.pdf, DOCPATH=/Users/t___/th___/tho___/thomas/Private/Documents/samir.pdf");
      // templateParams.put(UIDocActivity.DOCNAME, title);
      // templateParams.put(UIDocActivity.DOCUMENT_TITLE, title);
      // templateParams.put(UIDocActivity.IS_SYMLINK, "false");
      // templateParams.put(UIDocActivity.MIME_TYPE, "text/html");
      // templateParams.put(UIDocActivity.DOCPATH, messageFile.getPath());
      // activity.setTemplateParams(templateParams);
      // socialActivityManager.saveActivityNoReturn(activity);
      // // TODO LinkProvider.getSingleActivityUrl(activityId)
      // messageStore.saveMessage(activity.getId(), text);

      final String origType = org.exoplatform.wcm.ext.component.activity.listener.Utils.getActivityType();
      try {
        org.exoplatform.wcm.ext.component.activity.listener.Utils.setActivityType(OutlookMessageActivity.ACTIVITY_TYPE);
        ExoSocialActivity activity = org.exoplatform.wcm.ext.component.activity.listener.Utils.postFileActivity(messageFile,
                                                                                                                "SocialIntegration.messages.createdBy",
                                                                                                                true,
                                                                                                                false,
                                                                                                                "");
        // TODO care about activity removal with the message file
        activity.setPermanLink(LinkProvider.getSingleActivityUrl(activity.getId()));
        return activity;
      } finally {
        org.exoplatform.wcm.ext.component.activity.listener.Utils.setActivityType(origType);
      }
    }

  }

  protected class OutlookSpaceImpl extends OutlookSpace {

    class SpaceFolder extends UserFolder {

      protected SpaceFolder(Folder parent, Node node) throws RepositoryException, OutlookException {
        super(parent, node);
      }

      protected SpaceFolder(String rootPath, Node node) throws RepositoryException, OutlookException {
        super(rootPath, node);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      protected Set<Folder> readSubnodes() throws RepositoryException, OutlookException {
        Set<Folder> subfolders = super.readSubnodes();
        for (Folder sf : subfolders) {
          initDocumentLink(OutlookSpaceImpl.this, sf);
        }
        return subfolders;
      }
    }

    class RootFolder extends SpaceFolder {

      protected RootFolder(String rootPath, Node node) throws RepositoryException, OutlookException {
        super(rootPath, node);
        initDocumentLink(OutlookSpaceImpl.this, this);
        hasSubfolders(); // force child reading to init default folder in readSubnodes()
      }

      /**
       * {@inheritDoc}
       */
      @Override
      protected Set<Folder> readSubnodes() throws RepositoryException, OutlookException {
        Folder uploads = null;
        Set<Folder> subfolders = super.readSubnodes();
        for (Folder sf : subfolders) {
          if (sf.getTitle().equals(UPLAODS_FOLDER_TITLE)) {
            uploads = defaultSubfolder = sf;
            break;
          }
        }
        if (uploads == null) {
          final Node parent = getNode();
          Node subfolderNode = addFolder(node, UPLAODS_FOLDER_TITLE, false);
          uploads = newFolder(this, subfolderNode);
          parent.save();
          initDocumentLink(OutlookSpaceImpl.this, uploads);
          subfolders.add(uploads);
          defaultSubfolder = uploads;
        }
        return subfolders;
      }
    }

    protected final String                  rootPath;

    protected final ThreadLocal<RootFolder> rootFolder = new ThreadLocal<RootFolder>();

    protected final IdentityManager         socialIdentityManager;

    protected final ActivityManager         socialActivityManager;

    protected OutlookSpaceImpl(Space socialSpace) throws RepositoryException, OutlookException {
      super(socialSpace.getGroupId(), socialSpace.getDisplayName(), socialSpace.getShortName());
      this.rootPath = groupDocsPath(groupId);
      this.socialIdentityManager = socialIdentityManager();
      this.socialActivityManager = socialActivityManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Folder getFolder(String path) throws OutlookException, RepositoryException {
      Folder parent = getRootFolder();
      Folder folder;
      if (rootPath.equals(path)) {
        folder = parent;
      } else if (path.startsWith(rootPath)) {
        folder = new SpaceFolder(parent, node(path));
      } else {
        throw new BadParameterException("Path does not belong to space documents: " + path);
      }
      initDocumentLink(this, folder);
      return folder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Folder getRootFolder() throws OutlookException, RepositoryException {
      RootFolder root = rootFolder.get();
      if (root == null) {
        root = new RootFolder(rootPath, node(rootPath));
        rootFolder.set(root);
      }
      return root;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExoSocialActivity postActivity(OutlookMessage message) throws Exception {
      // post activity to space status stream under current user
      // Identity spaceIdentity = socialIdentityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
      // this.groupId,
      // true);
      // Identity userIdentity = socialIdentityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
      // currentUserId(),
      // true);
      // ExoSocialActivity activity = new ExoSocialActivityImpl(userIdentity.getId(),
      // UIOutlookMessageActivity.ACTIVITY_TYPE,
      // title);
      // socialActivityManager.saveActivityNoReturn(spaceIdentity, activity);
      // messageStore.saveMessage(activity.getId(), text);
      // return activity;

      Node spaceDocs = spaceDocumentsNode(groupId);
      Node messagesFolder = messagesFolder(spaceDocs, groupId);
      Node messageFile = addMessageFile(messagesFolder, message);
      setPermissions(messageFile, new StringBuilder("member:").append(groupId).toString());
      messagesFolder.save();

      final String origType = org.exoplatform.wcm.ext.component.activity.listener.Utils.getActivityType();
      try {
        org.exoplatform.wcm.ext.component.activity.listener.Utils.setActivityType(OutlookMessageActivity.ACTIVITY_TYPE);
        ExoSocialActivity activity = org.exoplatform.wcm.ext.component.activity.listener.Utils.postFileActivity(messageFile,
                                                                                                                "SocialIntegration.messages.createdBy",
                                                                                                                true,
                                                                                                                false,
                                                                                                                "");
        // TODO care about activity removal with the message file
        activity.setPermanLink(LinkProvider.getSingleActivityUrl(activity.getId()));
        return activity;
      } finally {
        org.exoplatform.wcm.ext.component.activity.listener.Utils.setActivityType(origType);
      }
    }
  }

  protected final RepositoryService                           jcrService;

  protected final SessionProviderService                      sessionProviders;

  protected final IdentityRegistry                            identityRegistry;

  protected final NodeFinder                                  finder;

  protected final NodeHierarchyCreator                        hierarchyCreator;

  protected final OrganizationService                         organization;

  protected final CookieTokenService                          tokenService;

  protected final ManageDriveService                          driveService;

  protected final ListenerService                             listenerService;

  /**
   * Authenticated users.
   */
  protected final ConcurrentHashMap<String, OutlookUser>      authenticated = new ConcurrentHashMap<String, OutlookUser>();

  protected final ConcurrentHashMap<String, OutlookSpaceImpl> spaces        = new ConcurrentHashMap<String, OutlookSpaceImpl>();

  // protected final OutlookMessageStore messageStore;

  protected MailAPI                                           mailserverApi;

  /**
   * Outlook service with storage in JCR and with managed features.
   * 
   * @param jcrService {@link RepositoryService}
   * @param sessionProviders {@link SessionProviderService}
   * @param identityRegistry
   * @param finder
   * @param organization
   * @param params
   * @throws ConfigurationException
   * @throws MailServerException
   */
  public OutlookServiceImpl(// OutlookMessageStore messageStore,
                            RepositoryService jcrService,
                            SessionProviderService sessionProviders,
                            IdentityRegistry identityRegistry,
                            NodeHierarchyCreator hierarchyCreator,
                            NodeFinder finder,
                            OrganizationService organization,
                            CookieTokenService tokenService,
                            ManageDriveService driveService,
                            ListenerService listenerService,
                            InitParams params) throws ConfigurationException, MailServerException {
    // this.messageStore = messageStore;

    this.jcrService = jcrService;
    this.sessionProviders = sessionProviders;
    this.identityRegistry = identityRegistry;
    this.hierarchyCreator = hierarchyCreator;
    this.finder = finder;
    this.organization = organization;
    this.tokenService = tokenService;
    this.driveService = driveService;
    this.listenerService = listenerService;

    // API for user requests (uses credentials from eXo user profile)
    MailAPI api = new MailAPI();
    this.mailserverApi = api;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Folder getFolder(String path) throws OutlookException, RepositoryException {
    Node node = node(path);
    Folder folder = new UserFolder(path, node);
    // TODO folder.init(path);
    return folder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Folder getFolder(Folder parent, String path) throws OutlookException, RepositoryException {
    Node node = node(path);
    return new UserFolder(parent, node);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<File> saveAttachment(OutlookSpace space,
                                   Folder destFolder,
                                   OutlookUser user,
                                   String comment,
                                   String messageId,
                                   String attachmentToken,
                                   String... attachmentIds) throws OutlookException, RepositoryException {
    List<File> files = new ArrayList<File>();
    Node parent = destFolder.getNode();
    for (String attachmentId : attachmentIds) {
      JsonValue vatt = mailserverApi.getAttachment(user, messageId, attachmentToken, attachmentId);
      JsonValue vName = vatt.getElement("Name");
      if (isNull(vName)) {
        throw new OutlookFormatException("Attachment doesn't contain Name");
      }
      String name = vName.getStringValue();
      JsonValue vContentType = vatt.getElement("ContentType");
      if (isNull(vContentType)) {
        throw new OutlookFormatException("Attachment (" + name + ") doesn't contain ContentType");
      }
      String contentType = vContentType.getStringValue();
      // TODO Do we need remote size?
      // JsonValue vSize = vatt.getElement("Size");
      // if (isNull(vSize)) {
      // throw new OutlookFormatException("Attachment (" + name + ") doesn't contain Size");
      // }
      // long size = vSize.getLongValue();
      JsonValue vContentBytes = vatt.getElement("ContentBytes");
      if (isNull(vContentBytes)) {
        throw new OutlookFormatException("Attachment (" + name + ") doesn't contain ContentBytes");
      }
      // FYI attachment content in BASE64 (may be twice!)
      String contentBytes = vContentBytes.getStringValue();

      // Save in JCR
      try (InputStream content = decode(contentBytes)) {
        Node attachmentNode = addFile(parent, name, comment, contentType, content);
        if (space != null) {
          setPermissions(attachmentNode, new StringBuilder("member:").append(space.getGroupId()).toString());
        } else {
          setPermissions(attachmentNode, user.getLocalUser(), "member:/platform/users");
        }
        files.add(new UserFile(destFolder, attachmentNode));
      } catch (IOException e) {
        throw new OutlookException("Error saving attachment in a file " + name, e);
      }
    }
    parent.save(); // save everything at the end only

    // TODO specified activity as in US_001_5

    // fire listener service to generate social activities
    // for (File f : files) {
    // try {
    // // This makes call:
    // // Utils.postFileActivity(currentNode, RESOURCE_BUNDLE_KEY_CREATED_BY, true, false, "");
    // listenerService.broadcast(ActivityCommonService.FILE_CREATED_ACTIVITY, null, f.getNode());
    // } catch (Exception e) {
    // LOG.warn("Error broadcasting the attachment file created activity for " + f.getPath(), e);
    // }
    // }

    // final String origType = org.exoplatform.wcm.ext.component.activity.listener.Utils.getActivityType();
    // try {
    // org.exoplatform.wcm.ext.component.activity.listener.Utils.setActivityType(OutlookAttachmentActivity.ACTIVITY_TYPE);
    // org.exoplatform.wcm.ext.component.activity.listener.Utils.postActivity(parent, comment, false, false,
    // "");
    // // TODO care about activity removal with the message file
    // } catch (Exception e) {
    // throw new OutlookException("Error posting activity for attachment files in " + destFolder.getName(),
    // e);
    // } finally {
    // org.exoplatform.wcm.ext.component.activity.listener.Utils.setActivityType(origType);
    // }

    postAttachmentActivity(destFolder, files, user, comment);

    if (space != null) {
      for (File f : files) {
        initDocumentLink(space, f);
      }
    }
    return files;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<File> saveAttachment(Folder destFolder,
                                   OutlookUser user,
                                   String comment,
                                   String messageId,
                                   String attachmentToken,
                                   String... attachmentIds) throws OutlookException, RepositoryException {
    return saveAttachment(null, destFolder, user, comment, messageId, attachmentToken, attachmentIds);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutlookEmail getAddress(String email, String displayName) throws OutlookException {
    return new OutlookEmail(email, displayName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutlookUser getUser(String email, String displayName, String ewsUrl) throws OutlookException, RepositoryException {
    ConversationState contextState = ConversationState.getCurrent();
    if (contextState != null) {
      String exoUsername = contextState.getIdentity().getUserId();
      if (!IdentityConstants.ANONIM.equals(exoUsername)) {
        URI mailServerUrl;
        if (ewsUrl != null) {
          try {
            URI ewsUri = new URI(ewsUrl);
            String host = ewsUri.getHost();
            int port = ewsUri.getPort();
            String scheme = ewsUri.getScheme();
            if (port <= 0) {
              port = "https".equalsIgnoreCase(scheme) ? 443 : 80;
            }

            mailServerUrl = new URI(scheme, null, host, port, null, null, null);
          } catch (URISyntaxException e) {
            throw new MailServerException("Error parsing EWS API URL " + ewsUrl, e);
          }
        } else {
          mailServerUrl = null;
        }

        OutlookUser user = authenticated.get(exoUsername);
        if (user == null) {
          // new user instance
          user = new UserImpl(email, displayName, exoUsername);
          // save user in map of authenticated for later use (multi-thread)
          authenticated.put(exoUsername, user);
        }
        if (email != null) {
          user.setEmail(email);
        }
        if (mailServerUrl != null) {
          user.setMailServerUrl(mailServerUrl);
        }
        return user;
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutlookMessage getMessage(String id,
                                   OutlookEmail user,
                                   OutlookEmail from,
                                   List<OutlookEmail> to,
                                   Calendar created,
                                   Calendar modified,
                                   String subject,
                                   String body) throws OutlookException {
    OutlookMessage message = new OutlookMessage(user, from);
    message.setId(id);
    message.setTo(to);
    message.setSubject(subject);
    message.setBody(body);
    message.setCreated(created);
    message.setModified(modified);
    return message;
  }

  /**
   * On-start initializer.
   */
  @Override
  public void start() {
    LOG.info("Outlook service successfuly started");
  }

  /**
   * On-stop finalizer.
   */
  @Override
  public void stop() {
    authenticated.clear();
    try {
      mailserverApi.close();
      LOG.info("Outlook service successfuly stopped");
    } catch (MailServerException e) {
      LOG.warn("Outlook service stop encountered with API error", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutlookSpace getSpace(String groupId) throws OutlookSpaceException, RepositoryException, OutlookException {
    OutlookSpaceImpl space = spaces.get(groupId);
    if (space == null) {
      Space socialSpace = spaceService().getSpaceByGroupId(groupId);
      if (socialSpace != null) {
        space = new OutlookSpaceImpl(socialSpace);
        spaces.put(socialSpace.getGroupId(), space);
      }
    }
    return space;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<OutlookSpace> getUserSpaces() throws OutlookSpaceException {
    return userSpaces(currentUserId());
  }

  // *********************** testing level **********************

  void setAPI(MailAPI mockedAPI) {
    this.mailserverApi = mockedAPI;
  }

  // *********************** implementation level ***************

  protected String nodeTitle(Node node) throws RepositoryException {
    return node.getProperty("exo:title").getString();
  }

  protected Node nodeContent(Node node) throws RepositoryException {
    return node.getNode("jcr:content");
  }

  protected Calendar nodeCreated(Node node) throws RepositoryException {
    return node.getProperty("jcr:created").getDate();
  }

  protected String mimeType(Node content) throws RepositoryException {
    return content.getProperty("jcr:mimeType").getString();
  }

  protected Property data(Node content) throws RepositoryException {
    return content.getProperty("jcr:data");
  }

  protected UUID generateId(String workspace, String path) {
    StringBuilder s = new StringBuilder();
    s.append(workspace);
    s.append(path);
    s.append(System.currentTimeMillis());
    s.append(String.valueOf(RANDOM.nextLong()));

    return UUID.nameUUIDFromBytes(s.toString().getBytes());
  }

  protected String nodePath(String workspace, String path) {
    return new StringBuilder().append(workspace).append(":").append(path).toString();
  }

  protected org.exoplatform.services.organization.User getExoUser(String userName) throws OutlookException {
    try {
      return organization.getUserHandler().findUserByName(userName);
    } catch (Exception e) {
      throw new OutlookException("Error searching user " + userName, e);
    }
  }

  protected Node node(String path) throws BadParameterException, RepositoryException {
    String ws = jcrService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
    return node(ws, path);
  }

  protected Node node(String workspace, String path) throws BadParameterException, RepositoryException {
    SessionProvider sp = sessionProviders.getSessionProvider(null);
    Session userSession = sp.getSession(workspace, jcrService.getCurrentRepository());

    Item item = finder.findItem(userSession, path);
    if (item.isNode()) {
      return (Node) item;
    } else {
      throw new BadParameterException("Not a node " + path);
    }
  }

  protected Node systemNode(String workspace, String path) throws BadParameterException, RepositoryException {
    SessionProvider sp = sessionProviders.getSystemSessionProvider(null);
    Session sysSession = sp.getSession(workspace, jcrService.getCurrentRepository());

    Item item = finder.findItem(sysSession, path);
    if (item.isNode()) {
      return (Node) item;
    } else {
      throw new BadParameterException("Not a node " + path);
    }
  }

  protected boolean checkout(Node node) throws RepositoryException {
    if (node.isNodeType("mix:versionable")) {
      if (!node.isCheckedOut()) {
        node.checkout();
      }
      return true;
    } else {
      return false;
    }
  }

  protected String getUserLang(String userId) throws OutlookException {
    UserProfileHandler hanlder = organization.getUserProfileHandler();
    try {
      UserProfile userProfile = hanlder.findUserProfileByName(userId);
      if (userProfile != null) {
        String lang = userProfile.getAttribute(Constants.USER_LANGUAGE);
        if (lang != null) {
          // XXX Onlyoffice doesn't support country codes (as of Apr 6, 2016)
          // All supported langauges here http://helpcenter.onlyoffice.com/tipstricks/available-languages.aspx
          int cci = lang.indexOf("_");
          if (cci > 0) {
            lang = lang.substring(0, cci);
          }
        } else {
          lang = Locale.ENGLISH.getLanguage();
        }
        return lang;
      } else {
        throw new BadParameterException("OutlookUser profile not found for " + userId);
      }
    } catch (Exception e) {
      throw new OutlookException("Error searching user profile " + userId, e);
    }
  }

  protected boolean isNull(JsonValue json) {
    return json == null || json.isNull();
  }

  protected boolean isNotNull(JsonValue json) {
    return json != null && !json.isNull();
  }

  protected Locale currentUserLocale() {
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    return context != null ? context.getLocale() : null;
  }

  protected InputStream decode(String contentBytes) {
    return new ByteArrayInputStream(Base64.decodeBase64(contentBytes));
  }

  /**
   * Add nt:file node for given content stream and title. If a node with such name exists a new name will be
   * generated by adding a numerical index to the end.
   * 
   * @param parent
   * @param title
   * @param contentType
   * @param content
   * @return
   * @throws RepositoryException
   */
  protected Node addFile(Node parent,
                         String title,
                         String summary,
                         String contentType,
                         InputStream content) throws RepositoryException {
    Node file;
    String baseName = cleanName(title);
    String name = baseName;

    int siblingNumber = 0;
    do {
      try {
        file = parent.getNode(name);
        // such node already exists - find new name for the file (by adding sibling index to the end)
        siblingNumber++;
        int extIndex = baseName.lastIndexOf(".");
        if (extIndex > 0 && extIndex < title.length()) {
          String jcrName = baseName.substring(0, extIndex);
          String jcrExt = baseName.substring(extIndex + 1);
          name = new StringBuilder(jcrName).append('-').append(siblingNumber).append('.').append(jcrExt).toString();
        } else {
          name = new StringBuilder(baseName).append('-').append(siblingNumber).toString();
        }
      } catch (PathNotFoundException e) {
        // no such node exists, add it using internalName created by CD's cleanName()
        file = parent.addNode(name, "nt:file");
        break;
      }
    } while (true);

    Node resource = file.addNode("jcr:content", "nt:resource");
    resource.setProperty("jcr:mimeType",
                         contentType != null ? contentType : ContentType.APPLICATION_OCTET_STREAM.getMimeType());
    Calendar fileDate = Calendar.getInstance();
    resource.setProperty("jcr:lastModified", fileDate);
    resource.setProperty("jcr:data", content);

    if (siblingNumber > 0) {
      int extIndex = title.lastIndexOf(".");
      if (extIndex > 0 && extIndex < title.length()) {
        String titleName = title.substring(0, extIndex);
        String titleExt = title.substring(extIndex + 1);
        title = new StringBuilder(titleName).append(" (").append(siblingNumber).append(").").append(titleExt).toString();
      } else {
        title = new StringBuilder(title).append(" (").append(siblingNumber).append(')').toString();
      }
    }

    if (!file.hasProperty("exo:title")) {
      file.addMixin(EXO_RSSENABLE);
    }
    file.setProperty("exo:title", title);
    file.setProperty("exo:summary", summary);
    try {
      file.setProperty("exo:name", title);
    } catch (ConstraintViolationException | ValueFormatException e) {
      LOG.warn("Cannot set exo:name property to '" + title + "' for file " + file.getPath() + ": " + e);
    }

    if (file.isNodeType(EXO_DATETIME)) {
      file.setProperty("exo:dateCreated", fileDate);
      file.setProperty("exo:dateModified", fileDate);
    }

    if (file.isNodeType(EXO_MODIFY)) {
      file.setProperty("exo:lastModifiedDate", fileDate);
      file.setProperty("exo:lastModifier", file.getSession().getUserID());
    }
    return file;
  }

  /**
   * Add nt:folder node with given title. If a node with such name exists and <code>forceNew</code> is
   * <code>true</code> a new name will be generated by adding a numerical index to the end, otherwise existing
   * node will be returned.
   * 
   * @param parent
   * @param title
   * @param forceNew if <code>true</code> then a new folder will be created with index in suffix, if
   *          <code>false</code> then existing folder will be returned
   * @return
   * @throws RepositoryException
   */
  protected Node addFolder(Node parent, String title, boolean forceNew) throws RepositoryException {
    Node folder;
    String baseName = cleanName(title);
    String name = baseName;

    int siblingNumber = 0;
    do {
      try {
        folder = parent.getNode(name);
        if (forceNew) {
          // such node already exists - find new name for the file (by adding sibling index to the end)
          siblingNumber++;
          int extIndex = baseName.lastIndexOf(".");
          if (extIndex > 0 && extIndex < title.length()) {
            String jcrName = baseName.substring(0, extIndex);
            String jcrExt = baseName.substring(extIndex + 1);
            name = new StringBuilder(jcrName).append('-').append(siblingNumber).append('.').append(jcrExt).toString();
          } else {
            name = new StringBuilder(baseName).append('-').append(siblingNumber).toString();
          }
        } else {
          break;
        }
      } catch (PathNotFoundException e) {
        // no such node exists, add it using internalName created by CD's cleanName()
        folder = parent.addNode(name, "nt:folder");
        break;
      }
    } while (true);

    if (folder.isNew()) {
      if (siblingNumber > 0) {
        int extIndex = title.lastIndexOf(".");
        if (extIndex > 0 && extIndex < title.length()) {
          String titleName = title.substring(0, extIndex);
          String titleExt = title.substring(extIndex + 1);
          title = new StringBuilder(titleName).append(" (").append(siblingNumber).append(").").append(titleExt).toString();
        } else {
          title = new StringBuilder(title).append(" (").append(siblingNumber).append(')').toString();
        }
      }

      folder.setProperty("exo:title", title);
      try {
        folder.setProperty("exo:name", title);
      } catch (ConstraintViolationException | ValueFormatException e) {
        LOG.warn("Cannot set exo:name property to '" + title + "' for folder " + folder.getPath() + ": " + e);
      }

      Calendar folderDate = Calendar.getInstance();
      if (folder.isNodeType(EXO_DATETIME)) {
        folder.setProperty("exo:dateCreated", folderDate);
        folder.setProperty("exo:dateModified", folderDate);
      }

      if (folder.isNodeType(EXO_MODIFY)) {
        folder.setProperty("exo:lastModifiedDate", folderDate);
        folder.setProperty("exo:lastModifier", folder.getSession().getUserID());
      }
    }
    return folder;
  }

  protected String currentUserId() {
    ConversationState contextState = ConversationState.getCurrent();
    if (contextState != null) {
      return contextState.getIdentity().getUserId();
    }
    return IdentityConstants.ANONIM;
  }

  /**
   * Current space in the request (will work only for portal requests to space portlets).
   * 
   * @return
   */
  @Deprecated // TODO NOT used
  protected Space contextSpace() {
    try {
      // TODO use thread-local to cache it a bit
      return Utils.getSpaceByContext();
    } catch (NullPointerException e) {
      // XXX NPE has a place when running not in portal request, assume it as normal
    } catch (Throwable e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Error getting context space: " + e.getMessage(), e);
      }
    }
    return null;
  }

  protected SpaceService spaceService() {
    return (SpaceService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SpaceService.class);
  }

  protected IdentityManager socialIdentityManager() {
    return (IdentityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(IdentityManager.class);
  }

  protected ActivityManager socialActivityManager() {
    return (ActivityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ActivityManager.class);
  }

  protected List<OutlookSpace> userSpaces(String userId) throws OutlookSpaceException {
    List<OutlookSpace> spaces = new ArrayList<OutlookSpace>();
    ListAccess<Space> list = spaceService().getMemberSpaces(userId);
    try {
      for (Space socialSpace : list.load(0, list.getSize())) {
        spaces.add(new OutlookSpaceImpl(socialSpace));
      }
      return spaces;
    } catch (Throwable e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Error loading user spaces", e);
      }
      throw new OutlookSpaceException("Error loading user spaces", e);
    }
  }

  /**
   * Generate the group documents (as /Groups/spaces/$SPACE_GROUP_ID/Documents).<br>
   * 
   * @param groupId {@link String}
   * @return {@link String}
   * @throws Exception
   */
  protected String groupDocsPath(String groupId) {
    // XXX we do here as ECMS does in ManageDriveServiceImpl
    return GROUP_DRIVE_PARRTEN.replace("${groupId}", groupId);
  }

  /**
   * Generate the user documents (as /Users/${userId}/Private/Documents).<br>
   * 
   * @param userId {@link String}
   * @return {@link String}
   * @throws Exception
   */
  protected String userDocsPath(String userId) {
    // XXX we do here as ECMS does in ManageDriveServiceImpl
    return PERSONAL_DRIVE_PARRTEN.replace("${userId}", userId) + "/Documents";
  }

  /**
   * Generate the group node path (as /Groups/spaces/$SPACE_GROUP_ID).<br>
   * 
   * @param groupId {@link String}
   * @return {@link String}
   * @throws Exception
   */
  protected String groupPath(String groupId) throws Exception {
    String groupsPath = hierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
    return groupsPath + groupId;
  }

  protected void initDocumentLink(OutlookSpace space, HierarchyNode node) throws OutlookException {
    // Code adapted from ECMS's PermlinkActionComponent.getPermlink()
    // We need like the following:
    // https://peter.exoplatform.com.ua:8443/portal/g/:spaces:product_team/product_team/documents?path=.spaces.product_team/Groups/spaces/product_team/Documents/uploads/page_management_https_loading.png

    StringBuilder url = new StringBuilder();

    String groupDriveName = space.getGroupId().replace("/", ".");
    String nodePath = node.getPath().replaceAll("/+", "/");

    String path = new StringBuilder().append(groupDriveName).append(nodePath).toString();
    NodeURL nodeURL = Util.getPortalRequestContext().createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteType.GROUP, // GROUP
                                                         space.getGroupId(), // /spaces/product_team
                                                         space.getShortName() + "/documents"); // product_team/documents
    nodeURL.setResource(resource);
    nodeURL.setQueryParameterValue("path", path);

    PortalRequestContext portalRequest = Util.getPortalRequestContext();
    if (portalRequest != null) {
      HttpServletRequest request = portalRequest.getRequest();
      try {
        URI requestUri = new URI(request.getScheme(),
                                 null,
                                 request.getServerName(),
                                 request.getServerPort(),
                                 null,
                                 null,
                                 null);
        url.append(requestUri.toASCIIString());
        url.append(nodeURL.toString());
      } catch (URISyntaxException e) {
        throw new OutlookException("Error creating server URL " + request.getRequestURI().toString(), e);
      }
    } else {
      LOG.warn("Portal request not found. Node URL will be relative to this server (w/o host name).");
      url.append(nodeURL.toString());
    }

    node.setUrl(url.toString());
  }

  /**
   * Find given user Personal Documents folder using user session.
   * 
   * @param displayName {@link String}
   * @return {@link Node} Personal Documents folder node or <code>null</code>
   * @throws Exception
   */
  protected Node userDocumentsNode(String userName) throws Exception {
    // code idea based on ECMS's UIJCRExplorerPortlet.getUserDrive()
    for (DriveData userDrive : driveService.getPersonalDrives(userName)) {
      String homePath = userDrive.getHomePath();
      if (homePath.endsWith("/Private")) {
        // TODO
        // SessionProvider sessionProvider = sessionProviders.getSessionProvider(null);
        // Node userNode = hierarchyCreator.getUserNode(sessionProvider, displayName);
        String driveRootPath = org.exoplatform.services.cms.impl.Utils.getPersonalDrivePath(homePath, userName);
        // int uhlen = userNode.getPath().length();
        // if (homePath.length() > uhlen) {
        // it should be w/o leading slash, e.g. "Private"
        // String driveSubPath = driveRootPath.substring(uhlen + 1);
        return node(driveRootPath).getParent().getNode("Public");
        // }
      }
    }
    return null;
  }

  /**
   * Find given group Documents folder using current user session.
   *
   * @param groupName {@link String}
   * @return {@link Node} space's Documents folder node or <code>null</code>
   * @throws Exception
   */
  protected Node spaceDocumentsNode(String groupId) throws Exception {
    // String groupDriveName = groupId.replace("/", ".");
    // DriveData groupDrive = driveService.getDriveByName(groupDriveName);
    // if (groupDrive != null) {
    // // TODO
    // //SessionProvider sessionProvider = sessionProviders.getSessionProvider(null);
    // // we actually don't need user home node, just a JCR session
    // //Session session = hierarchyCreator.getUserNode(sessionProvider, displayName).getSession();
    // //return (Node) session.getItem(groupDrive.getHomePath());
    // return node(groupDrive.getHomePath());
    // } else {
    // return null;
    // }
    return node(groupDocsPath(groupId));
  }

  /**
   * Set read permissions on the target node to all given identities (e.g. space group members). If node not
   * yet <code>exo:privilegeable</code> it will add such mixin to allow set the permissions first. Requested
   * permissions will not be set to the children nodes.<br>
   * 
   * @param node {@link Node} link target node
   * @param identities array of {@link String} with user identifiers (names or memberships)
   * @throws AccessControlException
   * @throws RepositoryException
   */
  protected void setPermissions(Node node, String... identities) throws AccessControlException, RepositoryException {
    setPermissions(node, true, false, identities);
  }

  /**
   * Set read permissions on the target node to all given identities (e.g. space group members). Permissions
   * will not be set if target not <code>exo:privilegeable</code> and <code>forcePrivilegeable</code> is
   * <code>false</code>. If <code>deep</code> is <code>true</code> the target children nodes will be checked
   * also for a need to set the requested permissions. <br>
   * 
   * @param node {@link Node} link target node
   * @param deep {@link Boolean} if <code>true</code> then also children nodes will be set to the requested
   *          permissions
   * @param forcePrivilegeable {@link Boolean} if <code>true</code> and node not yet
   *          <code>exo:privilegeable</code> it will add such mixin to allow set the permissions.
   * @param identities array of {@link String} with user identifiers (names or memberships)
   * @throws AccessControlException
   * @throws RepositoryException
   */
  protected void setPermissions(Node node,
                                boolean deep,
                                boolean forcePrivilegeable,
                                String... identities) throws AccessControlException, RepositoryException {
    ExtendedNode target = (ExtendedNode) node;
    boolean setPermissions = true;
    if (target.canAddMixin(EXO_PRIVILEGEABLE)) {
      if (forcePrivilegeable) {
        target.addMixin(EXO_PRIVILEGEABLE);
      } else {
        // will not set permissions on this node, but will check the child nodes
        setPermissions = false;
      }
    } // else, already exo:privilegeable
    if (setPermissions) {
      for (String identity : identities) {
        // It is for special debug cases
        // if (LOG.isDebugEnabled()) {
        // LOG.debug(">>> hasPermission " + identity + " identity: "
        // + IdentityHelper.hasPermission(target.getACL(), identity, PermissionType.READ));
        // }
        String[] ids = identity.split(":");
        if (ids.length == 2) {
          // it's group and we want allow given identity read only and additionally let managers remove the
          // link
          String managerMembership;
          try {
            MembershipType managerType = organization.getMembershipTypeHandler().findMembershipType("manager");
            managerMembership = managerType.getName();
          } catch (Exception e) {
            LOG.error("Error finding manager membership in organization service. "
                + "Will use any (*) to allow remove shared cloud file link", e);
            managerMembership = "*";
          }
          target.setPermission(new StringBuilder(managerMembership).append(':').append(ids[1]).toString(),
                               MANAGER_PERMISSION);
          target.setPermission(identity, READER_PERMISSION);
        } else {
          // in other cases, we assume it's user identity and user should be able to remove the node
          target.setPermission(identity, MANAGER_PERMISSION);
        }
      }
    }
    if (deep) {
      // check the all children also, but don't force adding exo:privilegeable
      for (NodeIterator niter = target.getNodes(); niter.hasNext();) {
        Node child = niter.nextNode();
        setPermissions(child, true, false, identities);
      }
    }
  }

  protected Node messagesFolder(Node parent, String... identity) throws RepositoryException {
    Node messagesFolder;
    if (!parent.hasNode("outlook-messages")) {
      messagesFolder = parent.addNode(OUTLOOK_MESSAGES_NAME, "nt:folder");
      messagesFolder.setProperty("exo:title", OUTLOOK_MESSAGES_TITLE);
      try {
        messagesFolder.setProperty("exo:name", OUTLOOK_MESSAGES_TITLE);
      } catch (ConstraintViolationException | ValueFormatException e) {
        LOG.warn("Cannot set exo:name property for folder " + messagesFolder.getPath() + ": " + e);
      }
      if (identity != null) {
        setPermissions(messagesFolder, identity);
      }
      parent.save();
    } else {
      messagesFolder = parent.getNode(OUTLOOK_MESSAGES_NAME);
    }
    return messagesFolder;
  }

  protected Node addMessageFile(Node parent, OutlookMessage message) throws RepositoryException,
                                                                     UnsupportedEncodingException,
                                                                     IOException {
    try (InputStream content = new ByteArrayInputStream(message.getBody().getBytes("UTF8"))) {
      // message file goes w/o summary, it will be generated in UI (OutlookMessageActivity)
      Node messageFile = addFile(parent, message.getSubject(), null, "text/html", content);
      messageFile.addMixin(MESSAGE_NODETYPE);
      messageFile.setProperty("mso:userEmail", message.getUser().getEmail());
      messageFile.setProperty("mso:userName", message.getUser().getDisplayName());
      messageFile.setProperty("mso:fromEmail", message.getFrom().getEmail());
      messageFile.setProperty("mso:fromName", message.getFrom().getDisplayName());
      // messageFile.setProperty("mso:toEmail", userEmail);
      messageFile.setProperty("mso:created", message.getCreated());
      messageFile.setProperty("mso:modified", message.getModified());
      messageFile.setProperty("mso:messageId", message.getId());
      return messageFile;
    }
  }

  protected ExoSocialActivity postAttachmentActivity(Folder destFolder,
                                                     List<File> files,
                                                     OutlookUser user,
                                                     String comment) throws RepositoryException {
    String author = user.getLocalUser();

    // FYI Code inspired by UIDocActivityComposer
    Map<String, String> activityParams = new LinkedHashMap<String, String>();

    StringBuilder uuidsLine = new StringBuilder();
    for (File f : files) {
      if (uuidsLine.length() > 0) {
        uuidsLine.append(',');
      }
      uuidsLine.append(f.getNode().getUUID());
    }

    activityParams.put(OutlookAttachmentActivity.FILE_UUIDS, uuidsLine.toString());
    activityParams.put(OutlookAttachmentActivity.WORKSPACE, destFolder.getNode().getSession().getWorkspace().getName());
    activityParams.put(OutlookAttachmentActivity.COMMENT, comment);
    activityParams.put(OutlookAttachmentActivity.AUTHOR, author);

    Calendar activityDate = Calendar.getInstance();
    DateFormat dateFormatter = new SimpleDateFormat(ISO8601.SIMPLE_DATETIME_FORMAT);
    String dateString = dateFormatter.format(activityDate.getTime());
    activityParams.put(OutlookAttachmentActivity.DATE_CREATED, dateString);
    activityParams.put(OutlookAttachmentActivity.DATE_LAST_MODIFIED, dateString);

    // if NT_FILE
    // activityParams.put(UIDocActivity.ID, node.isNodeType(NodetypeConstant.MIX_REFERENCEABLE) ?
    // node.getUUID() : "");
    // activityParams.put(UIDocActivity.CONTENT_NAME, node.getName());
    // activityParams.put(UIDocActivity.AUTHOR, activityOwnerId);
    // activityParams.put(UIDocActivity.DATE_CREATED, strDateCreated);
    // activityParams.put(UIDocActivity.LAST_MODIFIED, strLastModified);
    // activityParams.put(UIDocActivity.CONTENT_LINK, UIDocActivity.getContentLink(node));

    //
    IdentityManager identityManager = socialIdentityManager();
    Identity authorIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, author, true);

    //
    ExoSocialActivity activity = new ExoSocialActivityImpl(authorIdentity.getId(),
                                                           OutlookAttachmentActivity.ACTIVITY_TYPE,
                                                           comment,
                                                           null);
    activity.setTemplateParams(activityParams);

    //
    ActivityManager activityManager = socialActivityManager();
    activityManager.saveActivityNoReturn(authorIdentity, activity);

    activity = activityManager.getActivity(activity.getId());

    String activityId = activity.getId();
    if (!StringUtils.isEmpty(activityId)) {
      for (File f : files) {
        Node n = f.getNode();
        ActivityTypeUtils.attachActivityId(n, activityId);
        n.save();
      }
    }

    //
    return activity;
  }

  /**
   * Make JCR compatible item name.
   * 
   * @param String str
   * @return String - JCR compatible name of local file
   */
  public static String cleanName(String name) {
    String str = accentsConverter.transliterate(name.trim());
    // the character ? seems to not be changed to d by the transliterate function
    StringBuilder cleanedStr = new StringBuilder(str.trim());
    // delete special character
    if (cleanedStr.length() == 1) {
      char c = cleanedStr.charAt(0);
      if (c == '.' || c == '/' || c == ':' || c == '[' || c == ']' || c == '*' || c == '\'' || c == '"' || c == '|') {
        // any -> _<NEXNUM OF c>
        cleanedStr.deleteCharAt(0);
        cleanedStr.append('_');
        cleanedStr.append(Integer.toHexString(c).toUpperCase());
      }
    } else {
      for (int i = 0; i < cleanedStr.length(); i++) {
        char c = cleanedStr.charAt(i);
        if (c == '/' || c == ':' || c == '[' || c == ']' || c == '*' || c == '\'' || c == '"' || c == '|') {
          cleanedStr.deleteCharAt(i);
          cleanedStr.insert(i, '_');
        } else if (!(Character.isLetterOrDigit(c) || Character.isWhitespace(c) || c == '.' || c == '-' || c == '_')) {
          cleanedStr.deleteCharAt(i--);
        }
      }
    }
    return cleanedStr.toString().trim(); // finally trim also
  }
}
