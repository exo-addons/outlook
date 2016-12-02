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

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import org.exoplatform.outlook.BadParameterException;
import org.exoplatform.outlook.BaseTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Scanner;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: TestContentLink.java 00000 Nov 1, 2016 pnedonosko $
 */
public class TestContentLink extends BaseTestCase {

  /** The Constant SERVER_URL. */
  private static final String SERVER_URL = "http://localhost.localdomain";

  /** The Constant FILE_TITLE. */
  private static final String FILE_TITLE = "Test File.txt";

  /** The Constant FILE_TYPE. */
  private static final String FILE_TYPE  = "text/plain";
  
  /** The Constant FILE_CONTENT. */
  private static final String FILE_CONTENT  = "test file content";

  /** The Constant FILE_UUID. */
  private static final String FILE_UUID  = "fb859021-930c-4811-bd64-c719594a6659";

  /** The test file. */
  private Node                testFile;

  /** The test file link. */
  private String              testFileKey, testFileLink;

  /** The content link. */
  @Spy
  private ContentLink         contentLink;

  /**
   * Sets the up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    this.contentLink = (ContentLink) this.container.getComponentInstanceOfType(ContentLink.class);
    MockitoAnnotations.initMocks(this);

    UUID knownID = UUID.fromString(FILE_UUID);

    this.testFile = addTestFile(FILE_TITLE, FILE_TYPE, FILE_CONTENT);
    this.session.save();

    this.testFileKey = FILE_UUID.toString();
    this.testFileLink = new StringBuilder(SERVER_URL).append("/portal/rest/outlook/content/")
                                                     .append(EXO_USER1)
                                                     .append('/')
                                                     .append(this.testFileKey)
                                                     .toString();

    // spy-mock a single method of real ContentLink to return a known ID for test file
    when(contentLink.generateId(this.session.getWorkspace().getName(), this.testFile.getPath())).thenReturn(knownID);
  }

  /**
   * Tear down.
   *
   * @throws Exception the exception
   */
  @After
  public void tearDown() throws Exception {
    if (this.testFile != null) {
      this.testFile.remove();
    }
  }

  /**
   * Test create link.
   *
   * @throws RepositoryException the repository exception
   * @throws Exception the exception
   */
  @Test
  public void testCreateLink() throws RepositoryException, Exception {
    LinkResource link = contentLink.createUrl(EXO_USER1, testFile.getPath(), SERVER_URL);

    assertEquals("File title is wrong ", FILE_TITLE, link.getName());
    assertEquals("File link is wrong ", testFileLink, link.getLink());
  }

  /**
   * Test consume link.
   *
   * @throws RepositoryException the repository exception
   * @throws Exception the exception
   */
  @Test
  public void testConsumeLink() throws RepositoryException, Exception {
    contentLink.createUrl(EXO_USER1, testFile.getPath(), SERVER_URL);

    NodeContent content = contentLink.consume(EXO_USER1, testFileKey);

    assertNotNull("File content should be found ", content);
    assertEquals("File type is wrong ", FILE_TYPE, content.getType());

    try (Scanner scanner = new Scanner(content.getData(), "UTF-8")) {
      String fileContent = scanner.useDelimiter("\\A").next();
      assertEquals("File data is wrong ", fileContent, FILE_CONTENT);
    }
  }
  
  /**
   * Test link user wrong.
   *
   * @throws RepositoryException the repository exception
   * @throws Exception the exception
   */
  @Test(expected = BadParameterException.class) 
  public void testLinkUserWrong() throws RepositoryException, Exception {
    contentLink.createUrl(EXO_USER1, testFile.getPath(), SERVER_URL);

    // it should throw BadParameterException
    contentLink.consume("enemy", testFileKey);
  }
  
  /**
   * Test link expired.
   *
   * @throws RepositoryException the repository exception
   * @throws Exception the exception
   */
  @Test
  public void testLinkExpired() throws RepositoryException, Exception {
    contentLink.createUrl(EXO_USER1, testFile.getPath(), SERVER_URL);

    // wait for 5sec (+ a bit more) as configured in test's config of OutlookContentLinkCache liveTime
    // the link should expire and be not available after that
    Thread.sleep(5050);
    
    NodeContent content = contentLink.consume(EXO_USER1, testFileKey);

    assertNull("File content should not be found ", content);
  }

}
