
package org.exoplatform.outlook.jcr;

import org.apache.commons.io.input.AutoCloseInputStream;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.outlook.BadParameterException;
import org.exoplatform.outlook.OutlookException;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ContentLink.java 00000 Aug 14, 2016 pnedonosko $
 * 
 */
public class ContentLink {

  public static final String    CONFIG_HOST        = "server-host";

  public static final String    CONFIG_SCHEMA      = "server-schema";

  public static final int       KEY_EXPIRE_SECONDS = 60;

  protected static final Log    LOG                = ExoLogger.getLogger(ContentLink.class);

  protected static final Random RANDOM             = new Random();

  class KeyPath {
    final String   workspace;

    final String   path;

    final String   userId;

    final Calendar expired;

    protected KeyPath(String userId, String workspace, String path) {
      super();
      this.workspace = workspace;
      this.path = path;
      this.userId = userId;
      this.expired = Calendar.getInstance();
      this.expired.add(Calendar.SECOND, KEY_EXPIRE_SECONDS);
    }

  }

  protected final ConcurrentHashMap<String, KeyPath> active = new ConcurrentHashMap<String, KeyPath>();

  protected final RepositoryService                  jcrService;

  protected final SessionProviderService             sessionProviders;

  protected final NodeFinder                         finder;

  protected final OrganizationService                organization;

  protected final IdentityRegistry                   identityRegistry;

  protected final Map<String, String>                config;

  protected final String                             restUrl;

  public ContentLink(RepositoryService jcrService,
                     SessionProviderService sessionProviders,
                     NodeFinder finder,
                     OrganizationService organization,
                     IdentityRegistry identityRegistry,
                     InitParams params) throws ConfigurationException {
    this.jcrService = jcrService;
    this.sessionProviders = sessionProviders;
    this.finder = finder;
    this.organization = organization;
    this.identityRegistry = identityRegistry;

    /////
    if (params != null) {
      PropertiesParam param = params.getPropertiesParam("link-configuration");

      if (param != null) {
        config = Collections.unmodifiableMap(param.getProperties());
      } else {
        LOG.warn("Property parameters link-configuration not found will use default settings.");
        config = Collections.<String, String> emptyMap();
      }
    } else {
      LOG.warn("Component configuration not found will use default settings.");
      config = Collections.<String, String> emptyMap();
    }

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
          host = "localhost";
        }
      }

      LOG.warn("Configuration of " + CONFIG_HOST + " is not set, will use " + host);
    }

    StringBuilder restUrl = new StringBuilder();
    restUrl.append(schema);
    restUrl.append("://");
    restUrl.append(host);
    restUrl.append('/');
    restUrl.append(PortalContainer.getCurrentPortalContainerName());
    restUrl.append('/');
    restUrl.append(PortalContainer.getCurrentRestContextName());
    this.restUrl = restUrl.toString();

    // TODO organize active map cleanup in time: use eXo cache or thread worker running periodically
  }

  public ContentLink(RepositoryService jcrService,
                     SessionProviderService sessionProviders,
                     NodeFinder finder,
                     OrganizationService organization,
                     IdentityRegistry identityRegistry) throws ConfigurationException {
    this(jcrService, sessionProviders, finder, organization, identityRegistry, null);
  }

  public String create(String userId, String nodePath) throws Exception {
    Node node = getNode(userId, nodePath);
    return create(userId, node);
  }

  public String create(String userId, Node node) throws RepositoryException {
    String workspace = node.getSession().getWorkspace().getName();
    String path = node.getPath();

    KeyPath keyPath = new KeyPath(userId, workspace, path);
    UUID uuid = generateId(workspace, path);
    String key = uuid.toString();

    active.put(key, keyPath);

    return key;
  }

  public LinkResource createUrl(String userId, String nodePath, String serverLink) throws Exception {
    Node node = getNode(userId, nodePath);
    return createUrl(userId, node, serverLink);
  }

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
   * @param userId
   * @param key
   * @return
   * @throws Exception
   */
  public NodeContent consume(String userId, String key) throws Exception {
    KeyPath keyPath = active.remove(key);
    if (keyPath != null) {
      if (keyPath.userId.equals(userId)) {
        if (Calendar.getInstance().before(keyPath.expired)) {
          return content(keyPath.userId, keyPath.workspace, keyPath.path);
        }
      } else {
        throw new BadParameterException("User does not mach " + userId);
      }
    }
    return null;
  }

  public String getNodePath(Node node) throws Exception {
    String workspace = node.getSession().getWorkspace().getName();
    String path = node.getPath();
    return nodePath(workspace, path);
  }

  public Node getNode(String userId, String nodePath) throws Exception {
    String workspace, path;
    if (nodePath.startsWith("/")) {
      workspace = jcrService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
      path = nodePath;
    } else {
      // TODO not used, this code also exists in OutlookServiceImpl.node()
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

  protected Node node(String userId, String workspace, String path) throws Exception {
    // validate user exists
    User user = organization.getUserHandler().findUserByName(userId);
    String nodePath = nodePath(workspace, path);
    if (user == null) {
      LOG.warn("Attempt to access node (" + nodePath + ") under not existing user " + userId);
      throw new BadParameterException("User not found for " + nodePath);
    }

    // use user session here:
    // remember real context state and session provider to restore them at the end
    ConversationState contextState = ConversationState.getCurrent();
    SessionProvider contextProvider = sessionProviders.getSessionProvider(null);
    try {
      // XXX we want do all the job under actual (requester) user here
      Identity userIdentity = identityRegistry.getIdentity(userId);
      if (userIdentity != null) {
        ConversationState state = new ConversationState(userIdentity);
        // Keep subject as attribute in ConversationState.
        state.setAttribute(ConversationState.SUBJECT, userIdentity.getSubject());
        ConversationState.setCurrent(state);
        SessionProvider userProvider = new SessionProvider(state);
        sessionProviders.setSessionProvider(null, userProvider);
      } else {
        LOG.warn("User identity not found " + userId + " for content of " + nodePath);
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

  protected String nodePath(String workspace, String path) {
    return new StringBuilder().append(workspace).append("/").append(path).toString();
  }

  protected UUID generateId(String workspace, String path) {
    StringBuilder s = new StringBuilder();
    s.append(workspace);
    s.append(path);
    s.append(System.currentTimeMillis());
    s.append(String.valueOf(RANDOM.nextLong()));

    return UUID.nameUUIDFromBytes(s.toString().getBytes());
  }

}
