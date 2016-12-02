
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

import org.exoplatform.outlook.BadParameterException;
import org.exoplatform.outlook.OutlookException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.IdentityConstants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * JCR folder warapper for UI.
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: HierarchyNode.java 00000 Jun 14, 2016 pnedonosko $
 * 
 */
public abstract class HierarchyNode {

  /** The Constant MODIFIED_FORMAT. */
  public static final String    MODIFIED_FORMAT = "yyyy-MM-dd hh:mm:ss";

  /** The Constant PATH_SEPARATOR. */
  public static final String    PATH_SEPARATOR  = "/".intern();

  /** The Constant ROOT_PATH_LABEL. */
  public static final String    ROOT_PATH_LABEL = PATH_SEPARATOR;

  /** The Constant EMPTY. */
  protected static final String EMPTY           = "".intern();

  /** The Constant LOG. */
  protected static final Log    LOG             = ExoLogger.getLogger(Node.class);

  /**
   * Checks if is folder.
   *
   * @param node the node
   * @return true, if is folder
   * @throws RepositoryException the repository exception
   */
  public static boolean isFolder(Node node) throws RepositoryException {
    return node.isNodeType("nt:folder") || node.isNodeType("nt:unstructured");
  }

  /**
   * Checks if is file.
   *
   * @param node the node
   * @return true, if is file
   * @throws RepositoryException the repository exception
   */
  public static boolean isFile(Node node) throws RepositoryException {
    return node.isNodeType("nt:file");
  }

  /** The parent path. */
  protected final String   parentPath;

  /** The path. */
  protected final String   path;

  /** The name. */
  protected final String   name;
  
  /** The full path. */
  protected final String   fullPath;

  /** The title. */
  protected final String   title;

  /** The path label. */
  protected String         pathLabel;

  /** The last modifier. */
  protected final String   lastModifier;

  /** The last modified. */
  protected final Calendar lastModified;

  /** The node. */
  protected final Node     node;

  /** The hash code. */
  protected final int      hashCode;

  /** The url. */
  protected String         url;

  /** The webdav url. */
  protected String         webdavUrl;

  /**
   * New hierarchy node instance.
   * 
   * @param parentPath {@link String}
   * @param node {@link Node}
   * @throws RepositoryException storage error
   * @throws BadParameterException when parent path is {@code null}
   */
  protected HierarchyNode(String parentPath, Node node) throws RepositoryException, BadParameterException {
    this.path = node.getPath();
    this.fullPath = fullPath(node.getSession().getWorkspace().getName(), this.path);
    this.node = node;
    this.name = node.getName();

    if (parentPath == null) {
      throw new BadParameterException("Node  '" + name + "' should have root path or parent folder");
    }

    int hc = 1;
    hc = hc * 31 + this.path.hashCode();

    this.parentPath = parentPath;
    this.title = nodeTitle(node);
    if (path.equals(parentPath)) {
      this.pathLabel = ROOT_PATH_LABEL;
    } else {
      this.pathLabel = pathLabel(parentPath, node);
    }

    if (node.hasProperty("exo:lastModifier")) {
      String lastModifier = node.getProperty("exo:lastModifier").getString();
      if (IdentityConstants.SYSTEM.equals(lastModifier)) {
        lastModifier = "System";
      }
      this.lastModifier = lastModifier;
    } else {
      this.lastModifier = EMPTY;
    }

    if (node.hasProperty("exo:dateModified")) {
      this.lastModified = node.getProperty("exo:dateModified").getDate();
      hc = hc * 31 + this.lastModified.hashCode();
    } else {
      this.lastModified = null;
    }

    this.hashCode = hc;
  }

  /**
   * Instantiates a new hierarchy node.
   *
   * @param parent the parent
   * @param node the node
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  protected HierarchyNode(Folder parent, Node node) throws RepositoryException, OutlookException {
    this(parent != null ? parent.getPath() : null, node);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return hashCode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj != null && getClass().isAssignableFrom(obj.getClass())) {
      HierarchyNode other = getClass().cast(obj);
      Node thisNode = getNode();
      Node otherNode = other.getNode();
      if (thisNode != null && otherNode != null) {
        try {
          return thisNode.isSame(otherNode);
        } catch (RepositoryException e) {
          // ignore here
        }
      }
      return this.getFullPath().equals(other.getFullPath());
    }
    return super.equals(obj);
  }

  /**
   * Gets the url.
   *
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the url.
   *
   * @param url the url to set
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Gets the webdav url.
   *
   * @return the webdavUrl
   */
  public String getWebdavUrl() {
    return webdavUrl;
  }

