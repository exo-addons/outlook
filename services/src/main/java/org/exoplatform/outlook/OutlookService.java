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
import org.exoplatform.outlook.jcr.UserDocuments;

import java.util.Calendar;
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

  final String MESSAGE_NODETYPE       = "mso:message";

  /**
   * Build Outlook user object and associate it with current user in eXo portal.
   * 
   * @param email
   * @param userName
   * @param ewsUrl
   * @return {@link OutlookUser} object to access Outlook Mail API
   * @throws OutlookException
   * @throws RepositoryException
   */
  OutlookUser getUser(String email, String userName, String ewsUrl) throws OutlookException, RepositoryException;
  
  /**
   * Build Outlook Email address object.
   * 
   * @param email
   * @param displayName
   * @return
   * @throws OutlookException
   */
  OutlookEmail getAddress(String email, String displayName) throws OutlookException;

  /**
   * Build Outlook message object.
   * 
   * @param messageId
   * @param user
   * @param from
   * @param to
   * @param created
   * @param modified
   * @param subject
   * @param body
   * @return
   * @throws OutlookException
   */
  OutlookMessage buildMessage(String messageId,
                              OutlookUser user,
                              OutlookEmail from,
                              List<OutlookEmail> to,
                              Calendar created,
                              Calendar modified,
                              String subject,
                              String body) throws OutlookException;

  /**
   * Read Outlook message from server.
   * 
   * @param user
   * @param messageId
   * @param messageId
   * @param messageToken
   * 
   * @return
   * @throws OutlookException
   */
  OutlookMessage getMessage(OutlookUser user, String messageId, String messageToken) throws OutlookException;

  // OutlookMessage getUserMessage(OutlookUser user, String messageId) throws OutlookException;

  Folder getFolder(String path) throws OutlookException, RepositoryException;

  Folder getFolder(Folder parent, String path) throws OutlookException, RepositoryException;

  List<File> saveAttachment(OutlookSpace space,
                            Folder folder,
                            OutlookUser user,
                            String comment,
                            String messageId,
                            String attachmentToken,
                            String... attachmentIds) throws OutlookSpaceException, OutlookException, RepositoryException;

  List<File> saveAttachment(Folder destFolder,
                            OutlookUser user,
                            String comment,
                            String messageId,
                            String attachmentToken,
                            String... attachmentIds) throws OutlookException, RepositoryException;

  /**
   * Return Office space handler.
   * 
   * @return {@link OutlookSpace}
   * @throws OutlookSpaceException
   * @throws OutlookException
   * @throws RepositoryException
   */
  OutlookSpace getSpace(String groupId) throws OutlookSpaceException, RepositoryException, OutlookException;

  List<OutlookSpace> getUserSpaces() throws OutlookSpaceException, RepositoryException, OutlookException;

  UserDocuments getUserDocuments() throws RepositoryException, OutlookException;

}
