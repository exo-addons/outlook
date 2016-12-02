
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
package org.exoplatform.outlook.usecases;

import static org.junit.Assert.*;

import org.exoplatform.outlook.BaseTestCase;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: TestParamFilter.java 00000 Nov 23, 2016 pnedonosko $
 */
public class TestParamFilter {

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(TestParamFilter.class);

  /**
   * Sets the up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
  }

  /**
   * Tear down.
   *
   * @throws Exception the exception
   */
  @After
  public void tearDown() throws Exception {
  }

  /**
   * It's original Gets getBaseDomain() method but raising Exception when it happens instead of returning
   * <code>null</code>.
   *
   * @param url the url
   * @return the base domain
   * @throws Exception when error happen
   */
  private String getBaseDomainOriginal(String url) throws Exception {
    // try {
    URI uri = new URI(url);
    String host = uri.getHost();
    int startIndex = 0;
    int nextIndex = host.indexOf('.');
    int lastIndex = host.lastIndexOf('.');
    while (nextIndex < lastIndex) {
      startIndex = nextIndex + 1;
      nextIndex = host.indexOf('.', startIndex);
    }
    if (startIndex > 0) {
      return host.substring(startIndex);
    } else {
      return host;
    }
    // } catch (Exception e) {
    // LOG.error(" ERR getbase " + url);
    // }
    // return null;
  }
  
  /**
   * Gets the base domain fixed.
   *
   * @param url the url
   * @return the base domain fixed
   * @throws Exception the exception
   */
  private String getBaseDomainFixed(String url) throws Exception {
    URI uri;
    int queryStart = url.indexOf('?');
    if (queryStart > 0) {
      uri = new URI(url.substring(0, queryStart));
    } else {
      uri = new URI(url);
    }
    String host = uri.getHost();
    int startIndex = 0;
    int nextIndex = host.indexOf('.');
    int lastIndex = host.lastIndexOf('.');
    while (nextIndex < lastIndex) {
      startIndex = nextIndex + 1;
      nextIndex = host.indexOf('.', startIndex);
    }
    if (startIndex > 0) {
      return host.substring(startIndex);
    } else {
      return host;
    }
  }

  /**
   * Test.
   */
  @Test
  public void testGetBaseDomain_Original() {
    final String msOutlookUrl = "https://community.exoplatform.com/portal/intranet/outlook?et=&_host_Info=Outlook|Web|16.01|fr-FR|8e705027-851e-4260-4320-fef01cb2884f|";
    try {
      String domain = getBaseDomainOriginal(msOutlookUrl);
      assertEquals("Wrong domain extracted", "community.exoplatform.com", domain);
    } catch(URISyntaxException e) {
      // OK, it's expected as MS's link doesn't respect standards (contains | in the query) 
    } catch(Exception e) {
      fail("Exception should not happen for " + msOutlookUrl + " but it was " + e);
    }
  }
  
  /**
   * Test.
   */
  @Test
  public void testGetBaseDomain_Fixed() {
    final String msOutlookUrl = "https://community.exoplatform.com/portal/intranet/outlook?et=&_host_Info=Outlook|Web|16.01|fr-FR|8e705027-851e-4260-4320-fef01cb2884f|";
    try {
      String domain = getBaseDomainFixed(msOutlookUrl);
      assertEquals("Wrong domain extracted", "exoplatform.com", domain);
    } catch(Exception e) {
      fail("Exception should not happen for " + msOutlookUrl + " but it was " + e);
    }
  }

}
