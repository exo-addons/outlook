
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * JCR file wrapper for UI.
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: Folder.java 00000 Jun 14, 2016 pnedonosko $
 * 
 */
public abstract class File extends HierarchyNode {

  /**
   * File instance constructor.
   * 
   * @param parent {@link Folder}
   * @param node {@link Node}
   * @throws RepositoryException storage error
   * @throws OutlookException if not a file node
   */
  public File(Folder parent, Node node) throws RepositoryException, OutlookException {
    super(parent, node);
    if (!isFile(node)) {
      throw new OutlookException("Not a file node");
    }
  }

  /**
   * Instantiates a new file.
   *
   * @param parentPath the parent path
   * @param node the node
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  public File(String parentPath, Node node) throws RepositoryException, OutlookException {
    super(parentPath, node);
    if (!isFile(node)) {
      throw new OutlookException("Not a file node");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean isFolder() {
    return false;
  }

  /**
   * Gets the size.
   *
   * @return the size
   * @throws RepositoryException the repository exception
   */
  public long getSize() throws RepositoryException {
    Node node = getNode();
    if (node != null) {
      return node.getNode("jcr:content").getProperty("jcr:data").getLength();
    } else {
      return 0l;
    }
  }

}
