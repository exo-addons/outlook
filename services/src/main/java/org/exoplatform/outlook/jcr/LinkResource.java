
package org.exoplatform.outlook.jcr;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: LinkResource.java 00000 Sep 2, 2016 pnedonosko $
 * 
 */
public class LinkResource {
  private final String name;

  private final String link;

  LinkResource(String name, String link) {
    super();
    this.name = name;
    this.link = link;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the link
   */
  public String getLink() {
    return link;
  }

}