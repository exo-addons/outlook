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
   * @param email email
   * @param userName user name
   * @param ewsUrl Exchange web-service URL
   * @return {@link OutlookUser} object to access Outlook Mail API
   * @throws OutlookException Outlook exception
   * @throws RepositoryException storage exception
   */
  OutlookUser getUser(String email, String userName, String ewsUrl) throws OutlookException, RepositoryException;
  
  /**
   * Build Outlook Email address object.
   * 
   * @param email email
   * @param displayName user display name
   * @return OutlookEmail
   * @throws OutlookException when error
   */
  OutlookEmail getAddress(String email, String displayName) throws OutlookException;

  /**
   * Build Outlook message object.
   * 
   * @param messageId {@link String} email message ID
   * @param user {@link OutlookUser}
   * @param from {@link OutlookEmail}
   * @param to {@link OutlookEmail}
   * @param created {@link Calendar}
   * @param modified {@link Calendar}
   * @param subject {@link String} message subject
   * @param body {@link String} message body
   * @return {@link OutlookMessage}
   * @throws OutlookException when error
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
   * @param messageId {@link String} email message ID
   * @param user {@link OutlookUser}
   * @param messageToken {@link String} secure token to access message on Exchange server 
   * @return {@link OutlookMessage}
   * @throws OutlookException when error
   */
  OutlookMessage getMessage(OutlookUser user, String messageId, String messageToken) throws OutlookException;

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
   * @param groupId {@link String} email message ID
   * @return {@link OutlookSpace}
   * @throws OutlookSpaceException when space access error happen
   * @throws OutlookException when error
   * @throws RepositoryException when storage error
   */
  OutlookSpace getSpace(String groupId) throws OutlookSpaceException, RepositoryException, OutlookException;

  List<OutlookSpace> getUserSpaces() throws OutlookSpaceException, RepositoryException, OutlookException;

  UserDocuments getUserDocuments() throws RepositoryException, OutlookException;

}
