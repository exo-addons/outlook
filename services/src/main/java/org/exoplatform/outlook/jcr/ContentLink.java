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

package org.exoplatform.outlook.jcr;

import org.apache.commons.io.input.AutoCloseInputStream;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.outlook.BadParameterException;
import org.exoplatform.outlook.OutlookException;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ContentLink.java 00000 Aug 14, 2016 pnedonosko $
 */
public class ContentLink {

  /** The Constant CONFIG_HOST. */
  public static final String    CONFIG_HOST        = "server-host";

  /** The Constant CONFIG_SCHEMA. */
  public static final String    CONFIG_SCHEMA      = "server-schema";

  /** The Constant LINK_CACHE_NAME. */
  public static final String    LINK_CACHE_NAME    = "OutlookContentLinkCache";

  /** The Constant EXO_BASE_URL. */
  public static final String    EXO_BASE_URL       = "exo.base.url";

  /** The Constant KEY_EXPIRE_SECONDS. */
  public static final int       KEY_EXPIRE_SECONDS = 60;

  /** The Constant LOG. */
  protected static final Log    LOG                = ExoLogger.getLogger(ContentLink.class);

  /** The Constant RANDOM. */
  protected static final Random RANDOM             = new Random();

  /**
   * The Class KeyPath.
   */
  class KeyPath {
    
    /** The workspace. */
    final String workspace;

    /** The path. */
    final String path;

    /** The user id. */
    final String userId;

    /**
     * Instantiates a new key path.
     *
     * @param userId the user id
     * @param workspace the workspace
     * @param path the path
     */
    protected KeyPath(String userId, String workspace, String path) {
      super();
      this.workspace = workspace;
      this.path = path;
      this.userId = userId;
    }

  }

  /** The active links. */
  private final ExoCache<String, KeyPath> activeLinks;

  /** The jcr service. */
  protected final RepositoryService       jcrService;

  /** The session providers. */
  protected final SessionProviderService  sessionProviders;

  /** The finder. */
  protected final NodeFinder              finder;

  /** The organization. */
  protected final OrganizationService     organization;

  /** The identity registry. */
  protected final IdentityRegistry        identityRegistry;

  /** The config. */
  protected final Map<String, String>     config;

  /** The rest url. */
  protected final String                  restUrl;

