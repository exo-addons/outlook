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
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.entity.ContentType;
import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.StringCommonUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.forum.bbcode.core.ExtendedBBCodeProvider;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.TransformHTML;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.forum.ext.activity.BuildLinkUtils;
import org.exoplatform.forum.ext.activity.BuildLinkUtils.PORTLET_INFO;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumAdministration;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.ForumServiceUtils;
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.outlook.forum.ForumUtils;
import org.exoplatform.outlook.jcr.File;
import org.exoplatform.outlook.jcr.Folder;
import org.exoplatform.outlook.jcr.HierarchyNode;
import org.exoplatform.outlook.jcr.NodeFinder;
import org.exoplatform.outlook.jcr.UserDocuments;
import org.exoplatform.outlook.mail.MailAPI;
import org.exoplatform.outlook.mail.MailServerException;
import org.exoplatform.outlook.social.OutlookAttachmentActivity;
import org.exoplatform.outlook.social.OutlookMessageActivity;
import org.exoplatform.portal.Constants;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserProfileHandler;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.application.PeopleService;
import org.exoplatform.social.core.application.SpaceActivityPublisher;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.webui.Utils;
import org.exoplatform.social.webui.activity.UIDefaultActivity;
import org.exoplatform.wcm.webui.reader.ContentReader;
import org.exoplatform.web.security.security.CookieTokenService;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Permission;
import org.exoplatform.wiki.mow.api.PermissionEntry;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.IDType;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.ws.frameworks.json.value.JsonValue;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.picocontainer.Startable;
import org.xwiki.rendering.syntax.Syntax;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessControlException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
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

  protected static final String         WIKI_PERMISSION_ANY    = "any";

  protected static final String         UPLAODS_FOLDER_TITLE   = "Uploads";

  protected static final String         SPACES_HOME            = "/Groups/spaces";

  protected static final String         PERSONAL_DOCUMENTS     = "Personal Documents";

  protected static final String[]       READER_PERMISSION      = new String[] { PermissionType.READ };

  protected static final String[]       MANAGER_PERMISSION     = new String[] { PermissionType.READ, PermissionType.REMOVE };

  protected static final Log            LOG                    = ExoLogger.getLogger(OutlookServiceImpl.class);

  protected static final Random         RANDOM                 = new Random();

  protected static final Transliterator accentsConverter       = Transliterator.getInstance("Latin; NFD; [:Nonspacing Mark:] Remove; NFC;");

  protected class UserFolder extends Folder {

    protected UserFolder(Folder parent, Node node) throws RepositoryException, OutlookException {
      super(parent, node);
    }

    protected UserFolder(String parentPath, Node node) throws RepositoryException, OutlookException {
      super(parentPath, node);
    }

    protected UserFolder(Node node) throws RepositoryException, OutlookException {
      super(node.getPath(), node);
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected File newFile(Folder parent, Node node) throws RepositoryException, OutlookException {
      File file = new UserFile(parent, node);
      return file;
    }

  }

  protected class PersonalDocumentsFolder extends UserFolder implements UserDocuments {

    protected PersonalDocumentsFolder(Node node) throws RepositoryException, OutlookException {
      super(node);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Folder getRootFolder() throws OutlookException {
      return this;
    }

    /**
     * {@inheritDoc}
     * 
     */
    @Override
    public Folder getFolder(String path) throws OutlookException, RepositoryException {
      String rootPath = getPath();
      Folder folder;
      if (rootPath.equals(path)) {
        folder = this;
      } else if (path.startsWith(rootPath)) {
        Node node = node(path);
        folder = new UserFolder(node.getParent().getPath(), node);
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
    public Collection<File> findAllLastDocuments(String text) throws RepositoryException, OutlookException {
      QueryManager qm = getNode().getSession().getWorkspace().getQueryManager();

      Set<File> res = new LinkedHashSet<File>();

      if (text == null || text.length() == 0) {
        Query q = qm.createQuery("SELECT * FROM nt:file WHERE exo:lastModifier='" + currentUserId()
            + "' ORDER BY exo:lastModifiedDate DESC, exo:title ASC", Query.SQL);
        fetchQuery(q.execute(), 20, res);
      } else {
        Query qOwn = qm.createQuery("SELECT * FROM nt:file WHERE exo:lastModifier='" + currentUserId()
            + "' AND jcr:path LIKE '" + getPath() + "/%' AND exo:title LIKE '%" + text
            + "%' ORDER BY exo:lastModifiedDate DESC, exo:title ASC", Query.SQL);
        // fetch first three modified by this user only
        fetchQuery(qOwn.execute(), 3, res);
        // and add all others up to total 20 files
        Query qOthers = qm.createQuery("SELECT * FROM nt:file WHERE jcr:path LIKE '" + getPath()
            + "/%' AND exo:title LIKE '%" + text + "%' ORDER BY exo:lastModifiedDate DESC, exo:title ASC", Query.SQL);
        fetchQuery(qOthers.execute(), 17, res);
      }
      return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<File> findLastDocuments(String text) throws RepositoryException, OutlookException {
      QueryManager qm = getNode().getSession().getWorkspace().getQueryManager();

      Set<File> res = new LinkedHashSet<File>();

      // TODO this search will not include files from user's Public folder
      Query q;
      if (text == null || text.length() == 0) {
        q = qm.createQuery("SELECT * FROM nt:file WHERE jcr:path LIKE '" + getPath()
            + "/%' ORDER BY exo:lastModifiedDate DESC, exo:title ASC", Query.SQL);
      } else {
        q = qm.createQuery("SELECT * FROM nt:file WHERE jcr:path LIKE '" + getPath() + "/%' AND exo:title LIKE '%" + text
            + "%'", Query.SQL);
      }
      fetchQuery(q.execute(), 20, res);
      return res;
    }

    protected String getDriveName() {
      // XXX we use what pointed in XML config
      return PERSONAL_DOCUMENTS;
    }
  }

  protected class UserFile extends File {

    protected UserFile(Folder parent, Node node) throws RepositoryException, OutlookException {
      super(parent, node);
    }

    protected UserFile(String parentPath, Node node) throws RepositoryException, OutlookException {
      super(parentPath, node);
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
    public ExoSocialActivity postActivity(OutlookMessage message) throws OutlookException {
      // save text to user documents
      try {
        Node userDocs = userDocumentsNode(localUser);
        if (userDocs != null) {
          Node userPublicFolder = userDocs.getParent().getNode("Public");
          Node messagesFolder = messagesFolder(userPublicFolder, localUser, "member:/platform/users");
          Node messageFile = addMessageFile(messagesFolder, message);
          setPermissions(messageFile, localUser, "member:/platform/users");
          messagesFolder.save();
          message.setFileNode(messageFile);

          // TODO
          // return postMessageActivity(message);

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
          // templateParams.put(UIDocActivity.WORKSPACE,
          // messagesFolder.getSession().getWorkspace().getName());
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
        } else {
          throw new OutlookException("Has no Personal Documents folder for user " + localUser);
        }
      } catch (Exception e) {
        throw new OutlookException("Error posting activity for user " + localUser, e);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExoSocialActivity postActivity(String title, String body) throws Exception {
      // post activity to user status stream
      Identity userIdentity = socialIdentityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                        currentUserId(),
                                                                        true);
      String safeTitle = safeText(title);
      String safeBody = safeHtml(body);
      ExoSocialActivity activity = new ExoSocialActivityImpl(userIdentity.getId(), null, safeTitle, safeBody);
      socialActivityManager.saveActivityNoReturn(userIdentity, activity);
      activity.setPermanLink(LinkProvider.getSingleActivityUrl(activity.getId()));
      return activity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExoSocialActivity postActivity(String text) throws Exception {
      // post activity to user status stream
      Identity userIdentity = socialIdentityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                        currentUserId(),
                                                                        true);
      String safeText = safeActivityMessage(text);
      ExoSocialActivity activity = new ExoSocialActivityImpl(userIdentity.getId(),
                                                             PeopleService.PEOPLE_APP_ID,
                                                             safeText,
                                                             null);
      // XXX we do like done UIDefaultActivityComposer
      activity.setType(UIDefaultActivity.ACTIVITY_TYPE);

      socialActivityManager.saveActivityNoReturn(userIdentity, activity);
      activity.setPermanLink(LinkProvider.getSingleActivityUrl(activity.getId()));
      return activity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page addWikiPage(OutlookMessage message) throws Exception {
      String wikiType = PortalConfig.PORTAL_TYPE;
      String creator = message.getUser().getLocalUser();
      List<String> users = new ArrayList<String>();
      users.add(creator);
      // TODO add space group to users?

      return createWikiPage(wikiType,
                            "intranet",
                            creator,
                            message.getSubject(),
                            messageSummary(message),
                            message.getBody(),
                            users);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Topic addForumTopic(String categoryId, String forumId, OutlookMessage message) throws Exception {
      return createForumTopic(categoryId,
                              forumId,
                              message.getUser().getLocalUser(),
                              message.getSubject(),
                              messageSummary(message),
                              message.getBody());
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
      protected void readChildNodes() throws RepositoryException, OutlookException {
        super.readChildNodes();
        for (Folder sf : this.subfolders.get()) {
          initDocumentLink(OutlookSpaceImpl.this, sf);
        }
        for (File f : this.files.get()) {
          initDocumentLink(OutlookSpaceImpl.this, f);
        }
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
      super(socialSpace.getGroupId(), socialSpace.getDisplayName(), socialSpace.getShortName(), socialSpace.getPrettyName());
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
      if (root != null) {
        // ensure folder's node valid
        try {
          root.getNode().getIndex();
          return root;
        } catch (InvalidItemStateException e) {
          // it's invalid
        }
      }
      root = new RootFolder(rootPath, node(rootPath));
      rootFolder.set(root);
      return root;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<File> findLastDocuments(String text) throws RepositoryException, OutlookException {
      Folder root = getRootFolder();
      QueryManager qm = root.getNode().getSession().getWorkspace().getQueryManager();

      Set<File> res = new LinkedHashSet<File>();

      if (text == null || text.length() == 0) {
        Query qOwn = qm.createQuery("SELECT * FROM nt:file WHERE exo:lastModifier='" + currentUserId()
            + "' AND jcr:path LIKE '" + root.getPath() + "/%' ORDER BY exo:lastModifiedDate DESC, exo:title ASC", Query.SQL);
        // fetch first three modified by this user only
        fetchQuery(qOwn.execute(), 3, res);
        // and add all others up to total 20 files
        Query qOthers = qm.createQuery("SELECT * FROM nt:file WHERE jcr:path LIKE '" + root.getPath()
            + "/%' ORDER BY exo:lastModifiedDate DESC, exo:title ASC", Query.SQL);
        fetchQuery(qOthers.execute(), 17, res);
      } else {
        Query qOwn = qm.createQuery("SELECT * FROM nt:file WHERE exo:lastModifier='" + currentUserId()
            + "' AND jcr:path LIKE '" + root.getPath() + "/%' AND exo:title LIKE '%" + text
            + "%' ORDER BY exo:lastModifiedDate DESC, exo:title ASC", Query.SQL);
        // fetch first three modified by this user only
        fetchQuery(qOwn.execute(), 3, res);
        // and add all others up to total 20 files
        Query qOthers = qm.createQuery("SELECT * FROM nt:file WHERE jcr:path LIKE '" + root.getPath()
            + "/%' AND exo:title LIKE '%" + text + "%' ORDER BY exo:lastModifiedDate DESC, exo:title ASC", Query.SQL);
        fetchQuery(qOthers.execute(), 17, res);
      }
      return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExoSocialActivity postActivity(OutlookMessage message) throws Exception {
      // post activity to space status stream under current user
      // TODO cleanup or use instead of file activity
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
      message.setFileNode(messageFile);

      // TODO
      // return postMessageActivity(message);

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

    /**
     * {@inheritDoc}
     */
    @Override
    public ExoSocialActivity postActivity(OutlookUser user, String title, String body) throws Exception {
      // post activity to space status stream under current user
      Identity spaceIdentity = socialIdentityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, this.prettyName, true);
      Identity userIdentity = socialIdentityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                        currentUserId(),
                                                                        true);
      String safeTitle = safeText(title);
      String safeBody = safeHtml(body);
      ExoSocialActivity activity = new ExoSocialActivityImpl(userIdentity.getId(),
                                                             SpaceActivityPublisher.SPACE_APP_ID,
                                                             safeTitle,
                                                             safeBody);
      socialActivityManager.saveActivityNoReturn(spaceIdentity, activity);
      activity.setPermanLink(LinkProvider.getSingleActivityUrl(activity.getId()));
      return activity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExoSocialActivity postActivity(OutlookUser user, String text) throws Exception {
      // post activity to space status stream under current user
      Identity spaceIdentity = socialIdentityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, this.prettyName, true);
      Identity userIdentity = socialIdentityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                        currentUserId(),
                                                                        true);
      String safeText = safeActivityMessage(text);
      ExoSocialActivity activity = new ExoSocialActivityImpl(userIdentity.getId(),
                                                             SpaceActivityPublisher.SPACE_APP_ID,
                                                             safeText,
                                                             null);
      // XXX we do like done UIDefaultActivityComposer
      activity.setType(UIDefaultActivity.ACTIVITY_TYPE);

      socialActivityManager.saveActivityNoReturn(spaceIdentity, activity);
      activity.setPermanLink(LinkProvider.getSingleActivityUrl(activity.getId()));
      return activity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page addWikiPage(OutlookMessage message) throws Exception {
      String wikiType = PortalConfig.GROUP_TYPE;
      String creator = message.getUser().getLocalUser();
      List<String> users = new ArrayList<String>();
      users.add(creator);
      // TODO add space group to users?

      return createWikiPage(wikiType,
                            getGroupId(),
                            creator,
                            message.getSubject(),
                            messageSummary(message),
                            message.getBody(),
                            users);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Topic addForumTopic(OutlookMessage message) throws Exception {
      String creator = message.getUser().getLocalUser();

      //
      Group group = organization.getGroupHandler().findGroupById(getGroupId());
      String parentGrId = group.getParentId();

      // Category must exists as we are running against existing space
      String categoryId = org.exoplatform.forum.service.Utils.CATEGORY
          + parentGrId.replaceAll(CommonUtils.SLASH, CommonUtils.EMPTY_STR);

      // Forum must exists as we are running against existing space
      String forumId = org.exoplatform.forum.service.Utils.FORUM_SPACE_ID_PREFIX + group.getGroupName();

      //
      return createForumTopic(categoryId,
                              forumId,
                              creator,
                              message.getSubject(),
                              messageSummary(message),
                              message.getBody());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Topic addForumTopic(OutlookUser user, String name, String text) throws Exception {
      String creator = user.getLocalUser();

      //
      Group group = organization.getGroupHandler().findGroupById(getGroupId());
      String parentGrId = group.getParentId();

      // Category must exists as we are running against existing space
      String categoryId = org.exoplatform.forum.service.Utils.CATEGORY
          + parentGrId.replaceAll(CommonUtils.SLASH, CommonUtils.EMPTY_STR);

      // Forum must exists as we are running against existing space
      String forumId = org.exoplatform.forum.service.Utils.FORUM_SPACE_ID_PREFIX + group.getGroupName();

      //
      return createForumTopic(categoryId, forumId, creator, name, null, text);
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

  protected final WikiService                                 wikiService;

  protected final ForumService                                forumService;

  protected final TrashService                                trashService;

  protected final ResourceBundleService                       resourceBundleService;

  protected final PolicyFactory                               htmlPolicy        = Sanitizers.BLOCKS.and(Sanitizers.FORMATTING)
                                                                                                   .and(Sanitizers.IMAGES)
                                                                                                   .and(Sanitizers.LINKS)
                                                                                                   .and(Sanitizers.TABLES)
                                                                                                   // with
                                                                                                   // extra
                                                                                                   // attributes
                                                                                                   // for
                                                                                                   // tables
                                                                                                   // (MS
                                                                                                   // loves
                                                                                                   // to use
                                                                                                   // them
                                                                                                   // for HTML
                                                                                                   // re-formating)
                                                                                                   .and(new HtmlPolicyBuilder().allowStandardUrlProtocols()
                                                                                                                               .allowElements("table",
                                                                                                                                              "th",
                                                                                                                                              "tr",
                                                                                                                                              "td")
                                                                                                                               .allowAttributes("border",
                                                                                                                                                "cellpadding",
                                                                                                                                                "cellspacing",
                                                                                                                                                "width",
                                                                                                                                                "height")
                                                                                                                               .onElements("table")
                                                                                                                               .allowAttributes("bgcolor",
                                                                                                                                                "width",
                                                                                                                                                "height",
                                                                                                                                                "colspan",
                                                                                                                                                "rowspan")
                                                                                                                               .onElements("td",
                                                                                                                                           "tr",
                                                                                                                                           "th")
                                                                                                                               .toFactory())
                                                                                                   .and(Sanitizers.STYLES);

  protected final PolicyFactory                               textPolicy        = new HtmlPolicyBuilder().toFactory();

  /**
   * Custom policy to allow supported elements in activity text as described in
   * <a href=
   * "https://www.exoplatform.com/docs/PLF43/PLFUserGuide.GettingStarted.ActivitiesInActivityStream.HTMLTags.html">
   * Platform User Guide</a>
   */
  protected final PolicyFactory                               activityPolicy    = new HtmlPolicyBuilder().allowUrlProtocols("http",
                                                                                                                            "https")
                                                                                                         .allowElements("b",
                                                                                                                        "i",
                                                                                                                        "a",
                                                                                                                        "span",
                                                                                                                        "em",
                                                                                                                        "strong",
                                                                                                                        "p",
                                                                                                                        "ol",
                                                                                                                        "ul",
                                                                                                                        "li",
                                                                                                                        "br",
                                                                                                                        "img",
                                                                                                                        "blockquote",
                                                                                                                        "q")
                                                                                                         .allowAttributes("href")
                                                                                                         .onElements("a")
                                                                                                         .allowAttributes("target")
                                                                                                         .matching(true,
                                                                                                                   "_blank")
                                                                                                         .onElements("a")
                                                                                                         .allowAttributes("alt",
                                                                                                                          "src")
                                                                                                         .onElements("img")
                                                                                                         .toFactory();

  protected final Pattern                                     linkWithTarget    = Pattern.compile("<a(?=\\s|>).*?(target=['\"].*?['\"])[^>]*>.*?<\\/a>",
                                                                                                  Pattern.CASE_INSENSITIVE
                                                                                                      | Pattern.MULTILINE
                                                                                                      | Pattern.DOTALL);

  protected final Pattern                                     linkWithoutTarget = Pattern.compile("<a(?=\\s)(?:(?!target=).)*?([.\\W\\w\\S\\s[^>]])*?(>)",
                                                                                                  Pattern.CASE_INSENSITIVE
                                                                                                      | Pattern.MULTILINE
                                                                                                      | Pattern.DOTALL);

  /**
   * Authenticated users.
   */
  protected final ConcurrentHashMap<String, OutlookUser>      authenticated     = new ConcurrentHashMap<String, OutlookUser>();

  /**
   * Spaces cache.
   * TODO There is an issue with threads when different requests reuse them. Space's root node may be already
   * invalid. See also in getRootFolder().
   */
  protected final ConcurrentHashMap<String, OutlookSpaceImpl> spaces            = new ConcurrentHashMap<String, OutlookSpaceImpl>();

  protected MailAPI                                           mailserverApi;

  protected String                                            trashHomePath;

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
  public OutlookServiceImpl(RepositoryService jcrService,
                            SessionProviderService sessionProviders,
                            IdentityRegistry identityRegistry,
                            NodeHierarchyCreator hierarchyCreator,
                            NodeFinder finder,
                            OrganizationService organization,
                            CookieTokenService tokenService,
                            ManageDriveService driveService,
                            ListenerService listenerService,
                            WikiService wikiService,
                            ForumService forumService,
                            TrashService trashService,
                            ResourceBundleService resourceBundleService,
                            InitParams params) throws ConfigurationException, MailServerException {

    this.jcrService = jcrService;
    this.sessionProviders = sessionProviders;
    this.identityRegistry = identityRegistry;
    this.hierarchyCreator = hierarchyCreator;
    this.finder = finder;
    this.organization = organization;
    this.tokenService = tokenService;
    this.driveService = driveService;
    this.listenerService = listenerService;
    this.wikiService = wikiService;
    this.forumService = forumService;
    this.trashService = trashService;
    this.resourceBundleService = resourceBundleService;

    // API for user requests (uses credentials from eXo user profile)
    MailAPI api = new MailAPI();
    this.mailserverApi = api;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Deprecated
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
  @Deprecated
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
      byte[] decoded = Base64.decodeBase64(contentBytes);
      String content;
      try {
        content = new String(decoded, "UTF-8");
      } catch (UnsupportedEncodingException e1) {
        throw new OutlookException("Error reading message content in UTF-8 encoding: " + name, e1);
      }
      String safeContent = safeHtml(content);

      // Save in JCR
      try (InputStream contentStream = new ByteArrayInputStream(safeContent.getBytes())) {
        Node attachmentNode = addFile(parent, name, contentType, contentStream);
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

    postAttachmentActivity(destFolder, files, user, safeText(comment));

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
  public OutlookMessage buildMessage(String id,
                                     OutlookUser user,
                                     OutlookEmail from,
                                     List<OutlookEmail> to,
                                     Calendar created,
                                     Calendar modified,
                                     String subject,
                                     String body) throws OutlookException {
    OutlookMessage message = new OutlookMessage(user);
    message.setId(id);
    message.setFrom(from);
    message.setTo(to);
    message.setSubject(subject);
    message.setBody(body);
    message.setCreated(created);
    message.setModified(modified);
    return message;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutlookMessage getMessage(OutlookUser user, String messageId, String messageToken) throws OutlookException {

    // Read message from Exchange server by ID
    JsonValue vatt = mailserverApi.getMessage(user, messageId, messageToken);
    JsonValue vSubject = vatt.getElement("Subject");
    if (isNull(vSubject)) {
      throw new OutlookFormatException("Message " + messageId + " doesn't contain Subject");
    }
    String subject = vSubject.getStringValue();
    JsonValue vTo = vatt.getElement("ToRecipients");
    if (isNull(vTo)) {
      throw new OutlookFormatException("Message (" + messageId + " : " + subject + ") doesn't contain ToRecipients");
    }
    if (!vTo.isArray()) {
      throw new OutlookFormatException("Message (" + messageId + " : " + subject + ")'s ToRecipients isn't an array");
    }
    List<OutlookEmail> to = new ArrayList<OutlookEmail>(vTo.size());
    for (Iterator<JsonValue> toiter = vTo.getElements(); toiter.hasNext();) {
      to.add(readEmail(toiter.next()));
    }
    OutlookEmail from;
    JsonValue vFrom = vatt.getElement("From");
    if (isNull(vFrom)) {
      // TODO throw new OutlookFormatException("Message (" + messageId + " : " + subject + ") doesn't contain
      // From");
      // It's possible for saved draft messages
      from = null;
    } else {
      from = readEmail(vFrom);
    }
    JsonValue vCreatedDateTime = vatt.getElement("CreatedDateTime");
    if (isNull(vCreatedDateTime)) {
      throw new OutlookFormatException("Message (" + messageId + " : " + subject + ") doesn't contain CreatedDateTime");
    }
    Calendar created = Calendar.getInstance();
    try {
      created.setTime(OutlookMessage.DATE_FORMAT.parse(vCreatedDateTime.getStringValue()));
    } catch (ParseException e) {
      LOG.error("Error parsing message date " + vCreatedDateTime.getStringValue(), e);
    }
    JsonValue vLastModifiedDateTime = vatt.getElement("LastModifiedDateTime");
    if (isNull(vLastModifiedDateTime)) {
      throw new OutlookFormatException("Message (" + messageId + " : " + subject + ") doesn't contain LastModifiedDateTime");
    }
    Calendar modified = Calendar.getInstance();
    try {
      modified.setTime(OutlookMessage.DATE_FORMAT.parse(vLastModifiedDateTime.getStringValue()));
    } catch (ParseException e) {
      LOG.error("Error parsing message date " + vCreatedDateTime.getStringValue(), e);
    }
    JsonValue vBody = vatt.getElement("Body");
    if (isNull(vBody)) {
      throw new OutlookFormatException("Message (" + messageId + " : " + subject + ") doesn't contain Body");
    }
    JsonValue vContentType = vBody.getElement("ContentType");
    if (isNull(vContentType)) {
      throw new OutlookFormatException("Message (" + messageId + " : " + subject + ")'s body doesn't contain ContentType");
    }
    String contentType = vContentType.getStringValue();
    JsonValue vContent = vBody.getElement("Content");
    if (isNull(vContent)) {
      throw new OutlookFormatException("Message (" + messageId + " : " + subject + ")'s body doesn't contain Content");
    }
    String content = vContent.getStringValue();
    // TODO if contentType is HTML do sanitize the HTML

    //
    OutlookMessage message = new OutlookMessage(user);
    message.setId(messageId);
    message.setFrom(from);
    message.setTo(to);
    message.setSubject(subject);
    message.setBody(content);
    message.setType(contentType);
    message.setCreated(created);
    message.setModified(modified);
    return message;
  }

  /**
   * On-start initializer.
   */
  @Override
  public void start() {
    try {
      this.trashHomePath = trashService.getTrashHomeNode().getPath();
    } catch (RepositoryException e) {
      LOG.warn("Error getting Trash home node", e);
      this.trashHomePath = "/Trash";
    }
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

  /**
   * {@inheritDoc}
   */
  @Override
  public UserDocuments getUserDocuments() throws RepositoryException, OutlookException {
    // get Personal Documents folder
    String userName = currentUserId();
    try {
      Node userDocsNode = userDocumentsNode(userName);
      PersonalDocumentsFolder folder = new PersonalDocumentsFolder(userDocsNode);
      initDocumentLink(folder, folder);
      return folder;
    } catch (Exception e) {
      throw new OutlookException("Error reading user's Personal Documents node for " + userName, e);
    }
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
    return new StringBuilder().append(workspace).append("/").append(path).toString();
  }

  protected org.exoplatform.services.organization.User getExoUser(String userName) throws OutlookException {
    try {
      return organization.getUserHandler().findUserByName(userName);
    } catch (Exception e) {
      throw new OutlookException("Error searching user " + userName, e);
    }
  }

  protected Node node(String nodePath) throws BadParameterException, RepositoryException {
    String workspace, path;
    if (nodePath.startsWith("/")) {
      workspace = jcrService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
      path = nodePath;
    } else {
      // TODO it's not used experimental thing, see also ContentLink component
      int i = nodePath.indexOf('/');
      if (i > 0) {
        workspace = nodePath.substring(0, i);
        path = nodePath.substring(i);
      } else {
        throw new BadParameterException("Invalid path " + nodePath);
      }
    }
    return node(workspace, path);
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
  protected Node addFile(Node parent, String title, String contentType, InputStream content) throws RepositoryException {
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
    // file.setProperty("exo:summary", summary);
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

  protected void initWebDAVLink(HierarchyNode node) throws OutlookException {
    // WebDAV URL
    try {
      node.setWebdavUrl(org.exoplatform.wcm.webui.Utils.getWebdavURL(node.getNode(), false, true));
    } catch (Exception e) {
      throw new OutlookException("Error generating WebDav URL for node " + node.getPath(), e);
    }
  }

  protected void initDocumentLink(SiteType siteType,
                                  String driveName,
                                  String portalName,
                                  String nodeURI,
                                  HierarchyNode node) throws OutlookException {
    // WebDAV URL
    initWebDAVLink(node);

    // Portal URL
    // Code adapted from ECMS's PermlinkActionComponent.getPermlink()

    String npath = node.getPath().replaceAll("/+", "/");

    String path = new StringBuilder().append(driveName).append(npath).toString();
    PortalRequestContext portalRequest = Util.getPortalRequestContext();
    if (portalRequest != null) {
      NodeURL nodeURL = portalRequest.createURL(NodeURL.TYPE);
      NavigationResource resource = new NavigationResource(siteType, portalName, nodeURI);
      nodeURL.setResource(resource);
      nodeURL.setQueryParameterValue("path", path);

      HttpServletRequest request = portalRequest.getRequest();
      try {
        URI requestUri = new URI(request.getScheme(),
                                 null,
                                 request.getServerName(),
                                 request.getServerPort(),
                                 null,
                                 null,
                                 null);

        StringBuilder url = new StringBuilder();
        url.append(requestUri.toASCIIString());
        url.append(nodeURL.toString());
        node.setUrl(url.toString());
      } catch (URISyntaxException e) {
        throw new OutlookException("Error creating server URL " + request.getRequestURI().toString(), e);
      }
    } else {
      LOG.warn("Portal request not found. Node URL will be its WebDAV link. Node: " + node.getPath());
      node.setUrl(node.getWebdavUrl());
    }
  }

  protected void initDocumentLink(OutlookSpace space, HierarchyNode file) throws OutlookException {
    // WebDAV URL
    initWebDAVLink(file);

    // Portal URL
    // We need like the following:
    // https://peter.exoplatform.com.ua:8443/portal/g/:spaces:product_team/product_team/documents?path=.spaces.product_team/Groups/spaces/product_team/Documents/uploads/page_management_https_loading.png
    initDocumentLink(SiteType.GROUP, // GROUP
                     space.getGroupId().replace("/", "."),
                     space.getGroupId(), // /spaces/product_team
                     space.getShortName() + "/documents", // product_team/documents
                     file);

    // TODO cleanup
    // StringBuilder url = new StringBuilder();
    //
    // String groupDriveName = space.getGroupId().replace("/", ".");
    // String npath = node.getPath().replaceAll("/+", "/");
    //
    // String path = new StringBuilder().append(groupDriveName).append(npath).toString();
    // PortalRequestContext portalRequest = Util.getPortalRequestContext();
    // if (portalRequest != null) {
    // NodeURL nodeURL = portalRequest.createURL(NodeURL.TYPE);
    // NavigationResource resource = new NavigationResource(SiteType.GROUP, // GROUP
    // space.getGroupId(), // /spaces/product_team
    // space.getShortName() + "/documents"); // product_team/documents
    // nodeURL.setResource(resource);
    // nodeURL.setQueryParameterValue("path", path);
    //
    // HttpServletRequest request = portalRequest.getRequest();
    // try {
    // URI requestUri = new URI(request.getScheme(),
    // null,
    // request.getServerName(),
    // request.getServerPort(),
    // null,
    // null,
    // null);
    // url.append(requestUri.toASCIIString());
    // url.append(nodeURL.toString());
    //
    // node.setUrl(url.toString());
    // } catch (URISyntaxException e) {
    // throw new OutlookException("Error creating server URL " + request.getRequestURI().toString(), e);
    // }
    // } else {
    // LOG.warn("Portal request not found. Node URL will be its WebDAV link. Node: " + node.getPath());
    // node.setUrl(node.getWebdavUrl());
    // }
  }

  protected void initDocumentLink(PersonalDocumentsFolder personalDocuments, HierarchyNode file) throws OutlookException {
    // WebDAV URL
    initWebDAVLink(file);

    // Portal URL
    // We need like the following:
    // https://peter.exoplatform.com.ua:8443/portal/intranet/documents?path=Personal%20Documents/Users/j___/jo___/joh___/john/Private/Documents
    initDocumentLink(SiteType.PORTAL, // PORTAL
                     personalDocuments.getDriveName(),
                     "intranet", // intranet
                     "documents", // documents
                     file);

    // TODO cleanup
    // StringBuilder url = new StringBuilder();
    //
    // String nodePath = node.getPath().replaceAll("/+", "/");
    //
    // String path = new StringBuilder().append(personalDocuments.getDriveName()).append(nodePath).toString();
    // PortalRequestContext portalRequest = Util.getPortalRequestContext();
    // if (portalRequest != null) {
    // NodeURL nodeURL = portalRequest.createURL(NodeURL.TYPE);
    // NavigationResource resource = new NavigationResource(SiteType.PORTAL, // PORTAL
    // "intranet", // intranet
    // "documents"); // documents
    // nodeURL.setResource(resource);
    // nodeURL.setQueryParameterValue("path", path);
    //
    // HttpServletRequest request = portalRequest.getRequest();
    // try {
    // URI requestUri = new URI(request.getScheme(),
    // null,
    // request.getServerName(),
    // request.getServerPort(),
    // null,
    // null,
    // null);
    // url.append(requestUri.toASCIIString());
    // url.append(nodeURL.toString());
    //
    // node.setUrl(url.toString());
    // } catch (URISyntaxException e) {
    // throw new OutlookException("Error creating server URL " + request.getRequestURI().toString(), e);
    // }
    // } else {
    // LOG.warn("Portal request not found. Node URL will be its WebDAV link. Node: " + node.getPath());
    // node.setUrl(node.getWebdavUrl());
    // }
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
        return node(driveRootPath);
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
    String safeTitle = safeText(message.getSubject());
    String safeContent = safeHtml(message.getBody());
    try (InputStream content = new ByteArrayInputStream(safeContent.getBytes("UTF-8"))) {
      // message file goes w/o summary, it will be generated in UI (OutlookMessageActivity)
      Node messageFile = addFile(parent, safeTitle, "text/html", content);
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

    StringBuilder filesLine = new StringBuilder();
    for (File f : files) {
      if (filesLine.length() > 0) {
        filesLine.append(',');
      }
      filesLine.append(OutlookAttachmentActivity.attachmentString(f.getNode().getUUID(), f.getTitle()));
    }

    activityParams.put(OutlookAttachmentActivity.FILES, filesLine.toString());
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
    String title = comment != null && comment.length() > 0 ? comment
                                                           : new StringBuilder("User ").append(author)
                                                                                       .append(" has saved ")
                                                                                       .append(files.size())
                                                                                       .append(files.size() > 1 ? " files"
                                                                                                                : " file")
                                                                                       .toString();
    ExoSocialActivity activity = new ExoSocialActivityImpl(authorIdentity.getId(),
                                                           OutlookAttachmentActivity.ACTIVITY_TYPE,
                                                           title,
                                                           null);
    activity.setTemplateParams(activityParams);

    // activity destination (user or space)
    ActivityManager activityManager = socialActivityManager();
    String spaceGroupName = getSpaceName(destFolder.getNode());
    Space space = spaceService().getSpaceByGroupId(SpaceUtils.SPACE_GROUP + "/" + spaceGroupName);
    if (spaceGroupName != null && spaceGroupName.length() > 0 && space != null) {
      // post activity to space stream
      Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), true);
      activityManager.saveActivityNoReturn(spaceIdentity, activity);
    } else {
      // post activity to user status stream
      activityManager.saveActivityNoReturn(authorIdentity, activity);
    }

    //
    activity = activityManager.getActivity(activity.getId());

    // TODO we don't add activity info nodetype to do not remove the activity when attachment file(s) removed
    // String activityId = activity.getId();
    // if (!StringUtils.isEmpty(activityId)) {
    // for (File f : files) {
    // Node n = f.getNode();
    // ActivityTypeUtils.attachActivityId(n, activityId);
    // n.save();
    // }
    // }

    return activity;
  }

  @Deprecated // TODO not used
  protected ExoSocialActivity postMessageActivity(OutlookMessage message) throws RepositoryException {
    Node node = message.getFileNode();
    if (node == null) {
      throw new IllegalArgumentException("Message node not set in '" + message + "'");
    }

    String author = message.getUser().getLocalUser();

    // FYI Code inspired by UIDocActivityComposer
    Map<String, String> activityParams = new LinkedHashMap<String, String>();

    activityParams.put(OutlookMessageActivity.FILE_UUID, node.getUUID());
    activityParams.put(OutlookMessageActivity.WORKSPACE, node.getSession().getWorkspace().getName());
    activityParams.put(OutlookMessageActivity.AUTHOR, author);

    Calendar activityDate = Calendar.getInstance();
    DateFormat dateFormatter = new SimpleDateFormat(ISO8601.SIMPLE_DATETIME_FORMAT);
    String dateString = dateFormatter.format(activityDate.getTime());
    activityParams.put(OutlookMessageActivity.DATE_CREATED, dateString);
    activityParams.put(OutlookMessageActivity.DATE_LAST_MODIFIED, dateString);

    //
    IdentityManager identityManager = socialIdentityManager();
    Identity authorIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, author, true);

    //
    ExoSocialActivity activity = new ExoSocialActivityImpl(authorIdentity.getId(),
                                                           OutlookMessageActivity.ACTIVITY_TYPE,
                                                           safeText(message.getSubject()),
                                                           null);
    activity.setTemplateParams(activityParams);

    //
    ActivityManager activityManager = socialActivityManager();
    activityManager.saveActivityNoReturn(authorIdentity, activity);

    activity = activityManager.getActivity(activity.getId());

    // TODO we don't add activity info nodetype to do not remove the activity when message file removed
    // String activityId = activity.getId();
    // if (!StringUtils.isEmpty(activityId)) {
    // ActivityTypeUtils.attachActivityId(node, activityId);
    // node.save();
    // }

    //
    return activity;
  }

  protected OutlookEmail readEmail(JsonValue vElem) throws OutlookException {
    JsonValue vEmailAddress = vElem.getElement("EmailAddress");
    if (isNull(vEmailAddress)) {
      throw new OutlookFormatException("Element doesn't contain EmailAddress");
    }
    String email;
    JsonValue vAddress = vEmailAddress.getElement("Address");
    if (isNull(vAddress)) {
      throw new OutlookFormatException("Element doesn't contain Address");
    } else {
      email = vAddress.getStringValue();
    }
    String name;
    JsonValue vName = vEmailAddress.getElement("Name");
    if (isNull(vName)) {
      name = "".intern();
    } else {
      name = vName.getStringValue();
    }
    return getAddress(email, name);
  }

  protected void fetchQuery(QueryResult qr, int limit, Set<File> res) throws RepositoryException, OutlookException {
    SpaceService spaceService = spaceService();
    for (NodeIterator niter = qr.getNodes(); niter.getPosition() < limit && niter.hasNext();) {
      Node node = niter.nextNode();
      String path = node.getPath();
      if (path.indexOf("/ApplicationData") < 0 && path.indexOf("exo:applications") < 0) {
        try {
          AccessControlList acl = ((ExtendedNode) node).getACL();
          String owner = acl.getOwner();
          if ("root".equals(owner)) {
            limit++;
            continue;
          }
        } catch (RepositoryException e) {
          // ignore it
          if (LOG.isDebugEnabled()) {
            LOG.debug("Error getting node ACL/owner", e);
          }
        }
        if (org.exoplatform.ecm.webui.utils.Utils.isInTrash(node)) {
          limit++;
        } else {
          // detect is it space and then check if space member
          Space space;
          if (path.startsWith(SPACES_HOME)) {
            try {
              String groupId = path.substring(7, path.indexOf("/", SPACES_HOME.length() + 1));
              space = spaceService.getSpaceByGroupId(groupId);
              if (space != null) {
                Set<String> allMemembers = new HashSet<String>();
                for (String s : space.getManagers()) {
                  allMemembers.add(s);
                }
                for (String s : space.getMembers()) {
                  allMemembers.add(s);
                }
                if (!allMemembers.contains(currentUserId())) {
                  // when not a space member - skip this file (but user still may be an owner of it!)
                  limit++;
                  continue;
                }
              }
            } catch (IndexOutOfBoundsException e) {
              // XXX something not clear with space path, will use portal page path as for Personal Documents
              // (it works well in PLF 4.3)
              space = null;
            }
          } else {
            space = null;
          }

          UserFile file = new UserFile(node.getParent().getPath(), node);
          if (space != null) {
            initDocumentLink(SiteType.GROUP, // GROUP
                             space.getGroupId().replace("/", "."),
                             space.getGroupId(), // /spaces/product_team
                             space.getShortName() + "/documents", // product_team/documents
                             file);
          } else {
            initDocumentLink(SiteType.PORTAL, // PORTAL
                             PERSONAL_DOCUMENTS,
                             "intranet", // intranet
                             "documents", // documents
                             file);
          }
          res.add(file);
        }
      } else {
        limit++;
      }
    }
  }

  /**
   * Method adapted from eXo Chat's WikiService.createOrEditPage().
   * 
   * @param message
   * @return
   * @throws Exception
   */
  protected Page createWikiPage(String wikiType,
                                String wikiOwner,
                                String creator,
                                String title,
                                String summary,
                                String content,
                                List<String> users) throws Exception {
    String parentTitle = OUTLOOK_MESSAGES_TITLE;
    String parentId = TitleResolver.getId(parentTitle, false);

    synchronized (wikiService) {

      Page parentPage = wikiService.getPageOfWikiByName(wikiType, wikiOwner, parentId);
      if (parentPage == null) {
        parentPage = new Page();
        parentPage.setTitle(parentTitle);
        parentPage.setContent("= " + parentTitle + " =\n");
        parentPage.setSyntax(Syntax.XWIKI_2_0.toIdString());
        Wiki wiki = wikiService.getWikiByTypeAndOwner(wikiType, wikiOwner);
        if (wiki == null) {
          wiki = wikiService.createWiki(wikiType, wikiOwner);
        }
        Page wikiHome = wiki.getWikiHome();
        setPermissionForWikiPage(Collections.<String> emptyList(), parentPage, wikiHome);
        List<PermissionEntry> permissions = parentPage.getPermissions();
        permissions.add(new PermissionEntry(WIKI_PERMISSION_ANY,
                                            "",
                                            IDType.USER,
                                            new Permission[] {
                                                new Permission(org.exoplatform.wiki.mow.api.PermissionType.VIEWPAGE,
                                                               true) }));
        parentPage.setPermissions(permissions);
        Wiki pwiki = new Wiki();
        pwiki.setOwner(wikiOwner);
        pwiki.setType(wikiType);
        parentPage = wikiService.createPage(pwiki, "WikiHome", parentPage);

        // TODO do we need some kind of restrictions for message pages?
        // remove permissions on the Meeting Notes parent page for current user (automatically added by
        // the Wiki API)
        // permissions = parentPage.getPermissions();
        // for (int i = 0; i < permissions.size(); i++) {
        // PermissionEntry permission = permissions.get(i);
        // if (creator.equals(permission.getId())) {
        // permissions.remove(i);
        // }
        // }
        // parentPage.setPermissions(permissions);
        // wikiService.updatePage(parentPage, null);
      }

      Wiki wiki = new Wiki();
      wiki.setOwner(wikiOwner);
      wiki.setType(wikiType);

      Page page = new Page();

      if (isHTML(content)) {
        // TODO convert to xWiki format by RenderingService:
        // see in https://github.com/exodev/wiki/pull/78/files#diff-7971c1ca97b8cbdc2624f4a0f4a794baR136
        // markup = renderingService.render(htmlContent, Syntax.XHTML_1_0.toIdString(), syntaxId, false);

        // FYI XHTML doesn't work as when editing in eXo it does as for xWiki syntax
        // page.setSyntax(Syntax.XHTML_1_0.toIdString());
        page.setSyntax(Syntax.XWIKI_2_0.toIdString());
        // wrap message body as quoted into HTML Macro,
        // http://extensions.xwiki.org/xwiki/bin/view/Extension/HTML+Macro
        StringBuilder wikiContent = new StringBuilder();
        wikiContent.append("{{html wiki=\"false\"}}");
        if (summary != null) {
          // HTML also contains message summary (US_003_07)
          wikiContent.append("<div style='word-wrap: break-word; min-height: 30px;'>");
          wikiContent.append(summary);
          wikiContent.append("</div>");
        }
        wikiContent.append("<div style='overflow:auto;'><div style='position: relative; float: left;"
            + "box-sizing: border-box; padding-left: 7px; min-width: 100%; max-height: 100%;"
            + "border-width: 0px 0px 0px 12px; border-style: solid; border-color: #999999; background-color: white;'>");
        wikiContent.append(safeHtml(content));
        wikiContent.append("</div></div>");
        wikiContent.append("{{/html}}");
        page.setContent(wikiContent.toString());
      } else {
        page.setSyntax(Syntax.XWIKI_2_0.toIdString());
        page.setContent(content);
      }

      setPermissionForWikiPage(users, page, parentPage);
      page.setOwner(creator);
      page.setAuthor(creator);
      page.setMinorEdit(false);

      ///
      title = safeText(title);
      String baseTitle = title;

      int siblingNumber = 0;
      do {
        String pageId = TitleResolver.getId(title, false);
        page.setTitle(title);
        String path = "";
        if (wikiType.equals(PortalConfig.GROUP_TYPE)) {
          // http://demo.exoplatform.net/portal/intranet/wiki/group/spaces/bank_project/Meeting_06-11-2013
          path = "/portal/intranet/wiki/" + wikiType + wikiOwner + "/" + pageId;
        } else if (wikiType.equals(PortalConfig.PORTAL_TYPE)) {
          // http://demo.exoplatform.net/portal/intranet/wiki/Sales_Meetings_Meeting_06-11-2013
          path = "/portal/intranet/wiki/" + pageId;
        }
        page.setUrl(path);

        if (!wikiService.isExisting(wikiType, wikiOwner, pageId)) {
          try {
            page = wikiService.createPage(wiki, parentId, page);
            break;
          } catch (WikiException e) {
            LOG.warn("Error creating wiki page " + title + " (" + pageId + "). " + e.getMessage());
            try {
              wikiService.getPageById(pageId);
            } catch (WikiException ge) {
              // if we caught error here - we thrown a first one of the creation
              throw e;
            }
          }
        }

        // such page already exists - find new name for it (by adding sibling index to the end)
        siblingNumber++;
        title = new StringBuilder(baseTitle).append(" (").append(siblingNumber).append(')').toString();
      } while (true);

      return page;
    }
  }

  /**
   * Generate message summary text.
   * 
   * @param message
   * @return
   */
  protected String messageSummary(OutlookMessage message) {
    String fromEmail = message.getFrom().getEmail();
    String fromName = message.getFrom().getDisplayName();
    Date time = message.getCreated().getTime();

    Locale locale = Locale.ENGLISH;
    ResourceBundle res = resourceBundleService.getResourceBundle("locale.outlook.Outlook", locale);

    DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL, locale);
    DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, locale);

    StringBuilder fromLine = new StringBuilder();
    fromLine.append(fromName);
    fromLine.append('<');
    fromLine.append(fromEmail);
    fromLine.append('>');

    StringBuilder summary = new StringBuilder();
    summary.append(res.getString("Outlook.activity.from"));
    summary.append(": <a href='mailto:");
    summary.append(fromEmail);
    summary.append("' target='_top'>");
    summary.append(ContentReader.simpleEscapeHtml(fromLine.toString()));
    summary.append("</a> ");
    summary.append(res.getString("Outlook.activity.on"));
    summary.append(' ');
    summary.append(dateFormat.format(time));
    summary.append(' ');
    summary.append(res.getString("Outlook.activity.at"));
    summary.append(' ');
    summary.append(timeFormat.format(time));

    return summary.toString();
  }

  /**
   * Try detect is it a HTML content in the string.
   * 
   * @param content
   * @return
   */
  protected boolean isHTML(String content) {
    // XXX well, it's not proper, but working in most of cases approach

    int istart = content.indexOf("<html");
    int iend = content.indexOf("</html>");
    if (istart >= 0 && iend > 0 && istart < iend) {
      return true;
    }

    istart = content.indexOf("<body");
    iend = content.indexOf("</body>");
    if (istart >= 0 && iend > 0 && istart < iend) {
      return true;
    }

    istart = content.indexOf("<div");
    iend = content.indexOf("</div>");
    if (istart >= 0 && iend > 0 && istart < iend) {
      return true;
    }

    // FYI it's how looks message after MS Word pre-peocessor in Outlook for Windows:
    // everything in tables (no html, body or divs)
    istart = content.indexOf("<table");
    iend = content.indexOf("</table>");
    if (istart >= 0 && iend > 0 && istart < iend) {
      return true;
    }

    istart = content.indexOf("<style");
    iend = content.indexOf("</style>");
    if (istart >= 0 && iend > 0 && istart < iend) {
      return true;
    }

    return false;
  }

  /**
   * Allow full rage of HTML text and structures.
   * 
   * @param content
   * @return sanitized content
   */
  protected String safeHtml(String content) {
    String safe = htmlPolicy.sanitize(content);
    safe = makeLinksOpenNewWindow(safe);
    return safe;
  }

  /**
   * Allow only plain text.
   * 
   * @param content
   * @return sanitized content (as plain text)
   */
  protected String safeText(String content) {
    String safe = textPolicy.sanitize(content);
    safe = makeLinksOpenNewWindow(safe);
    safe = StringEscapeUtils.unescapeHtml(safe);
    return safe;
  }

  /**
   * Allow only activity tags as described in
   * https://www.exoplatform.com/docs/PLF43/PLFUserGuide.GettingStarted.ActivitiesInActivityStream.HTMLTags.
   * html.
   * 
   * @param content
   * @return allowed content
   */
  protected String safeActivityMessage(String text) {
    String safe = activityPolicy.sanitize(text);
    safe = makeLinksOpenNewWindow(safe);
    safe = StringEscapeUtils.unescapeHtml(safe);
    return safe;
  }

  protected String makeLinksOpenNewWindow(String text) {
    // Make all links target a new window
    // Replace in all links with target attribute to its _blank value
    Matcher m = linkWithTarget.matcher(text);
    StringBuilder sb = new StringBuilder();
    int pos = 0;
    while (m.find()) {
      int start = m.start(1);
      int end = m.end(1);
      if (start >= 0 && end >= 0) {
        // sb.replace(m.start(1), m.end(1), "target=\"_blank\"");
        sb.append(text.substring(pos, start));
        sb.append("target=\"_blank\"");
        pos = end;
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Cannot find link target group in " + m.group(1));
        }
      }
    }
    if (pos < text.length()) {
      sb.append(text.substring(pos));
    }
    text = sb.toString();

    // Add in all links without target attribute add it with _blank value
    m = linkWithoutTarget.matcher(text);
    sb = new StringBuilder();
    pos = 0;
    while (m.find()) {
      int start = m.start(2);
      int end = m.end(2);
      if (start >= 0 && end >= 0) {// sb.toString().substring(start - 5, start + 100);
        // sb.insert(start, " target=\"_blank\"");
        sb.append(text.substring(pos, start));
        sb.append(" target=\"_blank\"");
        sb.append(text.substring(start, end));
        pos = end;
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Cannot find link end group in " + m.group(2));
        }
      }
    }
    if (pos < text.length()) {
      sb.append(text.substring(pos));
    }
    return sb.toString();
  }

  /**
   * Method adapted from eXo Chat's WikiService.setPermissionForReportAsWiki().
   * 
   * @param users
   * @param page
   * @param parentPage
   */
  protected void setPermissionForWikiPage(List<String> users, Page page, Page parentPage) {
    Permission[] allPermissions = new Permission[] {
        new Permission(org.exoplatform.wiki.mow.api.PermissionType.VIEWPAGE, true),
        new Permission(org.exoplatform.wiki.mow.api.PermissionType.EDITPAGE, true), };
    List<PermissionEntry> permissions = parentPage.getPermissions();
    if (permissions != null) {
      // remove any permission
      int anyIndex = -1;
      for (int i = 0; i < permissions.size(); i++) {
        PermissionEntry any = permissions.get(i);
        if (WIKI_PERMISSION_ANY.equals(any.getId()))
          anyIndex = i;
      }
      if (anyIndex > -1) {
        permissions.remove(anyIndex);
      }
      for (int i = 0; i < users.size(); i++) {
        String strUser = users.get(i).toString();
        PermissionEntry userPermission = new PermissionEntry(strUser, strUser, IDType.USER, allPermissions);
        permissions.add(userPermission);
      }
      page.setPermissions(permissions);
    }
  }

  /**
   * Method inspired by code of UIPostForm.
   * 
   * @param message
   * @return
   * @throws Exception
   */
  protected Topic createForumTopic(String categoryId,
                                   String forumId,
                                   String creator,
                                   String title,
                                   String summary,
                                   String content) throws Exception {
    // save topic in ForumService
    org.exoplatform.forum.service.UserProfile userProfile = forumService.getUserSettingProfile(creator);
    if (checkForumHasAddTopic(userProfile, categoryId, forumId)) {
      Topic topic = new Topic();

      String message;
      if (isHTML(content)) {
        message = safeHtml(content);
      } else {
        message = content;
      }

      String safeTitle = safeText(title);
      // check if title not empty
      if (safeTitle.length() <= 0 || safeTitle.equals("null")) {
        if (summary != null) {
          safeTitle = safeText(summary);
        } else {
          safeTitle = safeText(message.substring(0, ForumUtils.MAXTITLE - 5) + "...");
        }
      }
      if (safeTitle.length() > ForumUtils.MAXTITLE) {
        // TODO throw an exception to user to ask for shorten title
        safeTitle = new StringBuilder(safeTitle.substring(0, ForumUtils.MAXTITLE - 3)).append("...").toString();
      }

      String checksms = TransformHTML.cleanHtmlCode(message,
                                                    new ArrayList<String>((new ExtendedBBCodeProvider()).getSupportedBBCodes()));
      checksms = checksms.replaceAll("&nbsp;", " ");
      int t = checksms.trim().length();
      if (t > 0 && !checksms.equals("null")) {
        // TODO
      }
      Date currentDate = CommonUtils.getGreenwichMeanTime().getTime();
      message = CommonUtils.encodeSpecialCharInSearchTerm(message);
      message = TransformHTML.fixAddBBcodeAction(message);
      // TODO do we need this when using safe HTML?
      message = message.replaceAll("<script", "&lt;script").replaceAll("<link", "&lt;link").replaceAll("</script>",
                                                                                                       "&lt;/script>");
      // remove any meta tags explicitly existing in the content
      message = message.replaceAll("<meta.*?>", "");
      // remove all embedded global styles
      message = message.replaceAll("<style.*?>[.\\s\\w\\W]*?<\\/style>", "");

      boolean isOffend = false;
      ForumAdministration forumAdministration = forumService.getForumAdministration();
      String[] censoredKeyword = ForumUtils.getCensoredKeyword(forumAdministration.getCensoredKeyword());
      checksms = checksms.toLowerCase();
      for (String string : censoredKeyword) {
        if (checksms.indexOf(string.trim()) >= 0) {
          isOffend = true;
          break;
        }
        if (safeTitle.toLowerCase().indexOf(string.trim()) >= 0) {
          isOffend = true;
          break;
        }
      }

      boolean topicClosed = false; // uiForm.getUIForumCheckBoxInput(FIELD_TOPICSTATE_SELECTBOX).isChecked();
      boolean topicLocked = false; // uiForm.getUIForumCheckBoxInput(FIELD_TOPICSTATUS_SELECTBOX).isChecked();
      boolean sticky = false; // uiForm.getUIForumCheckBoxInput(FIELD_STICKY_CHECKBOX).isChecked();
      boolean moderatePost = true; // uiForm.getUIForumCheckBoxInput(FIELD_MODERATEPOST_CHECKBOX).isChecked();
      boolean whenNewPost = true; // uiForm.getUIForumCheckBoxInput(FIELD_NOTIFYWHENADDPOST_CHECKBOX).isChecked();

      // TODO permissions?
      // UIPermissionPanel permissionTab = uiForm.getChildById(PERMISSION_TAB);
      String canPost = ForumUtils.EMPTY_STR; // permissionTab.getOwnersByPermission(CANPOST);
      String canView = ForumUtils.EMPTY_STR; // permissionTab.getOwnersByPermission(CANVIEW);

      // set link
      // FYI this origjnal Forum code will use current "outlook" portlet path to build the link
      // ForumUtils.createdForumLink(ForumUtils.TOPIC, topic.getId(), false)
      String link = BuildLinkUtils.buildLink(forumId, topic.getId(), PORTLET_INFO.FORUM);
      // finally escape the title
      safeTitle = CommonUtils.encodeSpecialCharInTitle(safeTitle);
      // TODO is it still required as we've removed scripts and used HTML sanitizer already?
      safeTitle = StringCommonUtils.encodeScriptMarkup(safeTitle);
      topic.setTopicName(safeTitle);
      topic.setModifiedBy(creator);
      topic.setModifiedDate(currentDate);
      // TODO do we need this? encode XSS script
      message = StringCommonUtils.encodeScriptMarkup(message);

      if (summary != null) {
        // if summary given then we assume need quote the message content
        StringBuilder topicContent = new StringBuilder();
        if (summary != null) {
          // HTML also contains message summary
          topicContent.append("<div class='messageSummary' style='word-wrap: break-word; min-height: 30px;'>");
          topicContent.append(summary);
          topicContent.append("</div>");
        }
        topicContent.append("<div class='messageQuote' style='overflow:auto;'><div class='messageContent' style='position: relative; float: left;"
            + "box-sizing: border-box; padding-left: 7px; min-width: 100%; max-height: 100%;"
            + "border-width: 0px 0px 0px 12px; border-style: solid; border-color: #999999; background-color: white;'>");
        topicContent.append(message);
        topicContent.append("</div></div>");
        message = topicContent.toString();
      } // otherwise message content will be a content of the topic post

      topic.setDescription(message);
      topic.setLink(link);
      if (whenNewPost) {
        String email = userProfile.getEmail();
        if (email == null || email.length() <= 0) {
          try {
            email = organization.getUserHandler().findUserByName(creator).getEmail();
          } catch (Exception e) {
            email = "true";
          }
        }
        topic.setIsNotifyWhenAddPost(email);
      } else {
        topic.setIsNotifyWhenAddPost(ForumUtils.EMPTY_STR);
      }
      // topicNew.setAttachments(uiForm.attachments_);
      topic.setIsWaiting(isOffend);
      topic.setIsClosed(topicClosed);
      topic.setIsLock(topicLocked);
      topic.setIsModeratePost(moderatePost);
      topic.setIsSticky(sticky);

      topic.setIcon("uiIconForumTopic uiIconForumLightGray");
      String[] canPosts = ForumUtils.splitForForum(canPost);
      String[] canViews = ForumUtils.splitForForum(canView);

      topic.setCanView(canViews);
      topic.setCanPost(canPosts);
      topic.setIsApproved(true); // !hasForumMod
      // XXX we cannot rely on ForumUtils.getDefaultMail() because it requires resources from Forum WAR
      // MessageBuilder messageBuilder = ForumUtils.getDefaultMail();
      MessageBuilder messageBuilder = new MessageBuilder();
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      ResourceBundle res = resourceBundleService.getResourceBundle("locale.portlet.forum.ForumPortlet", context.getLocale());
      if (res != null) {
        // TODO this will not work as resources aren't reachable here - it's DEAD CODE in fact
        try {
          messageBuilder.setContent(res.getString("UINotificationForm.label.notifyEmailContentDefault"));
          String header = res.getString("UINotificationForm.label.notifyEmailHeaderSubjectDefault");
          messageBuilder.setHeaderSubject(header == null || header.trim().length() == 0 ? ForumUtils.EMPTY_STR : header);
          messageBuilder.setTypes(res.getString("UIForumPortlet.label.category"),
                                  res.getString("UIForumPortlet.label.forum"),
                                  res.getString("UIForumPortlet.label.topic"),
                                  res.getString("UIForumPortlet.label.post"));
        } catch (Exception e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Failed to get resource bundle for Forum default content email notification", e);
          }
        }
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Locale resource bundle cannot be found for Forum default content email notification");
        }
      }

      messageBuilder.setLink(link);

      topic.setOwner(creator);
      topic.setCreatedDate(currentDate);
      topic.setLastPostBy(creator);
      topic.setLastPostDate(currentDate);
      topic.setVoteRating(0.0);
      topic.setUserVoteRating(new String[] {});
      try {
        String remoteAddr = ForumUtils.EMPTY_STR;
        // TODO if (forumPortlet.isEnableIPLogging()) {
        remoteAddr = WebUIUtils.getRemoteIP();
        // }
        topic.setRemoteAddr(remoteAddr);
        forumService.saveTopic(categoryId, forumId, topic, true, false, messageBuilder);
        if (userProfile.getIsAutoWatchMyTopics()) {
          List<String> values = new ArrayList<String>();
          values.add(userProfile.getEmail());
          String path = new StringBuilder(categoryId).append(ForumUtils.SLASH)
                                                     .append(forumId)
                                                     .append(ForumUtils.SLASH)
                                                     .append(topic.getId())
                                                     .toString();
          forumService.addWatch(1, path, values, creator);
        }
      } catch (PathNotFoundException e) {
        throw new OutlookException("Error saving forum topic '" + title + "'", e);
      }
      return topic;
    } else {
      throw new BadParameterException("Cannot add forum topic. Check user permissions or forum settings.");
    }
  }

  /**
   * Adapted code from UIForumPortlet.
   * 
   * @param userProfile
   * @param categoryId
   * @param forumId
   * @return
   * @throws Exception
   */
  protected boolean checkForumHasAddTopic(org.exoplatform.forum.service.UserProfile userProfile,
                                          String categoryId,
                                          String forumId) throws Exception {
    // is guest or banned
    if (userProfile.getUserRole() == org.exoplatform.forum.service.UserProfile.GUEST || userProfile.getIsBanned()
        || userProfile.isDisabled()) {
      return false;
    }
    try {
      Category cate = forumService.getCategory(categoryId);
      Forum forum = forumService.getForum(categoryId, forumId);
      if (forum == null) {
        return false;
      }
      // forum close or lock
      if (forum.getIsClosed() || forum.getIsLock()) {
        return false;
      }
      // isAdmin
      if (userProfile.getUserRole() == 0) {
        return true;
      }
      // is moderator
      if (userProfile.getUserRole() == 1) {
        String[] morderators = ForumUtils.arraysMerge(cate.getModerators(), forum.getModerators());
        //
        if (ForumServiceUtils.isModerator(morderators, userProfile.getUserId())) {
          return true;
        }
      }
      // TODO ban IP of forum.
      // if (isEnableIPLogging() && forum.getBanIP() != null &&
      // forum.getBanIP().contains(WebUIUtils.getRemoteIP())) {
      // return false;
      // }
      // check access category
      if (!ForumServiceUtils.hasPermission(cate.getUserPrivate(), userProfile.getUserId())) {
        return false;
      }
      // can add topic on category/forum
      String[] canCreadTopic = ForumUtils.arraysMerge(forum.getCreateTopicRole(), cate.getCreateTopicRole());
      if (!ForumServiceUtils.hasPermission(canCreadTopic, userProfile.getUserId())) {
        return false;
      }
    } catch (Exception e) {
      LOG.warn(String.format("Check permission to add topic of category %s, forum %s unsuccessfully.", categoryId, forumId));
      if (LOG.isDebugEnabled()) {
        LOG.debug(e);
      }
      return false;
    }
    return true;
  }

  /**
   * get the space name of node
   * 
   * @param node
   * @return the group name
   * @throws RepositoryException
   * @throws Exception
   */
  private static String getSpaceName(Node node) throws RepositoryException {
    NodeHierarchyCreator nodeHierarchyCreator = (NodeHierarchyCreator) ExoContainerContext.getCurrentContainer()
                                                                                          .getComponentInstanceOfType(NodeHierarchyCreator.class);
    String groupPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
    String spacesFolder = groupPath + "/spaces/";
    String spaceName = "";
    String nodePath = node.getPath();
    if (nodePath.startsWith(spacesFolder)) {
      spaceName = nodePath.substring(spacesFolder.length());
      spaceName = spaceName.substring(0, spaceName.indexOf("/"));
    }

    return spaceName;
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
