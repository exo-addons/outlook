
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

import java.util.Collection;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: UserDocuments.java 00000 Aug 12, 2016 pnedonosko $
 */
public interface UserDocuments {

  /**
   * Gets the folder.
   *
   * @param path the path
   * @return the folder
   * @throws OutlookException the outlook exception
   * @throws RepositoryException the repository exception
   */
  Folder getFolder(String path) throws OutlookException, RepositoryException;

  /**
   * Gets the root folder.
   *
   * @return the root folder
   * @throws OutlookException the outlook exception
   * @throws RepositoryException the repository exception
   */
  Folder getRootFolder() throws OutlookException, RepositoryException;

  /**
   * Find all last documents.
   *
   * @param text the text
   * @return the collection
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  Collection<File> findAllLastDocuments(String text) throws RepositoryException, OutlookException;

  /**
   * Find last documents.
   *
   * @param text the text
   * @return the collection
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  Collection<File> findLastDocuments(String text) throws RepositoryException, OutlookException;

}
