/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
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
package org.exoplatform.outlook;

import java.util.Calendar;
import java.util.List;

import javax.jcr.RepositoryException;

import org.exoplatform.outlook.jcr.File;
import org.exoplatform.outlook.jcr.Folder;
import org.exoplatform.outlook.jcr.UserDocuments;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookService.java 00000 Mar 4, 2016 pnedonosko $
 */
public interface OutlookService {

  /** The personal drive parrten. */
  final String PERSONAL_DRIVE_PARRTEN = "/Users/${userId}/Private";

  /** The group drive parrten. */
  final String GROUP_DRIVE_PARRTEN    = "/Groups${groupId}/Documents";

  /** The message nodetype. */
  final String MESSAGE_NODETYPE       = "mso:message";

  /**
   * Build Outlook user object and associate it with current user in eXo
   * organization.
   * 
   * @param email the email
   * @param userName the user name
   * @param ewsUrl the Exchange server web-service URL
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
   * @param title the title
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
                              String title,
                              String subject,
                              String body) throws OutlookException;

  /**
   * Read Outlook message from server.
   *
   * @param user {@link OutlookUser}
   * @param messageId {@link String} email message ID
   * @param messageToken {@link String} secure token to access message on
   *          Exchange server
   * @return {@link OutlookMessage}
   * @throws OutlookException when error
   */
  OutlookMessage getMessage(OutlookUser user, String messageId, String messageToken) throws OutlookException;

  /**
   * Save attachment.
   *
   * @param space the space
   * @param folder the folder
   * @param user the user
   * @param comment the comment
   * @param messageId the message id
   * @param attachmentToken the attachment token
   * @param attachmentIds the attachment ids
   * @return the list
   * @throws OutlookSpaceException the outlook space exception
   * @throws OutlookException the outlook exception
   * @throws RepositoryException the repository exception
   */
  List<File> saveAttachment(OutlookSpace space,
                            Folder folder,
                            OutlookUser user,
                            String comment,
                            String messageId,
                            String attachmentToken,
                            String... attachmentIds) throws OutlookSpaceException, OutlookException, RepositoryException;

  /**
   * Save attachment.
   *
   * @param destFolder the dest folder
   * @param user the user
   * @param comment the comment
   * @param messageId the message id
   * @param attachmentToken the attachment token
   * @param attachmentIds the attachment ids
   * @return the list
   * @throws OutlookException the outlook exception
   * @throws RepositoryException the repository exception
   */
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
   * @throws RepositoryException when storage error
   * @throws OutlookException when error
   */
  OutlookSpace getSpace(String groupId) throws OutlookSpaceException, RepositoryException, OutlookException;

  /**
   * Gets the user spaces.
   *
   * @return the user spaces
   * @throws OutlookSpaceException the outlook space exception
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  List<OutlookSpace> getUserSpaces() throws OutlookSpaceException, RepositoryException, OutlookException;

  /**
   * Gets current user Personal Documents.
   *
   * @return the user documents
   * @throws RepositoryException the repository exception
   * @throws OutlookException the outlook exception
   */
  UserDocuments getUserDocuments() throws RepositoryException, OutlookException;
}
