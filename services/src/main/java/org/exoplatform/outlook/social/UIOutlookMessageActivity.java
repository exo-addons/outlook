
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

import org.exoplatform.outlook.OutlookMessageStore;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: UIOutlookMessageActivity.java 00000 Jul 12, 2016 pnedonosko $
 * 
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:groovy/templates/UIOutlookMessageActivity.gtmpl",
                 events = { @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
                     @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
                     @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
                     @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
                     @EventConfig(listeners = BaseUIActivity.PostCommentActionListener.class),
                     @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class),
                     @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class) })
public class UIOutlookMessageActivity extends BaseUIActivity {

  public static final String ACTIVITY_TYPE = "OUTLOOK_MESSAGE_ACTIVITY";

  public String getBody() {
    OutlookMessageStore messageStore = this.getApplicationComponent(OutlookMessageStore.class);
    String body = messageStore.getMessage(getActivity().getId());
    if (body == null) {
      body = "<br>";
    }
    return body;
  }
  
}
