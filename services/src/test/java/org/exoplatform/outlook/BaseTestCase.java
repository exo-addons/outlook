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
package org.exoplatform.outlook;

import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Credential;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.PasswordCredential;
import org.exoplatform.services.security.UsernameCredential;
import org.exoplatform.wiki.service.WikiService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: BaseTestCase.java 00000 Nov 1, 2016 pnedonosko $
 */
public abstract class BaseTestCase {

  /** The Constant LOG. */
  protected static final Log       LOG            = ExoLogger.getLogger(BaseTestCase.class);

  /** The Constant EXO_USER1. */
  protected static final String    EXO_USER1      = "root";

  /** The tear down failed. */
  protected static AtomicBoolean   tearDownFailed = new AtomicBoolean(false);

  /** The test name. */
  @Rule
  public TestName                  testName       = new TestName();

  /** The container. */
  protected ExoContainer           container;

  /** The identity registry. */
  protected IdentityRegistry       identityRegistry;

  /** The repository service. */
  protected RepositoryService      repositoryService;

  /** The session providers. */
  protected SessionProviderService sessionProviders;

  /** The outlook service. */
  protected OutlookServiceImpl     outlookService;

  /** The repository. */
  protected Repository             repository;

  /** The session. */
  protected Session                session;

  /** The test workspace. */
  protected String                 testWorkspace;

  /** The test path. */
  protected String                 testPath;

  /** The test root. */
  protected Node                   testRoot;

  /**
   * Inits the.
   *
   * @throws Exception the exception
   */
  @Before
  public void init() throws Exception {
    if (tearDownFailed.get()) {
      fail(testName.getMethodName() + " ignored due to previous errors");
    } else {
      // init eXo environment
      if (container == null) {
        // XXX Portal container starts wiki and others with hard to provide standalone configuration
        container = PortalContainer.getInstance();
        // **** Standalone Container init
        // String containerConf =
        // BaseTestCase.class.getResource("/conf/standalone/test-configuration.xml").toString();
        // StandaloneContainer.addConfigurationURL(containerConf);
        // String loginConf = BaseTestCase.class.getResource("/login.conf").toString();
        // System.setProperty("java.security.auth.login.config", loginConf);
        // container = StandaloneContainer.getInstance();
        //

        identityRegistry = (IdentityRegistry) container.getComponentInstanceOfType(IdentityRegistry.class);
        repositoryService = (RepositoryService) container.getComponentInstanceOfType(RepositoryService.class);
        repositoryService.setCurrentRepositoryName(System.getProperty("gatein.jcr.repository.default"));
        sessionProviders = (SessionProviderService) container.getComponentInstanceOfType(SessionProviderService.class);

        // mock some heavy-weight components
        if (container.getComponentAdapter(WikiService.class) == null) {
          WikiService wikiService = mock(WikiService.class);
          given(wikiService.getDefaultWikiSyntaxId()).willReturn(Syntax.XWIKI_2_0.toIdString());
          // getPageOfWikiByName
          // getWikiByTypeAndOwner
          // createWiki
          // createPage
          // isExisting
          // createPage
          // getPageById
          container.registerComponentInstance(WikiService.class, wikiService);
        }

        if (container.getComponentAdapter(ForumService.class) == null) {
          ForumService forumService = mock(ForumService.class);
          // getCategory
          // getForum
          // getUserSettingProfile
          // getForumAdministration
          // saveTopic
          // addWatch
          container.registerComponentInstance(ForumService.class, forumService);
        }

        if (container.getComponentAdapter(ResourceBundleService.class) == null) {
          ResourceBundleService resourceService = mock(ResourceBundleService.class);
          // given(resourceService.getResourceBundle().willReturn("some text");
          container.registerComponentInstance(ResourceBundleService.class, resourceService);
        }

        // finally register the testing component
        if (container.getComponentAdapter(OutlookService.class) == null) {
          container.registerComponentImplementation(OutlookService.class, OutlookServiceImpl.class);
        }
      }

      // login via Authenticator
      loginCurrentUser(EXO_USER1, "");

      // and set session provider to the service
      SessionProvider sessionProvider = new SessionProvider(ConversationState.getCurrent());
      sessionProvider.setCurrentRepository(repositoryService.getCurrentRepository());
      sessionProvider.setCurrentWorkspace("collaboration");
      sessionProviders.setSessionProvider(null, sessionProvider);

      session = sessionProviders.getSessionProvider(null).getSession(sessionProvider.getCurrentWorkspace(),
                                                                     sessionProvider.getCurrentRepository());

      testRoot = session.getRootNode().addNode("testOutlookExtension", "nt:folder");
      session.save();
      testWorkspace = session.getWorkspace().getName();
      testPath = testRoot.getPath();

      //
      outlookService = (OutlookServiceImpl) this.container.getComponentInstanceOfType(OutlookServiceImpl.class);
    }
  }

  /**
   * Cleanup.
   *
   * @throws Exception the exception
   */
  @After
  public void cleanup() throws Exception {
    if (!tearDownFailed.get()) {
      try {
        testRoot.remove();
        session.save();
        // session.logout();
        // sessionProviders.removeSessionProvider(null);
      } catch (Throwable e) {
        if (tearDownFailed.compareAndSet(false, true)) {
          e.printStackTrace();
          fail(testName.getMethodName() + ".tearDown() failed: " + e.getMessage());
        }
      } finally {
        ConversationState.setCurrent(null);
        identityRegistry.unregister(EXO_USER1);
      }
    }
  }

  /**
   * Login current user.
   *
   * @param exoUsername the exo username
   * @param exoPassword the exo password
   * @throws Exception the exception
   */
  protected void loginCurrentUser(String exoUsername, String exoPassword) throws Exception {
    // login via Authenticator
    Authenticator authr = (Authenticator) container.getComponentInstanceOfType(Authenticator.class);
    String user =
                authr.validateUser(new Credential[] { new UsernameCredential(exoUsername), new PasswordCredential(exoPassword) });
    Identity identity = authr.createIdentity(user);
    ConversationState.setCurrent(new ConversationState(identity));

    IdentityRegistry identityRegistry = (IdentityRegistry) container.getComponentInstanceOfType(IdentityRegistry.class);
    // identity.setSubject(subject);
    identityRegistry.register(identity);
  }

  /**
   * Adds the test file.
   *
   * @param title the title
   * @param mimeType the mime type
   * @param content the content
   * @return the node
   * @throws RepositoryException the repository exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  protected Node addTestFile(String title, String mimeType, String content) throws RepositoryException, IOException {
    // use existing service to add a file exactly as it will be in runtime
    try (ByteArrayInputStream contentStream = new ByteArrayInputStream(content.getBytes("UTF-8"))) {
      return outlookService.addFile(testRoot, title, mimeType, contentStream);
    } catch (UnsupportedEncodingException e) {
      fail("Test environment doesn't support UTF-8 " + e);
      return null;
    }
  }

}
