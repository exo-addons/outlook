
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

  public static final String    MODIFIED_FORMAT = "yyyy-MM-dd hh:mm:ss";

  public static final String    PATH_SEPARATOR  = "/".intern();

  public static final String    ROOT_PATH_LABEL = PATH_SEPARATOR;

  protected static final String EMPTY           = "".intern();

  protected static final Log    LOG             = ExoLogger.getLogger(Node.class);

  public static boolean isFolder(Node node) throws RepositoryException {
    return node.isNodeType("nt:folder") || node.isNodeType("nt:unstructured");
  }

  public static boolean isFile(Node node) throws RepositoryException {
    return node.isNodeType("nt:file");
  }

  protected final String   rootPath;

  protected final String   path;

  protected final String   name;

  protected final String   title;

  protected String         pathLabel;

  protected final String   lastModifier;

  protected final Calendar lastModified;

  protected final Node     node;

  protected String         url;

  /**
   * @throws RepositoryException
   * 
   */
  protected HierarchyNode(String rootPath, Node node) throws RepositoryException, OutlookException {
    this.path = node.getPath();
    this.node = node;
    this.name = node.getName();

    if (rootPath == null) {
      throw new BadParameterException("Node  '" + name + "' should have root path or parent folder");
      // TODO
      // if (parent == null) {
      // } else {
      // // grab root path from given parent
      // rootPath = parent.getRootPath();
      // }
    }

    this.rootPath = rootPath;
    this.title = nodeTitle(node);
    if (path.equals(rootPath)) {
      this.pathLabel = ROOT_PATH_LABEL;
    } else {
      this.pathLabel = pathLabel(rootPath, node);
      // this.pathLabel = new
      // StringBuilder(parent.getPathLabel()).append(PATH_SEPARATOR).append(this.title).toString();
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
    } else {
      this.lastModified = null;
    }
  }

  protected HierarchyNode(Folder parent, Node node) throws RepositoryException, OutlookException {
    this(parent != null ? parent.getRootPath() : null, node);
  }

  /**
   * Optionally init path label.
   * 
   * @throws BadParameterException if given path isn't a parent path for this node.
   */
  @Deprecated // NOT USED
  public void init(String rootPath) throws BadParameterException {
    if (path.startsWith(rootPath)) {
      int rootLength = rootPath.length();
      if (rootLength < path.length()) {
        // this.pathLabel = new
        // StringBuilder(parent.getPathLabel()).append(PATH_SEPARATOR).append(title).toString();
        this.pathLabel = pathLabel.substring(rootLength);
      } else {
        this.pathLabel = ROOT_PATH_LABEL;
      }
    } else {
      throw new BadParameterException("Node '" + path + "' does not belong to given root path '" + rootPath + "'");
    }
  }

  /**
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * @param url the url to set
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * @return the node
   */
  public Node getNode() {
    return node;
  }

  /**
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the name
   */
  public String getTitle() {
    return title;
  }

  /**
   * @return the pathLabel
   */
  public String getPathLabel() {
    return pathLabel;
  }

  /**
   * @return the lastModifier
   */
  public String getLastModifier() {
    return lastModifier;
  }

  /**
   * @return the lastModified
   */
  public String getLastModified() {
    if (lastModified != null) {
      Locale locale = userLocale();
      if (locale == null) {
        locale = Locale.getDefault();
        LOG.warn("User locale not found, will use a default one " + locale);
      }
      SimpleDateFormat dateFormat = new SimpleDateFormat(MODIFIED_FORMAT, locale);
      return dateFormat.format(lastModified.getTime());
    } else {
      return EMPTY;
    }
  }

  /**
   * @return the rootPath
   */
  protected String getRootPath() {
    return rootPath;
  }

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
    // String pathLabel = new
    // StringBuilder(parent.getPathLabel()).append(PATH_SEPARATOR).append(this.title).toString();
    return pathLabel.toString();
  }

  protected abstract Locale userLocale();

}
