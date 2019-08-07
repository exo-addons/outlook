package org.exoplatform.outlook.portlet;

import org.exoplatform.social.core.identity.model.Identity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdentityInfo {
    private Identity identity;

    public IdentityInfo(Identity identity) {
        this.identity = identity;
    }


    public String getFirstName(){
        return (String) identity.getProfile().getProperty("firstName");
    }

    public String getLastName(){
        return (String) identity.getProfile().getProperty("lastName");
    }

    public String getFullName(){
        return identity.getProfile().getFullName();
    }

    public String getAvatarUrl(){
        return (String) identity.getProfile().getAvatarUrl();
    }

    public String getAboutMe(){
        return (String) identity.getProfile().getProperty("aboutMe");
    }

    public String getProfileLink(){
        return identity.getProfile().getUrl();
    }

    public String getPosition() {
        return identity.getProfile().getPosition();
    }

    public String getEmail() {
        return identity.getProfile().getEmail().toLowerCase();
    }

    public String getId() {
        return identity.getId();
    }

    public String getRemoteId() {
        return identity.getRemoteId();
    }

    public List<String> getLinks() {
        List<String> links = new ArrayList<>();
        List<Map<String,String>> urls = (List<Map<String,String>>)identity.getProfile().getProperty("urls");
        if (urls != null){
            urls.forEach(o -> links.add(o.get("value")));
        }
        return links;
    }

    public Map<String, List<String>> getIms() {
        if (identity.getProfile().getProperty("ims") != null) {

            List<Map<String, String>> imss = (List<Map<String, String>>) identity.getProfile().getProperty("ims");
            List<String> skype = new ArrayList<>(2);
            List<String> gtalk = new ArrayList<>(2);
            List<String> yahoo = new ArrayList<>(2);
            List<String> msn = new ArrayList<>(2);
            List<String> other = new ArrayList<>(2);
            for (Map<String, String> map : imss) {
                switch (map.get("key")) {
                    case "skype":
                        skype.add(map.get("value"));
                        break;
                    case "gtalk":
                        gtalk.add(map.get("value"));
                        break;
                    case "yahoo":
                        yahoo.add(map.get("value"));
                        break;
                    case "msn":
                        msn.add(map.get("value"));
                        break;
                    default:
                        other.add(map.get("value"));
                        break;
                }
            }

            Map<String, List<String>> map = new HashMap<>();
            if (!skype.isEmpty()) {
                map.put("skype", skype);
            }
            if (!gtalk.isEmpty()) {
                map.put("gtalk", gtalk);
            }
            if (!yahoo.isEmpty()) {
                map.put("yahoo", yahoo);
            }
            if (!msn.isEmpty()) {
                map.put("msn", msn);
            }
            if (!other.isEmpty()) {
                map.put("other", other);
            }
            return map;
        }
        return null;
    }

    public boolean isPhoneNumberPresent() {
        return identity.getProfile().getPhones() != null;
    }

    public Map<String, List<String>> getPhones(){
        List<Map<String,String>> ph =identity.getProfile().getPhones();
        List<String> work = new ArrayList<>(2);
        List<String> home = new ArrayList<>(2);
        List<String> other = new ArrayList<>(2);
        for (Map<String, String> map : ph) {
            switch (map.get("key")){
                case "work":
                    work.add(map.get("value"));
                    break;
                case "home":
                    home.add(map.get("value"));
                    break;
                default:
                    other.add(map.get("value"));
                    break;
            }
        }
        Map<String, List<String>> phones = new HashMap<>();
        if(!work.isEmpty()){
            phones.put("work", work);
        }
        if(!home.isEmpty()){
            phones.put("home", work);
        }
        if(!other.isEmpty()){
            phones.put("other", work);
        }

        return phones;
    }


}
