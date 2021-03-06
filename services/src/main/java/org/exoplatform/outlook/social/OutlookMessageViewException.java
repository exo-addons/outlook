
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
package org.exoplatform.outlook.social;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookMessageViewException.java 00000 Jul 12, 2016 pnedonosko $
 */
public class OutlookMessageViewException extends Exception {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 385592631491700809L;

  /**
   * Instantiates a new outlook message view exception.
   */
  public OutlookMessageViewException() {
  }

  /**
   * Instantiates a new outlook message view exception.
   *
   * @param message {@link String}
   */
  public OutlookMessageViewException(String message) {
    super(message);
  }

  /**
   * Instantiates a new outlook message view exception.
   *
   * @param cause {@link Throwable}
   */
  public OutlookMessageViewException(Throwable cause) {
    super(cause);
  }

  /**
   * Instantiates a new outlook message view exception.
   *
   * @param message {@link String}
   * @param cause {@link String}
   */
  public OutlookMessageViewException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Instantiates a new outlook message view exception.
   *
   * @param message {@link String}
   * @param cause {@link String}
   * @param enableSuppression boolean
   * @param writableStackTrace boolean
   */
  public OutlookMessageViewException(String message,
                                     Throwable cause,
                                     boolean enableSuppression,
                                     boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
