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

    public ActivityInfo(String title, String type, String link, Long postedDate) {
        this.title = title;
        this.type = type;
        this.link = link;
        this.postedDate = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(postedDate) ;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getPostedDateString() {
        return postedDate;
    }

    public void setPostedDateString(String postedDateString) {
        this.postedDate = postedDateString;
    }
}
