
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
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: TestContentLink.java 00000 Nov 1, 2016 pnedonosko $
 * 
 */
public class TestContentLink extends BaseTestCase {

  private static final String SERVER_URL = "http://localhost.localdomain";

  private static final String FILE_TITLE = "Test File.txt";

  private static final String FILE_TYPE  = "text/plain";
  
  private static final String FILE_CONTENT  = "test file content";

  private static final String FILE_UUID  = "fb859021-930c-4811-bd64-c719594a6659";

  private Node                testFile;

  private String              testFileKey, testFileLink;

  @Spy
  private ContentLink         contentLink;

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

  @After
  public void tearDown() throws Exception {
    if (this.testFile != null) {
      this.testFile.remove();
    }
  }

  @Test
  public void testCreateLink() throws RepositoryException, Exception {
    LinkResource link = contentLink.createUrl(EXO_USER1, testFile.getPath(), SERVER_URL);

    assertEquals("File title is wrong ", FILE_TITLE, link.getName());
    assertEquals("File link is wrong ", testFileLink, link.getLink());
  }

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
  
  @Test(expected = BadParameterException.class) 
  public void testLinkUserWrong() throws RepositoryException, Exception {
    contentLink.createUrl(EXO_USER1, testFile.getPath(), SERVER_URL);

    // it should throw BadParameterException
    contentLink.consume("enemy", testFileKey);
  }
  
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
