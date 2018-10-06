package com.yumesoftworks.fileshare.data;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

@Entity (tableName = "FileList")
public class FileListEntry {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String path;
    private String fileName;
    private int isTransferred;

    @Ignore
    public FileListEntry(String path, String fileName, int isTransferred){
        this.path=path;
        this.fileName=fileName;
        this.isTransferred=isTransferred;
    }

    public FileListEntry(int id, String path, String fileName, int isTransferred){
        this.id=id;
        this.path=path;
        this.fileName=fileName;
        this.isTransferred=isTransferred;
    }

    //getters and setters
    public void setId(int id) {
        this.id = id;
    }
    public int getId() {
        return id;
    }

    public void setPath(String path) {
        this.path = path;
    }
    public String getPath() {
        return path;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public String getFileName() {
        return fileName;
    }

    public void setIsTransferred(int isTransferred) {
        this.isTransferred = isTransferred;
    }
    public int getIsTransferred() {
        return isTransferred;
    }
}
