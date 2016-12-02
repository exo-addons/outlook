
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

import org.exoplatform.outlook.OutlookException;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

/**
 * JCR folder warapper for UI.
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: Folder.java 00000 Jun 14, 2016 pnedonosko $
 * 
 */
public abstract class Folder extends HierarchyNode {

  /** The subfolders. */
  protected final ThreadLocal<Set<Folder>> subfolders = new ThreadLocal<Set<Folder>>();

  /** The files. */
  protected final ThreadLocal<Set<File>>   files      = new ThreadLocal<Set<File>>();

  /** The default subfolder. */
  protected Folder                         defaultSubfolder;

  /**
   * Instantiates a new folder.
   *
   * @param parent the parent
   * @param node the node
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  public Folder(Folder parent, Node node) throws RepositoryException, OutlookException {
    super(parent, node);
    if (!isFolder(node)) {
      throw new OutlookException("Not a folder node");
    }
  }

  /**
   * Instantiates a new folder.
   *
   * @param parentPath the parent path
   * @param node the node
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  public Folder(String parentPath, Node node) throws RepositoryException, OutlookException {
    super(parentPath, node);
    if (!isFolder(node)) {
      throw new OutlookException("Not a folder node");
    }
  }

  /**
   * Checks if is root.
   *
   * @return true, if is root
   */
  public boolean isRoot() {
    return parentPath.equals(path);
  }

  /**
   * Gets the default subfolder.
   *
   * @return the defaultSubfolder
   */
  public Folder getDefaultSubfolder() {
    return defaultSubfolder;
  }

  /**
   * Checks for subfolders.
   *
   * @return true, if successful
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  public boolean hasSubfolders() throws RepositoryException, OutlookException {
    Set<Folder> subfolders = this.subfolders.get();
    if (subfolders == null) {
      subfolders = readSubnodes();
    }
    return subfolders.size() > 0;
  }

  /**
   * Gets the subfolders.
   *
   * @return the subfolders
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  public Set<Folder> getSubfolders() throws RepositoryException, OutlookException {
    Set<Folder> subfolders = this.subfolders.get();
    if (subfolders == null) {
      subfolders = readSubnodes();
    }
    return Collections.unmodifiableSet(subfolders);
  }

  /**
   * Checks for files.
   *
   * @return true, if successful
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  public boolean hasFiles() throws RepositoryException, OutlookException {
    Set<File> files = this.files.get();
    if (files == null) {
      files = readFiles();
    }
    return files.size() > 0;
  }

  /**
   * Gets the files.
   *
   * @return the files
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  public Set<File> getFiles() throws RepositoryException, OutlookException {
    Set<File> files = this.files.get();
    if (files == null) {
      files = readFiles();
    }
    return Collections.unmodifiableSet(files);
  }

  /**
   * Gets the children.
   *
   * @return the children
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  public Set<HierarchyNode> getChildren() throws RepositoryException, OutlookException {
    Set<HierarchyNode> children = new LinkedHashSet<HierarchyNode>();
    // folders first
    children.addAll(getSubfolders());
    // then files
    children.addAll(getFiles());
    return Collections.unmodifiableSet(children);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean isFolder() {
    return true;
  }

  /**
   * Read subnodes.
   *
   * @return the sets the
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  protected Set<Folder> readSubnodes() throws RepositoryException, OutlookException {
    readChildNodes();
    return this.subfolders.get();
  }

  /**
   * Read files.
   *
   * @return the sets the
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  protected Set<File> readFiles() throws RepositoryException, OutlookException {
    readChildNodes();
    return this.files.get();
  }

  /**
   * Read child nodes.
   *
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  protected void readChildNodes() throws RepositoryException, OutlookException {
    Set<Folder> folders = new LinkedHashSet<Folder>();
    Set<File> files = new LinkedHashSet<File>();
    if (node.hasNodes()) {
      NodeIterator niter = node.getNodes();
      while (niter.hasNext()) {
        Node f = niter.nextNode();
        if ((f.isNodeType("nt:folder") || f.isNodeType("nt:unstructured"))
            && !f.isNodeType(ThumbnailService.HIDDENABLE_NODETYPE)) {
          folders.add(newFolder(this, f));
        } else if (f.isNodeType("nt:file")) {
          files.add(newFile(this, f));
        }
      }
    }
    this.subfolders.set(folders);
    this.files.set(files);
  }

  /**
   * New folder.
   *
   * @param parent the parent
   * @param node the node
   * @return the folder
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  protected abstract Folder newFolder(Folder parent, Node node) throws RepositoryException, OutlookException;

  /**
   * New folder.
   *
   * @param rootPath the root path
   * @param node the node
   * @return the folder
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  protected abstract Folder newFolder(String rootPath, Node node) throws RepositoryException, OutlookException;

  /**
   * New file.
   *
   * @param parent the parent
   * @param node the node
   * @return the file
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  protected abstract File newFile(Folder parent, Node node) throws RepositoryException, OutlookException;

  /**
   * Adds the subfolder.
   *
   * @param name the name
   * @return the folder
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  public abstract Folder addSubfolder(String name) throws RepositoryException, OutlookException;
}
