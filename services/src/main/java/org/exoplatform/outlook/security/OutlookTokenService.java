
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
package org.exoplatform.outlook.security;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.web.login.LoginServlet;
import org.exoplatform.web.security.GateInTokenStore;
import org.exoplatform.web.security.codec.CodecInitializer;
import org.exoplatform.web.security.security.CookieTokenService;
import org.exoplatform.web.security.security.TokenServiceInitializationException;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookTokenService.java 00000 Jul 4, 2016 pnedonosko $
 */
public class OutlookTokenService extends CookieTokenService {

  /** The Constant COOKIE_NAME. */
  public static final String COOKIE_NAME = "remembermeoutlook";

  /**
   * Instantiates a new outlook token service.
   *
   * @param initParams {@link InitParams}
   * @param tokenStore {@link GateInTokenStore}
   * @param codecInitializer {@link CodecInitializer}
   * @throws TokenServiceInitializationException when initialization failed
   */
  public OutlookTokenService(InitParams initParams, GateInTokenStore tokenStore, CodecInitializer codecInitializer)
      throws TokenServiceInitializationException {
    super(initParams, tokenStore, codecInitializer);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String nextTokenId() {
    return LoginServlet.COOKIE_NAME + nextRandom();
  }

}