  /**
   * Sets the webdav url.
   *
   * @param webdavUrl the webdavUrl to set
   */
  public void setWebdavUrl(String webdavUrl) {
    this.webdavUrl = webdavUrl;
  }

  /**
   * Gets the node.
   *
   * @return the node
   */
  public Node getNode() {
    return node;
  }

  /**
   * Gets the full path.
   *
   * @return the full path
   */
  public String getFullPath() {
    return fullPath;
  }

  /**
   * Gets the path.
   *
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * Gets the name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the title.
   *
   * @return the name
   */
  public String getTitle() {
    return title;
  }

  /**
   * Gets the path label.
   *
   * @return the pathLabel
   */
  public String getPathLabel() {
    return pathLabel;
  }

  /**
   * Gets the last modifier.
   *
   * @return the lastModifier
   */
  public String getLastModifier() {
    return lastModifier;
  }

  /**
   * Gets the last modified.
   *
   * @return the lastModified
   */
  public String getLastModified() {
    if (lastModified != null) {
      Locale locale = userLocale();
      if (locale == null) {
        locale = Locale.getDefault();
        LOG.warn("OutlookUser locale not found, will use a default one " + locale);
      }
      SimpleDateFormat dateFormat = new SimpleDateFormat(MODIFIED_FORMAT, locale);
      return dateFormat.format(lastModified.getTime());
    } else {
      return EMPTY;
    }
  }

  /**
   * Node title.
   *
   * @param node the node
   * @return the string
   * @throws RepositoryException the repository exception
   */
  protected static String nodeTitle(Node node) throws RepositoryException {
    String title;
    if (node.hasProperty("exo:title")) {
      title = node.getProperty("exo:title").getString();
    } else if (node.hasProperty("exo:name")) {
      title = node.getProperty("exo:name").getString();
    } else {
      title = node.getName();
    }
    return title;
  }

  /**
   * Path label.
   *
   * @param rootPath the root path
   * @param node the node
   * @return the string
   * @throws RepositoryException the repository exception
   */
  protected static String pathLabel(String rootPath, Node node) throws RepositoryException {
    // we assume given root path already reflects the node path
    List<String> reversedSubpath = new ArrayList<String>();
    Node parent = node;
    while (!parent.getPath().equals(rootPath)) {
      reversedSubpath.add(nodeTitle(parent));
      parent = parent.getParent();
    }
    StringBuilder pathLabel = new StringBuilder();
    if (reversedSubpath.size() > 0) {
      for (int i = reversedSubpath.size() - 1; i >= 0; i--) {
        pathLabel.append(PATH_SEPARATOR);
        String plabel = reversedSubpath.get(i);
        pathLabel.append(plabel);
      }
    } else {
      reversedSubpath.add(PATH_SEPARATOR);
    }
    return pathLabel.toString();
  }

  /**
   * Full path.
   *
   * @param workspace the workspace
   * @param path the path
   * @return the string
   */
  public static String fullPath(String workspace, String path) {
    return new StringBuilder().append(workspace).append(path).toString();
  }
  
  /**
   * Gets the path.
   *
   * @param fullPath the full path
   * @return the path
   * @throws BadParameterException the bad parameter exception
   */
  public static String getPath(String fullPath) throws BadParameterException {
    if (fullPath.startsWith("/")) {
      return fullPath;
    } else {
      int i = fullPath.indexOf('/');
      if (i > 0) {
        //workspace = nodePath.substring(0, i);
        return fullPath.substring(i);
      } else {
        throw new BadParameterException("Invalid path " + fullPath);
      }
    }
  }
  
  /**
   * Gets the workspace.
   *
   * @param fullPath the full path
   * @return the workspace
   * @throws BadParameterException the bad parameter exception
   */
  public static String getWorkspace(String fullPath) throws BadParameterException {
    if (fullPath.startsWith("/")) {
      return null;
    } else {
      int i = fullPath.indexOf('/');
      if (i > 0) {
        return fullPath.substring(0, i);
      } else {
        throw new BadParameterException("Invalid path " + fullPath);
      }
    }
  }

  /**
   * Checks if is folder.
   *
   * @return true, if is folder
   */
  public abstract boolean isFolder();

  /**
   * User locale.
   *
   * @return the locale
   */
  protected abstract Locale userLocale();

}
