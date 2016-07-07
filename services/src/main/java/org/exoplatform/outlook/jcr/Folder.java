
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

  protected final ThreadLocal<Set<Folder>> subfolders = new ThreadLocal<Set<Folder>>();

  public Folder(Folder parent, Node node) throws RepositoryException, OutlookException {
    super(parent, node);
    if (!isFolder(node)) {
      throw new OutlookException("Not a folder node");
    }
  }

  public Folder(String rootPath, Node node) throws RepositoryException, OutlookException {
    super(rootPath, node);
    if (!isFolder(node)) {
      throw new OutlookException("Not a folder node");
    }
  }

  public boolean isRoot() {
    return rootPath.equals(path);
  }

  public boolean hasSubfolders() throws RepositoryException, OutlookException {
    Set<Folder> subfolders = this.subfolders.get();
    if (subfolders == null) {
      subfolders = readSubnodes();
    }
    return subfolders.size() > 0;
  }

  public Set<Folder> getSubfolders() throws RepositoryException, OutlookException {
    Set<Folder> subfolders = this.subfolders.get();
    if (subfolders == null) {
      subfolders = readSubnodes();
    }
    return Collections.unmodifiableSet(subfolders);
  }

  public abstract Folder addSubfolder(String name) throws RepositoryException, OutlookException;

  protected Set<Folder> readSubnodes() throws RepositoryException, OutlookException {
    Set<Folder> subfolders = new LinkedHashSet<Folder>();
    if (node.hasNodes()) {
      NodeIterator niter = node.getNodes();
      while (niter.hasNext()) {
        Node f = niter.nextNode();
        if ((f.isNodeType("nt:folder") || f.isNodeType("nt:unstructured"))
            && !f.isNodeType(ThumbnailService.HIDDENABLE_NODETYPE)) {
          subfolders.add(newFolder(this, f));
        }
      }
    }
    this.subfolders.set(subfolders);
    return subfolders;
  }

  protected abstract Folder newFolder(Folder parent, Node node) throws RepositoryException, OutlookException;

  protected abstract Folder newFolder(String rootPath, Node node) throws RepositoryException, OutlookException;
}
