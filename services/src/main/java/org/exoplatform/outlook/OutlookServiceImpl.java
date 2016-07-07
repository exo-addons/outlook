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
import org.apache.http.entity.ContentType;
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
import org.exoplatform.portal.Constants;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserProfileHandler;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Item;
import javax.jcr.Node;
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

  public static final String            MAILSERVER_URL   = "mailserver-url";

  public static final String            EXO_DATETIME     = "exo:datetime";

  public static final String            EXO_MODIFY       = "exo:modify";

  protected static final Log            LOG              = ExoLogger.getLogger(OutlookServiceImpl.class);

  protected static final Random         RANDOM           = new Random();

  protected static final Transliterator accentsConverter = Transliterator.getInstance("Latin; NFD; [:Nonspacing Mark:] Remove; NFC;");

  protected class UserFolder extends Folder {

    protected UserFolder(Folder parent, Node node) throws RepositoryException, OfficeException {
      super(parent, node);
    }

    protected UserFolder(String rootPath, Node node) throws RepositoryException, OfficeException {
      super(rootPath, node);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Folder addSubfolder(String name) throws RepositoryException, OfficeException {
      final Node parent = getNode();
      Node subfolderNode = addFolder(parent, name);
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
    protected Folder newFolder(Folder parent, Node node) throws RepositoryException, OfficeException {
      Folder folder = new UserFolder(parent, node);
      // initDocumentLink(space, folder);
      return folder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Folder newFolder(String rootPath, Node node) throws RepositoryException, OfficeException {
      Folder folder = new UserFolder(rootPath, node);
      // initDocumentLink(space, folder);
      return folder;
    }
  }

  protected class UserFile extends File {

    protected UserFile(Folder parent, Node node) throws RepositoryException, OfficeException {
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

  protected class OfficeSpaceImpl extends OfficeSpace {

    class SpaceFolder extends UserFolder {

      protected SpaceFolder(Folder parent, Node node) throws RepositoryException, OfficeException {
        super(parent, node);
      }

      protected SpaceFolder(String rootPath, Node node) throws RepositoryException, OfficeException {
        super(rootPath, node);
      }

      /**
       * {@inheritDoc}
       */
      @Override
      protected Set<Folder> readSubnodes() throws RepositoryException, OfficeException {
        Set<Folder> subfolders = super.readSubnodes();
        for (Folder sf : subfolders) {
          initDocumentLink(OfficeSpaceImpl.this, sf);
        }
        return subfolders;
      }
    }

    protected final String rootPath;

    protected OfficeSpaceImpl(Space socialSpace) {
      super(socialSpace.getGroupId(), socialSpace.getDisplayName(), socialSpace.getShortName());
      this.rootPath = groupDocsPath(groupId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Folder getFolder(String path) throws OfficeException, RepositoryException {
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
    public Folder getRootFolder() throws OfficeException, RepositoryException {
      Folder root = new SpaceFolder(rootPath, node(rootPath));
      // TODO root.init(rootPath);
      initDocumentLink(this, root);
      return root;
    }
  }

  protected final RepositoryService                          jcrService;

  protected final SessionProviderService                     sessionProviders;

  protected final IdentityRegistry                           identityRegistry;

  protected final NodeFinder                                 finder;

  protected final NodeHierarchyCreator                       hierarchyCreator;

  protected final OrganizationService                        organization;
  
  protected final CookieTokenService tokenService;

  /**
   * Authenticated users.
   */
  protected final ConcurrentHashMap<String, User>            authenticated = new ConcurrentHashMap<String, User>();

  protected final ConcurrentHashMap<String, OfficeSpaceImpl> spaces        = new ConcurrentHashMap<String, OfficeSpaceImpl>();

  protected MailAPI                                          mailserverApi;

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
                           OrganizationService organization, CookieTokenService tokenService,
                           InitParams params) throws ConfigurationException, MailServerException {
    this.jcrService = jcrService;
    this.sessionProviders = sessionProviders;
    this.identityRegistry = identityRegistry;
    this.hierarchyCreator = hierarchyCreator;
    this.finder = finder;
    this.organization = organization;
    this.tokenService = tokenService;

    // API for user requests (uses credentials from eXo user profile)
    MailAPI api = new MailAPI();
    this.mailserverApi = api;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Folder getFolder(String path) throws OfficeException, RepositoryException {
    Node node = node(path);
    Folder folder = new UserFolder(path, node);
    // TODO folder.init(path);
    return folder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Folder getFolder(Folder parent, String path) throws OfficeException, RepositoryException {
    Node node = node(path);
    return new UserFolder(parent, node);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<File> saveAttachment(OfficeSpace space,
                                   Folder destFolder,
                                   User user,
                                   String messageId,
                                   String attachmentToken,
                                   String... attachmentIds) throws OfficeException, RepositoryException {
    List<File> files = new ArrayList<File>();
    Node parent = destFolder.getNode();
    for (String attachmentId : attachmentIds) {
      JsonValue vatt = mailserverApi.getAttachment(user, messageId, attachmentToken, attachmentId);
      JsonValue vName = vatt.getElement("Name");
      if (isNull(vName)) {
        throw new OfficeFormatException("Attachment doesn't contain Name");
      }
      String name = vName.getStringValue();
      JsonValue vContentType = vatt.getElement("ContentType");
      if (isNull(vContentType)) {
        throw new OfficeFormatException("Attachment (" + name + ") doesn't contain ContentType");
      }
      String contentType = vContentType.getStringValue();
      // TODO Do we need remote size?
      // JsonValue vSize = vatt.getElement("Size");
      // if (isNull(vSize)) {
      // throw new OfficeFormatException("Attachment (" + name + ") doesn't contain Size");
      // }
      // long size = vSize.getLongValue();
      JsonValue vContentBytes = vatt.getElement("ContentBytes");
      if (isNull(vContentBytes)) {
        throw new OfficeFormatException("Attachment (" + name + ") doesn't contain ContentBytes");
      }
      // FYI attachment content in BASE64 (may be twice!)
      String contentBytes = vContentBytes.getStringValue();

      // Save in JCR
      try (InputStream content = decode(contentBytes)) {
        Node attachmentNode = addFile(parent, name, contentType, content);
        files.add(new UserFile(destFolder, attachmentNode));
      } catch (IOException e) {
        throw new OfficeException("Error saving attachment in a file " + name, e);
      }
    }
    parent.save(); // save everything at the end only

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
                                   User user,
                                   String messageId,
                                   String attachmentToken,
                                   String... attachmentIds) throws OfficeException, RepositoryException {
    return saveAttachment(null, destFolder, user, messageId, attachmentToken, attachmentIds);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public User getUser(String userEmail, String ewsUrl) throws OfficeException, RepositoryException {
    try {
      URI ewsUri = new URI(ewsUrl);
      String host = ewsUri.getHost();
      int port = ewsUri.getPort();
      String scheme = ewsUri.getScheme();
      if (port <= 0) {
        port = "https".equalsIgnoreCase(scheme) ? 443 : 80;
      }

      URI mailServerUrl = new URI(scheme, null, host, port, null, null, null);

      // TODO cache users?
      return new User(userEmail, mailServerUrl);
    } catch (URISyntaxException e) {
      throw new MailServerException("Error parsing EWS API URL " + ewsUrl, e);
    }
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
  public OfficeSpace getSpace(String groupId) throws OfficeSpaceException {
    OfficeSpaceImpl space = spaces.get(groupId);
    if (space == null) {
      Space socialSpace = spaceService().getSpaceByGroupId(groupId);
      if (socialSpace != null) {
        space = new OfficeSpaceImpl(socialSpace);
        spaces.put(socialSpace.getGroupId(), space);
      }
    }
    return space;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<OfficeSpace> getUserSpaces() throws OfficeSpaceException {
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

  protected User getCurrentUser() throws OfficeException {
    ConversationState contextState = ConversationState.getCurrent();
    if (contextState != null) {
      String exoUsername = contextState.getIdentity().getUserId();
      if (!IdentityConstants.ANONIM.equals(exoUsername)) {
        User user = authenticated.get(exoUsername);
        if (user == null) {
          // ensure user credentials aren't saved already in eXo user profile
          // TODO
          // BasicCredentials anaplanCreds = getUserCredentials(exoUsername);
          // if (anaplanCreds != null) {
          // // already loged-in, check valid credentials and cache them
          // checkCredentials(anaplanCreds.getUsername(), anaplanCreds.getPassword());
          //
          // // new user instance
          // user = new UserImpl(exoUsername, anaplanCreds.getUsername());
          //
          // // save user in map of authenticated for later use (multi-thread)
          // authenticated.put(exoUsername, user);
          // }
        }
        return user;
      }
    }
    return null;
  }

  protected org.exoplatform.services.organization.User getExoUser(String username) throws OfficeException {
    try {
      return organization.getUserHandler().findUserByName(username);
    } catch (Exception e) {
      throw new OfficeException("Error searching user " + username, e);
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

  protected String getUserLang(String userId) throws OfficeException {
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
        throw new BadParameterException("User profile not found for " + userId);
      }
    } catch (Exception e) {
      throw new OfficeException("Error searching user profile " + userId, e);
    }
  }

  protected String userKey(String exoUsername, String officeUsername) {
    return new StringBuilder(exoUsername).append(officeUsername).toString();
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

    file.setProperty("exo:title", title);
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
   * Add nt:folder node with given title. If a node with such name exists a new name will be
   * generated by adding a numerical index to the end.
   * 
   * @param parent
   * @param title
   * @param contentType
   * @param content
   * @return
   * @throws RepositoryException
   */
  protected Node addFolder(Node parent, String title) throws RepositoryException {
    Node folder;
    String baseName = cleanName(title);
    String name = baseName;

    int siblingNumber = 0;
    do {
      try {
        folder = parent.getNode(name);
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
        folder = parent.addNode(name, "nt:folder");
        break;
      }
    } while (true);

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

  protected List<OfficeSpace> userSpaces(String userId) throws OfficeSpaceException {
    List<OfficeSpace> spaces = new ArrayList<OfficeSpace>();
    ListAccess<Space> list = spaceService().getMemberSpaces(userId);
    try {
      for (Space socialSpace : list.load(0, list.getSize())) {
        spaces.add(new OfficeSpaceImpl(socialSpace));
      }
      return spaces;
    } catch (Throwable e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Error loading user spaces", e);
      }
      throw new OfficeSpaceException("Error loading user spaces", e);
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

  protected void initDocumentLink(OfficeSpace space, HierarchyNode node) throws OfficeException {
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
        throw new OfficeException("Error creating server URL " + request.getRequestURI().toString(), e);
      }
    } else {
      LOG.warn("Portal request not found. Node URL will be relative to this server (w/o host name).");
      url.append(nodeURL.toString());
    }

    node.setUrl(url.toString());
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
