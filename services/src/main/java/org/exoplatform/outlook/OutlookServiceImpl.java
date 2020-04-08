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
package org.exoplatform.outlook;

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
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.AccessDeniedException;
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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.entity.ContentType;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.picocontainer.Startable;

import com.ibm.icu.text.Transliterator;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.outlook.jcr.File;
import org.exoplatform.outlook.jcr.Folder;
import org.exoplatform.outlook.jcr.HierarchyNode;
import org.exoplatform.outlook.jcr.NodeFinder;
import org.exoplatform.outlook.jcr.UserDocuments;
import org.exoplatform.outlook.mail.MailAPI;
import org.exoplatform.outlook.mail.MailServerException;
import org.exoplatform.outlook.social.OutlookAttachmentActivity;
import org.exoplatform.outlook.social.OutlookMessageActivity;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.application.PeopleService;
import org.exoplatform.social.core.application.SpaceActivityPublisher;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.plugin.doc.UIDocActivity;
import org.exoplatform.social.webui.activity.UIDefaultActivity;
import org.exoplatform.wcm.ext.component.activity.FileUIActivity;
import org.exoplatform.wcm.webui.reader.ContentReader;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.ws.frameworks.json.value.JsonValue;

/**
 * Service implementing {@link OutlookService} and {@link Startable}.<br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: AnaplanServiceImpl.java 00000 Mar 3, 2016 pnedonosko $
 */
public class OutlookServiceImpl implements OutlookService, Startable {

  /** The Constant MAILSERVER_URL. */
  public static final String            MAILSERVER_URL         = "mailserver-url";

  /** The Constant EXO_PRIVILEGEABLE. */
  protected static final String         EXO_PRIVILEGEABLE      = "exo:privilegeable";

  /** The Constant OUTLOOK_MESSAGES_TITLE. */
  protected static final String         OUTLOOK_MESSAGES_TITLE = "Outlook Messages";

  /** The Constant OUTLOOK_MESSAGES_NAME. */
  protected static final String         OUTLOOK_MESSAGES_NAME  = "outlook-messages";

  /** The Constant UPLAODS_FOLDER_TITLE. */
  protected static final String         UPLAODS_FOLDER_TITLE   = "Uploads";

  /** The Constant SPACES_HOME. */
  protected static final String         SPACES_HOME            = "/Groups/spaces";

  /** The Constant ROOT_USER. */
  protected static final String         ROOT_USER              = "root";

  /** The Constant PERSONAL_DOCUMENTS. */
  protected static final String         PERSONAL_DOCUMENTS     = "Personal Documents";

  /** The Constant READER_PERMISSION. */
  protected static final String[]       READER_PERMISSION      = new String[] { PermissionType.READ };

  /** The Constant MANAGER_PERMISSION. */
  protected static final String[]       MANAGER_PERMISSION     = new String[] { PermissionType.READ, PermissionType.REMOVE };

  /** The Constant LOG. */
  protected static final Log            LOG                    = ExoLogger.getLogger(OutlookServiceImpl.class);

  /** The Constant RANDOM. */
  protected static final Random         RANDOM                 = new Random();

  /** The Constant accentsConverter. */
  protected static final Transliterator accentsConverter       =
                                                         Transliterator.getInstance("Latin; NFD; [:Nonspacing Mark:] Remove; NFC;");

  /**
   * The Class UserFolder.
   */
  protected class UserFolder extends Folder {

    /**
     * Instantiates a new user folder.
     *
     * @param parent the parent
     * @param node the node
     * @throws RepositoryException the repository exception
     * @throws OutlookException the outlook exception
     */
    protected UserFolder(Folder parent, Node node) throws RepositoryException, OutlookException {
      super(parent, node);
    }

    /**
     * Instantiates a new user folder.
     *
     * @param parentPath the parent path
     * @param node the node
     * @throws RepositoryException the repository exception
     * @throws OutlookException the outlook exception
     */
    protected UserFolder(String parentPath, Node node) throws RepositoryException, OutlookException {
      super(parentPath, node);
    }

