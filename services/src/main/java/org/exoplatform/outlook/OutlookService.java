/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
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

import org.exoplatform.outlook.jcr.File;
import org.exoplatform.outlook.jcr.Folder;

import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookService.java 00000 Mar 4, 2016 pnedonosko $
 */
public interface OutlookService {

  final String PERSONAL_DRIVE_PARRTEN = "/Users/${userId}/Private";

  final String GROUP_DRIVE_PARRTEN    = "/Groups${groupId}/Documents";

  /**
   * Create Outlook user.
   * 
   * @return {@link User} object to access Outlook Mail API
   * @throws OutlookException
   * @throws RepositoryException
   */
  User getUser(String userEmail, String ewsUrl) throws OutlookException, RepositoryException;

  Folder getFolder(String path) throws OutlookException, RepositoryException;

  Folder getFolder(Folder parent, String path) throws OutlookException, RepositoryException;

  List<File> saveAttachment(OutlookSpace space,
                            Folder folder,
                            User user,
                            String messageId,
                            String attachmentToken,
                            String... attachmentIds) throws OutlookSpaceException, OutlookException, RepositoryException;

  List<File> saveAttachment(Folder destFolder,
                            User user,
                            String messageId,
                            String attachmentToken,
                            String... attachmentIds) throws OutlookException, RepositoryException;

  /**
   * Return Office space handler.
   * 
   * @return {@link OutlookSpace}
   * @throws OutlookSpaceException
   */
  OutlookSpace getSpace(String groupId) throws OutlookSpaceException;

  List<OutlookSpace> getUserSpaces() throws OutlookSpaceException;

}
