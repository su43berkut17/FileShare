package com.yumesoftworks.fileshare.data;

public class StorageListEntry {
    private String name;
    private String path;

    public StorageListEntry(String name, String path){
        this.name=name;
        this.path=path;
    }

    //getters and setters
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return name;
    }
}
