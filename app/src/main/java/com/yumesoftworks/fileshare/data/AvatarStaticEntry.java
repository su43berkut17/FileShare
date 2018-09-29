package com.yumesoftworks.fileshare.data;

public class AvatarStaticEntry {
    public static final String TYPE_LOCAL="local";
    public static final String TYPE_REMOTE="remote";

    private int id;
    private String type;
    private String path;

    public AvatarStaticEntry(int id, String type, String path){
        this.id=id;
        this.type=type;
        this.path=path;
    }

    //getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