  /**
   * Instantiates a new content link.
   *
   * @param jcrService the jcr service
   * @param sessionProviders the session providers
   * @param finder the finder
   * @param organization the organization
   * @param identityRegistry the identity registry
   * @param cacheService the cache service
   * @param params the params
   * @throws ConfigurationException the configuration exception
   */
  public ContentLink(RepositoryService jcrService,
                     SessionProviderService sessionProviders,
                     NodeFinder finder,
                     OrganizationService organization,
                     IdentityRegistry identityRegistry,
                     CacheService cacheService,
                     InitParams params)
      throws ConfigurationException {
    this.jcrService = jcrService;
    this.sessionProviders = sessionProviders;
    this.finder = finder;
    this.organization = organization;
    this.identityRegistry = identityRegistry;

    this.activeLinks = cacheService.getCacheInstance(LINK_CACHE_NAME);

    if (params != null) {
      PropertiesParam param = params.getPropertiesParam("link-configuration");
      if (param != null) {
        config = Collections.unmodifiableMap(param.getProperties());
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Property parameters link-configuration not found, will use default settings.");
        }
        config = Collections.<String, String> emptyMap();
      }
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Component configuration not found, will use default settings.");
      }
      config = Collections.<String, String> emptyMap();
    }

    StringBuilder restUrl = new StringBuilder();
    String exoBaseUrl = System.getProperty(EXO_BASE_URL);
    if (exoBaseUrl == null || exoBaseUrl.toUpperCase().toLowerCase().startsWith("http://localhost")) {
      // seems we have base URL not set explicitly for the server
      String schema = config.get(CONFIG_SCHEMA);
      if (schema == null || (schema = schema.trim()).length() == 0) {
        schema = "http";
      }

      String host = config.get(CONFIG_HOST);
      if (host == null || host.trim().length() == 0) {
        host = null;
        try {
          Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
          while (host == null && interfaces.hasMoreElements()) {
            NetworkInterface nic = interfaces.nextElement();
            Enumeration<InetAddress> addresses = nic.getInetAddresses();
            while (host == null && addresses.hasMoreElements()) {
              InetAddress address = addresses.nextElement();
              if (!address.isLoopbackAddress()) {
                host = address.getHostName();
              }
            }
          }
        } catch (SocketException e) {
          // cannot get net interfaces
        }

        if (host == null) {
          try {
            host = InetAddress.getLocalHost().getHostName();
          } catch (UnknownHostException e) {
            host = "localhost:8080"; // assume development environment otherwise
          }
        }
      }
      restUrl.append(schema);
      restUrl.append("://");
      restUrl.append(host);
    } else {
      restUrl.append(exoBaseUrl);
    }

    restUrl.append('/');
    restUrl.append(PortalContainer.getCurrentPortalContainerName());
    restUrl.append('/');
    restUrl.append(PortalContainer.getCurrentRestContextName());
    this.restUrl = restUrl.toString();

    LOG.info("Default service URL for content links is " + this.restUrl);
  }

  /**
   * Creates the.
   *
   * @param userId the user id
   * @param workspace the workspace
   * @param nodePath the node path
   * @return the string
   * @throws Exception the exception
   */
  public String create(String userId, String workspace, String nodePath) throws Exception {
    Node node = node(userId, workspace, nodePath);
    return create(userId, node);
  }

  /**
   * Creates the.
   *
   * @param userId the user id
   * @param node the node
   * @return the string
   * @throws RepositoryException the repository exception
   */
  public String create(String userId, Node node) throws RepositoryException {
    String workspace = node.getSession().getWorkspace().getName();
    String path = node.getPath();

    KeyPath keyPath = new KeyPath(userId, workspace, path);
    UUID uuid = generateId(workspace, path);
    String key = uuid.toString();

    // active.put(key, keyPath);
    activeLinks.put(key, keyPath);

    return key;
  }

  /**
   * Creates the url.
   *
   * @param userId the user id
   * @param nodePath the node path
   * @param serverLink the server link
   * @return the link resource
   * @throws Exception the exception
   */
  public LinkResource createUrl(String userId, String nodePath, String serverLink) throws Exception {
    Node node = getNode(userId, nodePath);
    return createUrl(userId, node, serverLink);
  }

  /**
   * Creates the url.
   *
   * @param userId the user id
   * @param node the node
   * @param serverLink the server link
   * @return the link resource
   * @throws RepositoryException the repository exception
   */
  public LinkResource createUrl(String userId, Node node, String serverLink) throws RepositoryException {
    String key = create(userId, node);
    StringBuilder link = new StringBuilder();
    if (serverLink != null) {
      link.append(serverLink);
      link.append('/');
      link.append(PortalContainer.getCurrentPortalContainerName());
      link.append('/');
      link.append(PortalContainer.getCurrentRestContextName());
    } else {
      link.append(restUrl);
    }
    link.append("/outlook/content");

    String name;
    if (node.hasProperty("exo:title")) {
      name = node.getProperty("exo:title").getString();
    } else {
      name = node.getName();
    }

    if (name.indexOf('.') < 0) {
      try {
        String mimetype;
        if (node.isNodeType("nt:file")) {
          mimetype = node.getNode("jcr:content").getProperty("jcr:mimeType").getString();
        } else {
          mimetype = DMSMimeTypeResolver.getInstance().getMimeType(name);
        }
        String ext = DMSMimeTypeResolver.getInstance().getExtension(mimetype);
        StringBuilder nameExt = new StringBuilder(name);
        nameExt.append('.');
        nameExt.append(ext);
        name = nameExt.toString();
      } catch (Exception e) {
        LOG.warn("Error getting file extension by mimetype for " + node, e);
      }
    }

    return new LinkResource(name, link.append('/').append(userId).append('/').append(key).toString());
  }

  /**
   * Consume.
   *
   * @param userId the user id
   * @param key the key
   * @return the node content
   * @throws Exception the exception
   */
  public NodeContent consume(String userId, String key) throws Exception {
    // KeyPath keyPath = active.remove(key);
    KeyPath keyPath = activeLinks.remove(key);
    if (keyPath != null) {
      if (keyPath.userId.equals(userId)) {
        // if (Calendar.getInstance().before(keyPath.expired))
        return content(keyPath.userId, keyPath.workspace, keyPath.path);
      } else {
        throw new BadParameterException("User does not mach " + userId);
      }
    }
    return null;
  }

  /**
   * Gets the node.
   *
   * @param userId the user id
   * @param nodePath the node path
   * @return the node
   * @throws Exception the exception
   */
  protected Node getNode(String userId, String nodePath) throws Exception {
    String workspace, path;
    if (nodePath.startsWith("/")) {
      workspace = jcrService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
      path = nodePath;
    } else {
      // used when path given from portlet/REST request where it prefixed with a workspace name
      int i = nodePath.indexOf('/');
      if (i > 0) {
        workspace = nodePath.substring(0, i);
        path = nodePath.substring(i);
      } else {
        throw new BadParameterException("Invalid path " + nodePath);
      }
    }
    return node(userId, workspace, path);
  }

  /**
   * Content.
   *
   * @param userId the user id
   * @param workspace the workspace
   * @param path the path
   * @return the node content
   * @throws Exception the exception
   */
  protected NodeContent content(String userId, String workspace, String path) throws Exception {
    Node node = node(userId, workspace, path);
    Node content = node.getNode("jcr:content");

    final String mimeType = content.getProperty("jcr:mimeType").getString();
    // data stream will be closed when EoF will be reached
    final InputStream data = new AutoCloseInputStream(content.getProperty("jcr:data").getStream());
    return new NodeContent() {
      @Override
      public String getType() {
        return mimeType;
      }

      @Override
      public InputStream getData() {
        return data;
      }
    };
  }

  /**
   * Node.
   *
   * @param userId the user id
   * @param workspace the workspace
   * @param path the path
   * @return the node
   * @throws Exception the exception
   */
  protected Node node(String userId, String workspace, String path) throws Exception {
    // validate user exists
    User user = organization.getUserHandler().findUserByName(userId);
    String fullPath = HierarchyNode.fullPath(workspace, path);
    if (user == null) {
      LOG.warn("Attempt to access node (" + fullPath + ") under not existing user " + userId);
      throw new BadParameterException("User not found for " + fullPath);
    }

    // use user session here:
    // remember real context state and session provider to restore them at the end
    ConversationState contextState = ConversationState.getCurrent();
    SessionProvider contextProvider = sessionProviders.getSessionProvider(null);
    try {
      // we want do all the job under actual (requester) user here
      Identity userIdentity = identityRegistry.getIdentity(userId);
      if (userIdentity != null) {
        ConversationState state = new ConversationState(userIdentity);
        // Keep subject as attribute in ConversationState.
        state.setAttribute(ConversationState.SUBJECT, userIdentity.getSubject());
        ConversationState.setCurrent(state);
        SessionProvider userProvider = new SessionProvider(state);
        sessionProviders.setSessionProvider(null, userProvider);
      } else {
        LOG.warn("User identity not found " + userId + " for content of " + fullPath);
        throw new OutlookException("User identity not found " + userId);
      }

      // work in user session
      SessionProvider sp = sessionProviders.getSessionProvider(null);
      Session userSession = sp.getSession(workspace, jcrService.getCurrentRepository());

      Item item = finder.findItem(userSession, path);
      if (item.isNode()) {
        return (Node) item;
      } else {
        throw new BadParameterException("Not a node " + path);
      }
    } finally {
      // restore context env
      ConversationState.setCurrent(contextState);
      sessionProviders.setSessionProvider(null, contextProvider);
    }
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

}
