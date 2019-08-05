package org.exoplatform.outlook.portlet;

import java.util.List;

public class addAddresseeInfo {

  private final List<IdentityInfo> connectionList;
  
  private final String[] presentEmail;

  public addAddresseeInfo(List<IdentityInfo> connectionList, String[] presentEmail) {
    this.connectionList = connectionList;
    this.presentEmail = presentEmail;
  }

  public List<IdentityInfo> getConnectionList() {
    return connectionList;
  }

  public String[] getPresentEmail() {
    return presentEmail;
  }
}
