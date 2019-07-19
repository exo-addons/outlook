package org.exoplatform.outlook.portlet;

import java.text.DateFormat;
import java.util.Locale;

public class ActivityInfo {
    private String title;
    private String type;
    private String link;
    private String postedDate;

    public ActivityInfo() {
    }

    ActivityInfo(String title, String type, String link, Long postedDate) {
        this.title = title;
        this.type = type;
        this.link = link;
        this.postedDate = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(postedDate) ;
    }
}
