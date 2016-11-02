
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

/**
 * Error in Outlook response format.<br> 
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OutlookFormatException.java 00000 May 16, 2016 pnedonosko $
 * 
 */
public class OutlookFormatException extends OutlookException {

  /**
   * 
   */
  private static final long serialVersionUID = -7588755544731933601L;

  /**
   * @param message error message
   */
  public OutlookFormatException(String message) {
    super(message);
  }

  /**
   * @param cause error cause
   */
  public OutlookFormatException(Throwable cause) {
    super(cause);
  }

  /**
   * @param message error message
   * @param cause error cause
   */
  public OutlookFormatException(String message, Throwable cause) {
    super(message, cause);
  }

}