    /**
     * Instantiates a new user folder.
     *
     * @param node the node
     * @throws RepositoryException the repository exception
     * @throws OutlookException the outlook exception
     */
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
      return folder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Folder newFolder(String rootPath, Node node) throws RepositoryException, OutlookException {
      Folder folder = new UserFolder(rootPath, node);
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

  /**
   * The Class PersonalDocuments.
   */
  protected class PersonalDocuments implements UserDocuments {

    /**
     * The Class PersonalFolder.
     */
    class PersonalFolder extends UserFolder {
    
      /**
       * Instantiates a new Personal Documents folder (root).
       *
       * @param node the root node of Personal Documents drive
       * @throws RepositoryException the repository exception
       * @throws OutlookException the outlook exception
       */
      protected PersonalFolder(Node node) throws RepositoryException, OutlookException {
        super(node);
      }
    
      /**
       * Instantiates a new personal folder.
       *
       * @param rootPath the root path
       * @param node the node
       * @throws RepositoryException the repository exception
       * @throws OutlookException the outlook exception
       */
      protected PersonalFolder(String rootPath, Node node) throws RepositoryException, OutlookException {
        super(rootPath, node);
      }
    
      /**
       * {@inheritDoc}
       */
      @Override
      protected void readChildNodes() throws RepositoryException, OutlookException {
        super.readChildNodes();
        for (Folder sf : this.subfolders.get()) {
          initDocumentLink(PersonalDocuments.this, sf);
        }
        for (File f : this.files.get()) {
          initDocumentLink(PersonalDocuments.this, f);
        }
      }
    }
    
    protected final Node driveNode;

    /**
     * Instantiates a new personal documents folder.
     *
     * @param node the node
     * @throws RepositoryException the repository exception
     * @throws OutlookException the outlook exception
     */
    protected PersonalDocuments(Node node) throws RepositoryException, OutlookException {
      this.driveNode = node;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PersonalFolder getRootFolder() throws OutlookException, RepositoryException {
      return new PersonalFolder(driveNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Folder getFolder(String path) throws OutlookException, RepositoryException {
      PersonalFolder theRoot = getRootFolder();
      String rootPath = theRoot.getPath();
      Folder folder;
      String folderPath = HierarchyNode.getPath(path);
      if (rootPath.equals(folderPath)) {
        folder = theRoot;
      } else if (folderPath.startsWith(rootPath)) {
        Node node = node(folderPath);
        folder = new PersonalFolder(node.getParent().getPath(), node);
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
      QueryManager qm = driveNode.getSession().getWorkspace().getQueryManager();

      Set<File> res = new LinkedHashSet<File>();

      if (text == null || text.length() == 0) {
        Query q = qm.createQuery(
                                 "SELECT * FROM nt:file WHERE exo:lastModifier='" + currentUserId()
                                     + "' ORDER BY exo:lastModifiedDate DESC, exo:title ASC",
                                 Query.SQL);
        final String currentUser = currentUserId();
        // fetch and filter nodes: not trashed and not system
        fetchQuery(q.execute(), 20, res, node -> {
          try {
            // XXX skip some known system locations/names
            String path = node.getPath();
            if (path.indexOf("/ApplicationData") >= 0 || path.indexOf("exo:applications") >= 0) {
              return false;
            }
            // skip trashed
            if (org.exoplatform.ecm.webui.utils.Utils.isInTrash(node)) {
              return false;
            }
            // access all nodes except of owned by root, but only if it is not
            // his session
            AccessControlList acl = ((ExtendedNode) node).getACL();
            String owner = acl.getOwner();
            if (currentUser.equals(owner)) {
              return true;
            } else {
              return !ROOT_USER.equals(owner);
            }
          } catch (RepositoryException e) {
            // ignore it
            if (LOG.isDebugEnabled()) {
              LOG.debug("Error getting ACL/owner of " + node, e);
            }
            return false;
          }
        });
      } else {
        String drivePath = driveNode.getPath();
        Query qOwn = qm.createQuery("SELECT * FROM nt:file WHERE exo:lastModifier='" + currentUserId() + "' AND jcr:path LIKE '"
            + drivePath + "/%' AND exo:title LIKE '%" + text + "%' ORDER BY exo:lastModifiedDate DESC, exo:title ASC", Query.SQL);
        // fetch first three modified by this user only
        fetchQuery(qOwn.execute(), 3, res);
        // and add all others up to total 20 files
        Query qOthers = qm.createQuery(
                                       "SELECT * FROM nt:file WHERE jcr:path LIKE '" + drivePath + "/%' AND exo:title LIKE '%"
                                           + text + "%' ORDER BY exo:lastModifiedDate DESC, exo:title ASC",
                                       Query.SQL);
        fetchQuery(qOthers.execute(), 20 - res.size(), res);
      }

      if (res.size() < 20) {
        // If have some space, then fetch latest from spaces
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM nt:file WHERE jcr:path LIKE '");
        sql.append(SPACES_HOME);
        sql.append("/%/Documents/%'");
        if (text != null && text.length() > 0) {
          sql.append(" AND exo:title LIKE '%");
          sql.append(text);
          sql.append("%'");
        }
        sql.append(" ORDER BY exo:lastModifiedDate DESC, exo:title ASC");
        Query qSpaces = qm.createQuery(sql.toString(), Query.SQL);
        fetchQuery(qSpaces.execute(), 20 - res.size(), res);
      }
      return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<File> findLastDocuments(String text) throws RepositoryException, OutlookException {
      QueryManager qm = driveNode.getSession().getWorkspace().getQueryManager();

      Set<File> res = new LinkedHashSet<File>();
      
      String drivePath = driveNode.getPath();
      
      // TODO this search will not include files from user's Public folder
      Query q;
      if (text == null || text.length() == 0) {
        q = qm.createQuery(
                           "SELECT * FROM nt:file WHERE jcr:path LIKE '" + drivePath
                               + "/%' ORDER BY exo:lastModifiedDate DESC, exo:title ASC",
                           Query.SQL);
      } else {
        q = qm.createQuery("SELECT * FROM nt:file WHERE jcr:path LIKE '" + drivePath + "/%' AND exo:title LIKE '%" + text + "%'",
                           Query.SQL);
      }
      fetchQuery(q.execute(), 20, res);
      return res;
    }

    /**
     * Gets the drive name.
     *
     * @return the drive name
     */
    protected String getDriveName() {
      // XXX we use what pointed in XML config
      return PERSONAL_DOCUMENTS;
    }
  }

  /**
   * The Class UserFile.
   */
  protected class UserFile extends File {

    /**
     * Instantiates a new user file.
     *
     * @param parent the parent
     * @param node the node
     * @throws RepositoryException the repository exception
     * @throws OutlookException the outlook exception
     */
    protected UserFile(Folder parent, Node node) throws RepositoryException, OutlookException {
      super(parent, node);
    }

    /**
     * Instantiates a new user file.
     *
     * @param parentPath the parent path
     * @param node the node
     * @throws RepositoryException the repository exception
     * @throws OutlookException the outlook exception
     */
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

  /**
   * The Class UserImpl.
   */
  protected class UserImpl extends OutlookUser {

    /** The social identity manager. */
    protected final IdentityManager     socialIdentityManager;

    /** The social activity manager. */
    protected final ActivityManager     socialActivityManager;

    /** The social relationship manager. */
    protected final RelationshipManager socialRelationshipManager;

    /**
     * Instantiates a new user by given email, display name and username.
     *
     * @param email the email, can be <code>null</code>
     * @param displayName the display name, can be <code>null</code>
     * @param userName the user name in eXo organization
     * @throws OutlookException if userName is not valid
     */
    protected UserImpl(String email, String displayName, String userName) throws OutlookException {
      super(email, displayName, userName);
      this.socialIdentityManager = socialIdentityManager();
      this.socialActivityManager = socialActivityManager();
      this.socialRelationshipManager = socialRelationshipManager();
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
          // Since upgrade to Platform 5.2 root of user folder has no user
          // permissions even for read.
          // Node userPublicFolder = userDocs.getParent().getNode("Public");
          Node sysPublicFolder = systemNode(userDocs.getSession().getWorkspace().getName(), userDocs.getPath() + "/Public");
          Node sysMessagesFolder = messagesFolder(sysPublicFolder, localUser, "member:/platform/users");
          Node sysMessageFile = addMessageFile(sysMessagesFolder, message);
          setPermissions(sysMessageFile, localUser, "member:/platform/users");
          sysMessagesFolder.save();
          // Take node under user session to use in the message and activity
          Node messageFile = node(sysMessageFile.getSession().getWorkspace().getName(), sysMessageFile.getPath());
          message.setFileNode(messageFile);

          String userMessage = message.getTitle();
          userMessage = userMessage != null && userMessage.length() > 0 ? safeText(userMessage) : null;

          final String origType = org.exoplatform.wcm.ext.component.activity.listener.Utils.getActivityType();
          try {
            org.exoplatform.wcm.ext.component.activity.listener.Utils.setActivityType(OutlookMessageActivity.ACTIVITY_TYPE);
            ExoSocialActivity activity = org.exoplatform.wcm.ext.component.activity.listener.Utils.postFileActivity(messageFile,
                                                                                                                    userMessage,
                                                                                                                    true,
                                                                                                                    false,
                                                                                                                    "",
                                                                                                                    "");
            // TODO should we care about activity removal with the message file?
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
      Identity userIdentity = socialIdentityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, currentUserId(), true);
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
      Identity userIdentity = socialIdentityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, currentUserId(), true);
      String safeText = safeActivityMessage(text);
      ExoSocialActivity activity = new ExoSocialActivityImpl(userIdentity.getId(), PeopleService.PEOPLE_APP_ID, safeText, null);
      // we do like done UIDefaultActivityComposer
      activity.setType(UIDefaultActivity.ACTIVITY_TYPE);

      socialActivityManager.saveActivityNoReturn(userIdentity, activity);
      activity.setPermanLink(LinkProvider.getSingleActivityUrl(activity.getId()));
      return activity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity getSocialIdentity() throws Exception {
      return socialIdentityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, getLocalUser(), true);
    }
  }

  /**
   * The Class OutlookSpaceImpl.
   */
  protected class OutlookSpaceImpl extends OutlookSpace {

    /**
     * The Class PersonalFolder.
     */
    class SpaceFolder extends UserFolder {

      /**
       * Instantiates a new space folder.
       *
       * @param parent the parent
       * @param node the node
       * @throws RepositoryException the repository exception
       * @throws OutlookException the outlook exception
       */
      protected SpaceFolder(Folder parent, Node node) throws RepositoryException, OutlookException {
        super(parent, node);
      }

      /**
       * Instantiates a new space folder.
       *
       * @param rootPath the root path
       * @param node the node
       * @throws RepositoryException the repository exception
       * @throws OutlookException the outlook exception
       */
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

    /**
     * The Class RootFolder.
     */
    class RootFolder extends SpaceFolder {

      /**
       * Instantiates a new root folder.
       *
       * @param rootPath the root path
       * @param node the node
       * @throws RepositoryException the repository exception
       * @throws OutlookException the outlook exception
       */
      protected RootFolder(String rootPath, Node node) throws RepositoryException, OutlookException {
        super(rootPath, node);
        initDocumentLink(OutlookSpaceImpl.this, this);
        hasSubfolders(); // force child reading to init default folder in
                         // readSubnodes()
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
          try {
            Node subfolderNode = addFolder(node, UPLAODS_FOLDER_TITLE, false);
            uploads = newFolder(this, subfolderNode);
            node.save();
            initDocumentLink(OutlookSpaceImpl.this, uploads);
            subfolders.add(uploads);
            defaultSubfolder = uploads;
          } catch (AccessDeniedException e) {
            if (LOG.isDebugEnabled()) {
              // gather some info about the user for the log
              String currentUserId = currentUserId();
              StringBuilder userInfo = new StringBuilder();
              userInfo.append(currentUserId);
              try {
                userInfo.append('[');
                for (Membership m : organization.getMembershipHandler().findMembershipsByUser(currentUserId())) {
                  userInfo.append(m.getMembershipType());
                  userInfo.append(':');
                  userInfo.append(m.getGroupId());
                  userInfo.append(' ');
                }
                userInfo.setCharAt(userInfo.length() - 1, ']');
              } catch (Exception oe) {
                LOG.warn("Error getting organization user " + currentUserId, e);
              }
              LOG.debug("Error creating " + UPLAODS_FOLDER_TITLE + " folder in " + getPath() + ". User: " + userInfo.toString()
                  + ". Parent node: " + node, e);
              // TODO we don't want throw Access error here, it should be thrown where actually will affect an user 
              // throw new AccessException("Access denied to " + OutlookSpaceImpl.this.getTitle(), e);
            }
            defaultSubfolder = null;
          }
        }
        return subfolders;
      }
    }

    /** The root path. */
    protected final String                  rootPath;

    /** The root folder. */
    protected final ThreadLocal<RootFolder> rootFolder = new ThreadLocal<RootFolder>();

    /** The social identity manager. */
    protected final IdentityManager         socialIdentityManager;

    /** The social activity manager. */
    protected final ActivityManager         socialActivityManager;

    /**
     * Instantiates a new outlook space impl.
     *
     * @param socialSpace the social space
     * @throws RepositoryException the repository exception
     * @throws OutlookException the outlook exception
     */
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
    public SpaceFolder getFolder(String path) throws OutlookException, RepositoryException {
      SpaceFolder parent = getRootFolder();
      SpaceFolder folder;
      String folderPath = HierarchyNode.getPath(path);
      if (rootPath.equals(folderPath)) {
        folder = parent;
      } else if (folderPath.startsWith(rootPath)) {
        folder = new SpaceFolder(parent, node(folderPath));
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
    public RootFolder getRootFolder() throws OutlookException, RepositoryException {
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
        Query qOwn = qm.createQuery(
                                    "SELECT * FROM nt:file WHERE exo:lastModifier='" + currentUserId() + "' AND jcr:path LIKE '"
                                        + root.getPath() + "/%' ORDER BY exo:lastModifiedDate DESC, exo:title ASC",
                                    Query.SQL);
        // fetch first three modified by this user only
        fetchQuery(qOwn.execute(), 3, res);
        // and add all others up to total 20 files
        Query qOthers = qm.createQuery(
                                       "SELECT * FROM nt:file WHERE jcr:path LIKE '" + root.getPath()
                                           + "/%' ORDER BY exo:lastModifiedDate DESC, exo:title ASC",
                                       Query.SQL);
        fetchQuery(qOthers.execute(), 20 - res.size(), res);
      } else {
        Query qOwn = qm.createQuery("SELECT * FROM nt:file WHERE exo:lastModifier='" + currentUserId() + "' AND jcr:path LIKE '"
            + root.getPath() + "/%' AND exo:title LIKE '%" + text + "%' ORDER BY exo:lastModifiedDate DESC, exo:title ASC",
                                    Query.SQL);
        // fetch first three modified by this user only
        fetchQuery(qOwn.execute(), 3, res);
        // and add all others up to total 20 files
        Query qOthers = qm.createQuery("SELECT * FROM nt:file WHERE jcr:path LIKE '" + root.getPath()
            + "/%' AND exo:title LIKE '%" + text + "%' ORDER BY exo:lastModifiedDate DESC, exo:title ASC", Query.SQL);
        fetchQuery(qOthers.execute(), 20 - res.size(), res);
      }
      return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExoSocialActivity postActivity(OutlookMessage message) throws Exception {
      Node spaceDocs = spaceDocumentsNode(groupId);
      Node messagesFolder = messagesFolder(spaceDocs, groupId);
      Node messageFile = addMessageFile(messagesFolder, message);
      setPermissions(messageFile, new StringBuilder("member:").append(groupId).toString());
      messagesFolder.save();
      message.setFileNode(messageFile);

      String userMessage = message.getTitle();
      userMessage = userMessage != null && userMessage.length() > 0 ? safeText(userMessage) : null;

      final String origType = org.exoplatform.wcm.ext.component.activity.listener.Utils.getActivityType();
      try {
        org.exoplatform.wcm.ext.component.activity.listener.Utils.setActivityType(OutlookMessageActivity.ACTIVITY_TYPE);
        ExoSocialActivity activity = org.exoplatform.wcm.ext.component.activity.listener.Utils.postFileActivity(messageFile,
                                                                                                                userMessage,
                                                                                                                true,
                                                                                                                false,
                                                                                                                "",
                                                                                                                "");
        // TODO should we care about activity removal with the message file?
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
      Identity userIdentity = socialIdentityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, currentUserId(), true);
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
      Identity userIdentity = socialIdentityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, currentUserId(), true);
      String safeText = safeActivityMessage(text);
      ExoSocialActivity activity = new ExoSocialActivityImpl(userIdentity.getId(),
                                                             SpaceActivityPublisher.SPACE_APP_ID,
                                                             safeText,
                                                             null);
      // we do like done UIDefaultActivityComposer
      activity.setType(UIDefaultActivity.ACTIVITY_TYPE);

      socialActivityManager.saveActivityNoReturn(spaceIdentity, activity);
      activity.setPermanLink(LinkProvider.getSingleActivityUrl(activity.getId()));
      return activity;
    }

  }

  /** The jcr service. */
  protected final RepositoryService                           jcrService;

  /** The session providers. */
  protected final SessionProviderService                      sessionProviders;

  /** The finder. */
  protected final NodeFinder                                  finder;

  /** The hierarchy creator. */
  protected final NodeHierarchyCreator                        hierarchyCreator;

  /** The organization. */
  protected final OrganizationService                         organization;

  /** The drive service. */
  protected final ManageDriveService                          driveService;

  /** The listener service. */
  protected final ListenerService                             listenerService;

  /** The trash service. */
  protected final TrashService                                trashService;

  /** The resource bundle service. */
  protected final ResourceBundleService                       resourceBundleService;

  /** The resource document service. */
  protected final DocumentService                             documentService;

  /** The html policy. */
  protected final PolicyFactory                               htmlPolicy        =
                                                                         Sanitizers.BLOCKS.and(Sanitizers.FORMATTING)
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
                                                                                          // to
                                                                                          // use
                                                                                          // them
                                                                                          // for
                                                                                          // HTML
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

  /** The text policy. */
  protected final PolicyFactory                               textPolicy        = new HtmlPolicyBuilder().toFactory();

  /**
   * Custom policy to allow supported elements in activity text as described in
   * <a href=
   * "https://www.exoplatform.com/docs/PLF43/PLFUserGuide.GettingStarted.ActivitiesInActivityStream.HTMLTags.html">
   * Platform User Guide</a>
   */
  protected final PolicyFactory                               activityPolicy    =
                                                                             new HtmlPolicyBuilder().allowUrlProtocols("http",
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
                                                                                                    .matching(true, "_blank")
                                                                                                    .onElements("a")
                                                                                                    .allowAttributes("alt", "src")
                                                                                                    .onElements("img")
                                                                                                    .toFactory();

  /** The link with href not a hash in local document target. */
  protected final Pattern                                     linkNotLocal      =
                                                                           Pattern.compile("href=['\"][^#][.\\w\\W\\S]*?['\"]",
                                                                                           Pattern.CASE_INSENSITIVE
                                                                                               | Pattern.MULTILINE
                                                                                               | Pattern.DOTALL);

  /** The link with target. */
  protected final Pattern                                     linkWithTarget    =
                                                                             Pattern.compile("<a(?=\\s).*?(target=['\"].*?['\"])[^>]*>",
                                                                                             Pattern.CASE_INSENSITIVE
                                                                                                 | Pattern.MULTILINE
                                                                                                 | Pattern.DOTALL);

  /** The link without target. */
  protected final Pattern                                     linkWithoutTarget =
                                                                                Pattern.compile("<a(?=\\s)(?:(?!target=).)*?([.\\W\\w\\S\\s[^>]])*?(>)",
                                                                                                Pattern.CASE_INSENSITIVE
                                                                                                    | Pattern.MULTILINE
                                                                                                    | Pattern.DOTALL);

  /**
   * Spaces cache. TODO There is an issue with threads when different requests
   * reuse them. Space's root node may be already invalid. See also in
   * getRootFolder().
   */
  protected final ConcurrentHashMap<String, OutlookSpaceImpl> spaces            =
                                                                     new ConcurrentHashMap<String, OutlookSpaceImpl>();

  /** The mailserver api. */
  protected MailAPI                                           mailserverApi;

  /** The trash home path. */
  protected String                                            trashHomePath;

  /**
   * Outlook service with storage in JCR and with managed features.
   *
   * @param jcrService {@link RepositoryService}
   * @param sessionProviders {@link SessionProviderService}
   * @param hierarchyCreator {@link NodeHierarchyCreator}
   * @param finder {@link NodeFinder}
   * @param organization {@link OrganizationService}
   * @param listenerService {@link ListenerService}
   * @param driveService {@link ManageDriveService}
   * @param trashService {@link TrashService}
   * @param resourceBundleService {@link ResourceBundleService}
   * @param params {@link InitParams}
   * @throws ConfigurationException when parameters configuration error
   * @throws MailServerException when Mail server API error
   */
  public OutlookServiceImpl(RepositoryService jcrService,
                            SessionProviderService sessionProviders,
                            NodeHierarchyCreator hierarchyCreator,
                            NodeFinder finder,
                            OrganizationService organization,
                            ListenerService listenerService,
                            ManageDriveService driveService,
                            TrashService trashService,
                            ResourceBundleService resourceBundleService,
                            InitParams params,
                            DocumentService documentService)
      throws ConfigurationException,
      MailServerException {

    this.jcrService = jcrService;
    this.sessionProviders = sessionProviders;
    this.hierarchyCreator = hierarchyCreator;
    this.finder = finder;
    this.organization = organization;
    this.driveService = driveService;
    this.listenerService = listenerService;
    this.trashService = trashService;
    this.resourceBundleService = resourceBundleService;
    this.documentService = documentService;

    // API for user requests (uses credentials from eXo user profile)
    MailAPI api = new MailAPI();
    this.mailserverApi = api;
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
      JsonValue vContentBytes = vatt.getElement("ContentBytes");
      if (isNull(vContentBytes)) {
        throw new OutlookFormatException("Attachment (" + name + ") doesn't contain ContentBytes");
      }
      // FYI attachment content in BASE64 (may be twice!)
      String contentBytes = vContentBytes.getStringValue();
      byte[] decoded = Base64.decodeBase64(contentBytes);

      // Save in JCR: content goes as-is here
      try (InputStream contentStream = new ByteArrayInputStream(decoded)) {
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

    // TODO care about activity removal with the message file
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
            String scheme = ewsUri.getScheme();
            int port = ewsUri.getPort();
            // TODO do we need remove obvious things?
            // if (port >= 0) {
            // if (port == 443 && "https".equalsIgnoreCase(scheme)) {
            // port = -1;
            // } else if (port == 80 && "http".equalsIgnoreCase(scheme)) {
            // port = -1;
            // }
            // }

            mailServerUrl = new URI(scheme, null, host, port, null, null, null);
          } catch (URISyntaxException e) {
            throw new MailServerException("Error parsing EWS API URL " + ewsUrl, e);
          }
        } else {
          mailServerUrl = null;
        }

        // new user instance
        UserImpl user = new UserImpl(email, displayName, exoUsername);
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
                                     String title,
                                     String subject,
                                     String body) throws OutlookException {
    OutlookMessage message = new OutlookMessage(user);
    message.setId(id);
    message.setFrom(from);
    message.setTo(to);
    message.setTitle(title);
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
      PersonalDocuments personalDocs = new PersonalDocuments(userDocsNode);
      return personalDocs;
    } catch (Exception e) {
      throw new OutlookException("Error reading user's Personal Documents node for " + userName, e);
    }
  }

  // *********************** testing level **********************

  /**
   * Sets the api.
   *
   * @param mockedAPI the new api
   */
  void setAPI(MailAPI mockedAPI) {
    this.mailserverApi = mockedAPI;
  }

  // *********************** implementation level ***************

  /**
   * Node title.
   *
   * @param node the node
   * @return the string
   * @throws RepositoryException the repository exception
   */
  protected String nodeTitle(Node node) throws RepositoryException {
    return node.getProperty(NodetypeConstant.EXO_TITLE).getString();
  }

  /**
   * Node content.
   *
   * @param node the node
   * @return the node
   * @throws RepositoryException the repository exception
   */
  protected Node nodeContent(Node node) throws RepositoryException {
    return node.getNode("jcr:content");
  }

  /**
   * Node created.
   *
   * @param node the node
   * @return the calendar
   * @throws RepositoryException the repository exception
   */
  protected Calendar nodeCreated(Node node) throws RepositoryException {
    return node.getProperty("jcr:created").getDate();
  }

  /**
   * Mime type.
   *
   * @param content the content
   * @return the string
   * @throws RepositoryException the repository exception
   */
  protected String mimeType(Node content) throws RepositoryException {
    return content.getProperty("jcr:mimeType").getString();
  }

  /**
   * Data.
   *
   * @param content the content
   * @return the property
   * @throws RepositoryException the repository exception
   */
  protected Property data(Node content) throws RepositoryException {
    return content.getProperty("jcr:data");
  }

  /**
   * Generate id.
   *
   * @param workspace the workspace
   * @param path the path
   * @return the uuid
   */
  protected UUID generateId(String workspace, String path) {
    StringBuilder s = new StringBuilder();
    s.append(workspace);
    s.append(path);
    s.append(System.currentTimeMillis());
    s.append(String.valueOf(RANDOM.nextLong()));

    return UUID.nameUUIDFromBytes(s.toString().getBytes());
  }

  /**
   * Gets the exo user.
   *
   * @param userName the user name
   * @return the exo user
   * @throws OutlookException the outlook exception
   */
  protected org.exoplatform.services.organization.User getExoUser(String userName) throws OutlookException {
    try {
      return organization.getUserHandler().findUserByName(userName);
    } catch (Exception e) {
      throw new OutlookException("Error searching user " + userName, e);
    }
  }

  /**
   * Node.
   *
   * @param nodePath the node path
   * @return the node
   * @throws BadParameterException the bad parameter exception
   * @throws RepositoryException the repository exception
   */
  protected Node node(String nodePath) throws BadParameterException, RepositoryException {
    String workspace, path;
    if (nodePath.startsWith("/")) {
      workspace = jcrService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
      path = nodePath;
    } else {
      // TODO it's a not used experimental thing, see also ContentLink component
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

  /**
   * Node.
   *
   * @param workspace the workspace
   * @param path the path
   * @return the node
   * @throws BadParameterException the bad parameter exception
   * @throws RepositoryException the repository exception
   */
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

  /**
   * System node.
   *
   * @param workspace the workspace
   * @param path the path
   * @return the node
   * @throws BadParameterException the bad parameter exception
   * @throws RepositoryException the repository exception
   */
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

  /**
   * Checkout.
   *
   * @param node the node
   * @return true, if successful
   * @throws RepositoryException the repository exception
   */
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

  /**
   * Checks if is null.
   *
   * @param json the json
   * @return true, if is null
   */
  protected boolean isNull(JsonValue json) {
    return json == null || json.isNull();
  }

  /**
   * Checks if is not null.
   *
   * @param json the json
   * @return true, if is not null
   */
  protected boolean isNotNull(JsonValue json) {
    return json != null && !json.isNull();
  }

  /**
   * Current user locale.
   *
   * @return the locale
   */
  protected Locale currentUserLocale() {
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    return context != null ? context.getLocale() : null;
  }

  /**
   * Add nt:file node for given content stream and title. If a node with such
   * name exists a new name will be generated by adding a numerical index to the
   * end.
   * 
   * @param parent {@link Node}
   * @param title {@link String}
   * @param contentType {@link String}
   * @param content {@link InputStream}
   * @return {@link Node}
   * @throws RepositoryException when storage error
   */
  protected Node addFile(Node parent, String title, String contentType, InputStream content) throws RepositoryException {
    Node file;
    String baseName = cleanName(title);
    String name = baseName;

    int siblingNumber = 0;
    do {
      try {
        file = parent.getNode(name);
        // such node already exists - find new name for the file (by adding
        // sibling index to the end)
        siblingNumber++;
        int extIndex = baseName.lastIndexOf(".");
        if (extIndex > 0 && extIndex != baseName.length() - 1) {
          String jcrName = baseName.substring(0, extIndex);
          String jcrExt = baseName.substring(extIndex + 1);
          name = new StringBuilder(jcrName).append('-').append(siblingNumber).append('.').append(jcrExt).toString();
        } else {
          name = new StringBuilder(baseName).append('-').append(siblingNumber).toString();
        }
      } catch (PathNotFoundException e) {
        // no such node exists, add it using internalName created by CD's
        // cleanName()
        file = parent.addNode(name, "nt:file");
        break;
      }
    } while (true);

    Node resource = file.addNode("jcr:content", "nt:resource");
    resource.setProperty("jcr:mimeType", contentType != null ? contentType : ContentType.APPLICATION_OCTET_STREAM.getMimeType());
    Calendar fileDate = Calendar.getInstance();
    resource.setProperty("jcr:lastModified", fileDate);
    resource.setProperty("jcr:data", content);

    if (siblingNumber > 0) {
      int extIndex = title.lastIndexOf(".");
      if (extIndex > 0 && extIndex != title.length() - 1) {
        String titleName = title.substring(0, extIndex);
        String titleExt = title.substring(extIndex + 1);
        title = new StringBuilder(titleName).append(" (").append(siblingNumber).append(").").append(titleExt).toString();
      } else {
        title = new StringBuilder(title).append(" (").append(siblingNumber).append(')').toString();
      }
    }

    if (!file.hasProperty(NodetypeConstant.EXO_TITLE)) {
      file.addMixin(NodetypeConstant.EXO_RSS_ENABLE);
    }
    file.setProperty(NodetypeConstant.EXO_TITLE, title);
    // file.setProperty("exo:summary", summary);
    try {
      file.setProperty(NodetypeConstant.EXO_NAME, title);
    } catch (ConstraintViolationException | ValueFormatException e) {
      LOG.warn("Cannot set exo:name property to '" + title + "' for file " + file.getPath() + ": " + e);
    }

    if (file.isNodeType(NodetypeConstant.EXO_DATETIME)) {
      file.setProperty(NodetypeConstant.EXO_DATE_CREATED, fileDate);
      file.setProperty(NodetypeConstant.EXO_DATE_MODIFIED, fileDate);
    }

    if (file.isNodeType(NodetypeConstant.EXO_MODIFY)) {
      file.setProperty(NodetypeConstant.EXO_LAST_MODIFIED_DATE, fileDate);
      file.setProperty(NodetypeConstant.EXO_LAST_MODIFIER, file.getSession().getUserID());
    }

    // Added when upgraded to PLF 4.4
    if (!file.isNodeType(NodetypeConstant.MIX_REFERENCEABLE)) {
      file.addMixin(NodetypeConstant.MIX_REFERENCEABLE);
    }

    if (!file.isNodeType(NodetypeConstant.MIX_COMMENTABLE)) {
      file.addMixin(NodetypeConstant.MIX_COMMENTABLE);
    }

    if (!file.isNodeType(NodetypeConstant.MIX_VOTABLE)) {
      file.addMixin(NodetypeConstant.MIX_VOTABLE);
    }

    if (!file.isNodeType(NodetypeConstant.MIX_I18N)) {
      file.addMixin(NodetypeConstant.MIX_I18N);
    }

    return file;
  }

  /**
   * Add nt:folder node with given title. If a node with such name exists and
   * <code>forceNew</code> is <code>true</code> a new name will be generated by
   * adding a numerical index to the end, otherwise existing node will be
   * returned.
   * 
   * @param parent {@link Node}
   * @param title {@link String}
   * @param forceNew if <code>true</code> then a new folder will be created with
   *          index in suffix, if <code>false</code> then existing folder will
   *          be returned
   * @return {@link Node}
   * @throws RepositoryException when storage error
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
          // such node already exists - find new name for the file (by adding
          // sibling index to the end)
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
        // no such node exists, add it using internalName created by CD's
        // cleanName()
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

      folder.setProperty(NodetypeConstant.EXO_TITLE, title);
      try {
        folder.setProperty(NodetypeConstant.EXO_NAME, title);
      } catch (ConstraintViolationException | ValueFormatException e) {
        LOG.warn("Cannot set exo:name property to '" + title + "' for folder " + folder.getPath() + ": " + e);
      }

      Calendar folderDate = Calendar.getInstance();
      if (folder.isNodeType(NodetypeConstant.EXO_DATETIME)) {
        folder.setProperty(NodetypeConstant.EXO_DATE_CREATED, folderDate);
        folder.setProperty(NodetypeConstant.EXO_DATE_MODIFIED, folderDate);
      }

      if (folder.isNodeType(NodetypeConstant.EXO_MODIFY)) {
        folder.setProperty(NodetypeConstant.EXO_LAST_MODIFIED_DATE, folderDate);
        folder.setProperty(NodetypeConstant.EXO_LAST_MODIFIER, folder.getSession().getUserID());
      }
    }
    return folder;
  }

  /**
   * Current user id.
   *
   * @return the string
   */
  protected String currentUserId() {
    ConversationState contextState = ConversationState.getCurrent();
    if (contextState != null) {
      return contextState.getIdentity().getUserId();
    }
    return IdentityConstants.ANONIM;
  }

  /**
   * Space service.
   *
   * @return the space service
   */
  protected SpaceService spaceService() {
    return (SpaceService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SpaceService.class);
  }

  /**
   * Social identity manager.
   *
   * @return the identity manager
   */
  protected IdentityManager socialIdentityManager() {
    return (IdentityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(IdentityManager.class);
  }

  /**
   * Social activity manager.
   *
   * @return the activity manager
   */
  protected ActivityManager socialActivityManager() {
    return (ActivityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ActivityManager.class);
  }

  /**
   * Social relationship manager.
   *
   * @return the relationship manager
   */
  protected RelationshipManager socialRelationshipManager() {
    return (RelationshipManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(RelationshipManager.class);
  }

  /**
   * User spaces.
   *
   * @param userId the user id
   * @return the list
   * @throws OutlookSpaceException the outlook space exception
   */
  protected List<OutlookSpace> userSpaces(String userId) throws OutlookSpaceException {
    List<OutlookSpace> spaces = new ArrayList<OutlookSpace>();
    ListAccess<Space> list = spaceService().getMemberSpaces(userId);
    try {
      for (Space socialSpace : list.load(0, list.getSize())) {
        if (!checkRootFolderExistence(socialSpace)) {
          continue;
        }
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
   * Checks the root folder existence.
   *
   * @param socialSpace the social space
   * @return the boolean - 'true' if the root folder exists
   */
  protected boolean checkRootFolderExistence(Space socialSpace) throws RepositoryException, BadParameterException {
    boolean exists = false;
    String rootPath = groupDocsPath(socialSpace.getGroupId());
    try {
      node(rootPath);
      exists = true;
    } catch (PathNotFoundException ex) {
      LOG.warn(socialSpace.getDisplayName() + " doesn’t have Documents node " + rootPath);
    }
    return exists;
  }

  /**
   * Generate the group documents (as
   * /Groups/spaces/$SPACE_GROUP_ID/Documents).<br>
   * 
   * @param groupId {@link String}
   * @return {@link String}
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
   * @throws Exception when error
   */
  protected String groupPath(String groupId) throws Exception {
    String groupsPath = hierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
    return groupsPath + groupId;
  }

  /**
   * Inits the web DAV link.
   *
   * @param node the node
   * @throws OutlookException the outlook exception
   */
  protected void initWebDAVLink(HierarchyNode node) throws OutlookException {
    // WebDAV URL
    try {
      node.setWebdavUrl(org.exoplatform.wcm.webui.Utils.getWebdavURL(node.getNode(), false, true));
    } catch (Exception e) {
      throw new OutlookException("Error generating WebDav URL for node " + node.getFullPath(), e);
    }
  }

  /**
   * Inits the document link.
   *
   * @param siteType the site type
   * @param driveName the drive name
   * @param portalName the portal name
   * @param nodeURI the node URI
   * @param node the node
   * @throws OutlookException the outlook exception
   */
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
        URI requestUri = new URI(request.getScheme(), null, request.getServerName(), request.getServerPort(), null, null, null);

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

  /**
   * Inits the document link.
   *
   * @param space the space
   * @param file the file
   * @throws OutlookException the outlook exception
   */
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
  }

  /**
   * Inits the document link.
   *
   * @param personalDocuments the personal documents
   * @param file the file
   * @throws OutlookException the outlook exception
   */
  protected void initDocumentLink(PersonalDocuments personalDocuments, HierarchyNode file) throws OutlookException {
    // WebDAV URL
    initWebDAVLink(file);

    // Portal URL
    // We need like the following:
    // https://peter.exoplatform.com.ua:8443/portal/intranet/documents?path=Personal%20Documents/Users/j___/jo___/joh___/john/Private/Documents
    initDocumentLink(SiteType.PORTAL, // PORTAL
                     personalDocuments.getDriveName(),
                     Util.getPortalRequestContext().getPortalOwner(), // intranet or dw will be
                     "drives", // documents or drives will be
                     file);
  }

  /**
   * Find given user Personal Documents folder using user session.
   * 
   * @param userName {@link String}
   * @return {@link Node} Personal Documents folder node or <code>null</code>
   * @throws Exception when error
   */
  protected Node userDocumentsNode(String userName) throws Exception {
    // code idea based on ECMS's UIJCRExplorerPortlet.getUserDrive()
    for (DriveData userDrive : driveService.getPersonalDrives(userName)) {
      String homePath = userDrive.getHomePath();
      if (homePath.endsWith("/Private")) {
        String driveRootPath = org.exoplatform.services.cms.impl.Utils.getPersonalDrivePath(homePath, userName);
        return node(driveRootPath);
      }
    }
    return null;
  }

  /**
   * Find given group Documents folder using current user session.
   *
   * @param groupId {@link String}
   * @return {@link Node} space's Documents folder node or <code>null</code>
   * @throws Exception when error
   */
  protected Node spaceDocumentsNode(String groupId) throws Exception {
    return node(groupDocsPath(groupId));
  }

  /**
   * Set read permissions on the target node to all given identities (e.g. space
   * group members). If node not yet <code>exo:privilegeable</code> it will add
   * such mixin to allow set the permissions first. Requested permissions will
   * not be set to the children nodes.<br>
   * 
   * @param node {@link Node} link target node
   * @param identities array of {@link String} with user identifiers (names or
   *          memberships)
   * @throws AccessControlException when access error
   * @throws RepositoryException when storage error
   */
  protected void setPermissions(Node node, String... identities) throws AccessControlException, RepositoryException {
    setPermissions(node, true, false, identities);
  }

  /**
   * Set read permissions on the target node to all given identities (e.g. space
   * group members). Permissions will not be set if target not
   * <code>exo:privilegeable</code> and <code>forcePrivilegeable</code> is
   * <code>false</code>. If <code>deep</code> is <code>true</code> the target
   * children nodes will be checked also for a need to set the requested
   * permissions. <br>
   * 
   * @param node {@link Node} link target node
   * @param deep {@link Boolean} if <code>true</code> then also children nodes
   *          will be set to the requested permissions
   * @param forcePrivilegeable {@link Boolean} if <code>true</code> and node not
   *          yet <code>exo:privilegeable</code> it will add such mixin to allow
   *          set the permissions.
   * @param identities array of {@link String} with user identifiers (names or
   *          memberships)
   * @throws AccessControlException when access error
   * @throws RepositoryException when storage error
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
        String[] ids = identity.split(":");
        if (ids.length == 2) {
          // it's group and we want allow given identity read only and
          // additionally let managers remove the
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
          target.setPermission(new StringBuilder(managerMembership).append(':').append(ids[1]).toString(), MANAGER_PERMISSION);
          target.setPermission(identity, READER_PERMISSION);
        } else {
          // in other cases, we assume it's user identity and user should be
          // able to remove the node
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

  /**
   * Messages folder.
   *
   * @param parent the parent
   * @param identity the identity
   * @return the node
   * @throws RepositoryException the repository exception
   */
  protected Node messagesFolder(Node parent, String... identity) throws RepositoryException {
    Node messagesFolder;
    if (!parent.hasNode(OUTLOOK_MESSAGES_NAME)) {
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

  /**
   * Adds the message file.
   *
   * @param parent the parent
   * @param message the message
   * @return the node
   * @throws RepositoryException the repository exception
   * @throws UnsupportedEncodingException the unsupported encoding exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  protected Node addMessageFile(Node parent,
                                OutlookMessage message) throws RepositoryException, UnsupportedEncodingException, IOException {
    String safeTitle = safeText(message.getSubject());
    String safeContent = safeHtml(message.getBody());
    try (InputStream content = new ByteArrayInputStream(safeContent.getBytes("UTF-8"))) {
      // message file goes w/o summary, it will be generated in UI
      // (OutlookMessageActivity)
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

  /**
   * Post attachment activity.
   *
   * @param destFolder the dest folder
   * @param files the files
   * @param user the user
   * @param comment the comment
   * @return the exo social activity
   * @throws RepositoryException the repository exception
   */
  protected ExoSocialActivity postAttachmentActivity(Folder destFolder,
                                                     List<File> files,
                                                     OutlookUser user,
                                                     String comment) throws RepositoryException {

    final String origType = org.exoplatform.wcm.ext.component.activity.listener.Utils.getActivityType();
    try {
      String author = user.getLocalUser();
      IdentityManager identityManager = socialIdentityManager();
      Identity authorIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, author, true);

      // FYI Code inspired by UIDocActivityComposer
      Map<String, String> activityParams = new LinkedHashMap<String, String>();
      Calendar activityDate = Calendar.getInstance();
      DateFormat dateFormatter = new SimpleDateFormat(ISO8601.SIMPLE_DATETIME_FORMAT);
      String fileName = "", id = "", doclink = "", docpath = "", mimeType = "", contenLink = "", workspace = "", repository = "",
          dateString = "", owner = "", isSymlink = "";
      String REGEX = "|@|";
      int i = 0;
      for (File f : files) {
        if (files.size() - 1 == i) {
          REGEX = "";
        }
        fileName += f.getName() + REGEX;
        id += f.getNode().getUUID() + REGEX;
        mimeType += f.getNode().getNode("jcr:content").getProperty("jcr:mimeType").getString() + REGEX;
        docpath += f.getNode().getPath() + REGEX;
        owner += author + REGEX;
        isSymlink += false + REGEX;
        try {
          contenLink += documentService.getLinkInDocumentsApp(NodeLocation.getNodeLocationByNode(f.getNode()).getPath()) + REGEX;
          doclink += org.exoplatform.wcm.webui.Utils.getWebdavURL(f.getNode()) + REGEX;
        } catch (Exception e) {
          LOG.error("Error getting node download link " + f.getNode(), e);
        }
        dateString += dateFormatter.format(activityDate.getTime()) + REGEX;
        workspace += destFolder.getNode().getSession().getWorkspace().getName() + REGEX;
        repository += ((ManageableRepository) destFolder.getNode().getSession().getRepository()).getConfiguration().getName()
            + REGEX;
        i++;
      }
      activityParams.put(OutlookAttachmentActivity.FILES, fileName);
      activityParams.put(OutlookAttachmentActivity.DOCTITLE, fileName);
      activityParams.put(OutlookAttachmentActivity.WORKSPACE, workspace);
      activityParams.put(OutlookAttachmentActivity.REPOSITORY, repository);
      activityParams.put(OutlookAttachmentActivity.COMMENT, comment);
      activityParams.put(OutlookAttachmentActivity.DOCLINK, doclink);
      activityParams.put(OutlookAttachmentActivity.DOCNAME, fileName);
      activityParams.put(OutlookAttachmentActivity.DOCPATH, docpath);
      activityParams.put(OutlookAttachmentActivity.AUTHOR, owner);
      activityParams.put(OutlookAttachmentActivity.DATE_CREATED, dateString);
      activityParams.put(OutlookAttachmentActivity.DATE_LAST_MODIFIED, dateString);
      activityParams.put(FileUIActivity.ID, id);
      activityParams.put(FileUIActivity.CONTENT_NAME, fileName);
      activityParams.put(FileUIActivity.ACTIVITY_STATUS, comment);
      activityParams.put(FileUIActivity.MIME_TYPE, mimeType);
      activityParams.put(FileUIActivity.CONTENT_LINK, contenLink);
      activityParams.put(FileUIActivity.DOCUMENT_TITLE, fileName);
      activityParams.put(UIDocActivity.IS_SYMLINK, isSymlink);

      // if NT_FILE
      // activityParams.put(UIDocActivity.ID,
      // node.isNodeType(NodetypeConstant.MIX_REFERENCEABLE) ?
      // node.getUUID() : "");
      // activityParams.put(UIDocActivity.CONTENT_NAME, node.getName());
      // activityParams.put(UIDocActivity.AUTHOR, activityOwnerId);
      // activityParams.put(UIDocActivity.DATE_CREATED, strDateCreated);
      // activityParams.put(UIDocActivity.LAST_MODIFIED, strLastModified);
      // activityParams.put(UIDocActivity.CONTENT_LINK,
      // UIDocActivity.getContentLink(node));

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
      activity = activityManager.getActivity(activity.getId());
      return activity;
    } finally {
      org.exoplatform.wcm.ext.component.activity.listener.Utils.setActivityType(origType);
    }
  }

  /**
   * Read email.
   *
   * @param vElem the v elem
   * @return the outlook email
   * @throws OutlookException the outlook exception
   */
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

  /**
   * Fetch query.
   *
   * @param qr the qr
   * @param limit the limit
   * @param res the res
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  protected void fetchQuery(QueryResult qr, int limit, Set<File> res) throws RepositoryException, OutlookException {
    fetchQuery(qr, limit, res, n -> true);
  }

  /**
   * Fetch query.
   *
   * @param qr the qr
   * @param limit the limit
   * @param res the res
   * @param acceptNode the accept node
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  protected void fetchQuery(QueryResult qr, int limit, Set<File> res, Predicate<Node> acceptNode) throws RepositoryException,
                                                                                                  OutlookException {
    SpaceService spaceService = spaceService();
    for (NodeIterator niter = qr.getNodes(); niter.getPosition() < limit && niter.hasNext();) {
      Node node = niter.nextNode();
      try {
        if (acceptNode.test(node)) {
          // detect is it space and then check if space member
          String path = node.getPath();
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
                  // when not a space member - skip this file (but user still
                  // may be an owner of it!)
                  limit++;
                  continue;
                }
              }
            } catch (IndexOutOfBoundsException e) {
              // XXX something not clear with space path, will use portal page
              // path as for Personal
              // Documents (it works well in PLF 4.3)
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
                             Util.getPortalRequestContext().getPortalOwner(), // intranet or dw will be
                             "drives", // documents or drives will be
                             file);
          }
          res.add(file);
        } else {
          limit++;
        }
      } catch (RepositoryException e) {
        LOG.warn("Error read queried node " + e.getMessage() + ". Node skipped: " + node);
        limit++;
      }
    }

  }

  /**
   * Generate message summary text.
   * 
   * @param message {@link String}
   * @return {@link String}
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
   * @param content {@link String}
   * @return boolean
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

    // FYI it's how looks message after MS Word pre-peocessor in Outlook for
    // Windows:
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
   * @param content {@link String}
   * @return {@link String} sanitized content
   */
  protected String safeHtml(String content) {
    String safe = htmlPolicy.sanitize(content);
    safe = makeLinksOpenNewWindow(safe);
    return safe;
  }

  /**
   * Allow only plain text.
   * 
   * @param content {@link String}
   * @return {@link String} sanitized content (as plain text)
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
   * @param text {@link String}
   * @return {@link String} allowed content
   */
  protected String safeActivityMessage(String text) {
    String safe = activityPolicy.sanitize(text);
    safe = makeLinksOpenNewWindow(safe);
    safe = StringEscapeUtils.unescapeHtml(safe);
    return safe;
  }

  /**
   * Make links open new window.
   *
   * @param text the text
   * @return the string
   */
  protected String makeLinksOpenNewWindow(String text) {
    // Make all links target a new window
    // Replace in all links with target attribute to its _blank value
    Matcher m = linkWithTarget.matcher(text);
    StringBuilder sb = new StringBuilder();
    int pos = 0;
    while (m.find()) {
      if (linkNotLocal.matcher(m.group()).find()) {
        int start = m.start(1);
        int end = m.end(1);
        if (start >= 0 && end >= 0) {
          sb.append(text.substring(pos, start));
          sb.append("target=\"_blank\"");
          pos = end;
        } else {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Cannot find link target group in " + m.group(1));
          }
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
      if (linkNotLocal.matcher(m.group()).find()) {
        int start = m.start(2);
        int end = m.end(2);
        if (start >= 0 && end >= 0) {
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
    }
    if (pos < text.length()) {
      sb.append(text.substring(pos));
    }
    return sb.toString();
  }

  /**
   * Get the space name of node.
   *
   * @param node {@link Node}
   * @return {@link String} the group name
   * @throws RepositoryException when storage error
   */
  private static String getSpaceName(Node node) throws RepositoryException {
    NodeHierarchyCreator nodeHierarchyCreator =
                                              (NodeHierarchyCreator) ExoContainerContext.getCurrentContainer()
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
   * @param name {@link String}
   * @return {@link String} JCR compatible name of local file
   */
  public static String cleanName(String name) {
    String str = accentsConverter.transliterate(name.trim());
    // the character ? seems to not be changed to d by the transliterate
    // function
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
      // XXX finally ensure the name doesn't hava a dot at the end
      // https://github.com/exo-addons/outlook/issues/5
      // https://jira.exoplatform.org/browse/COMMONS-510
      int lastCharIndex = cleanedStr.length() - 1;
      char c;
      while (lastCharIndex >= 0 && (c = cleanedStr.charAt(lastCharIndex)) == '.') {
        cleanedStr.deleteCharAt(lastCharIndex);
        if (lastCharIndex == 0) {
          cleanedStr.append('_');
          cleanedStr.append(Integer.toHexString(c).toUpperCase());
        }
        lastCharIndex--;
      }
    }
    return cleanedStr.toString().trim(); // finally trim also
  }
}
