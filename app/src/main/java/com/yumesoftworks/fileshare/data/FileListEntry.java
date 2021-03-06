package com.yumesoftworks.fileshare.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity (tableName = "FileList")
public class FileListEntry implements Comparable<FileListEntry>{

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String path;
    private String fileName;
    private int isTransferred;
    private String parentFolder;
    private int isSelected;
    private String mimeType;
    @Ignore
    private Boolean isDirectory;

    @Ignore
    public FileListEntry(String path, String fileName, int isTransferred, String parentFolder, int isSelected, String mimeType, Boolean isDirectory){
        this.path=path;
        this.fileName=fileName;
        this.isTransferred=isTransferred;
        this.parentFolder=parentFolder;
        this.isSelected=isSelected;
        this.mimeType=mimeType;
        this.isDirectory=isDirectory;
    }

    public FileListEntry(int id, String path, String fileName, int isTransferred, String parentFolder, int isSelected, String mimeType){
        this.id=id;
        this.path=path;
        this.fileName=fileName;
        this.isTransferred=isTransferred;
        this.parentFolder=parentFolder;
        this.isSelected=isSelected;
        this.mimeType=mimeType;
        //this.isDirectory=isDirectory;
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

    public void setParentFolder(String parentFolder) {
        this.parentFolder = parentFolder;
    }
    public String getParentFolder() {
        return parentFolder;
    }

    public void setIsSelected(int isSelected) {
        this.isSelected = isSelected;
    }
    public int getIsSelected() {
        return isSelected;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    public String getMimeType() {
        return mimeType;
    }

    public void setDirectory(Boolean directory) {
        isDirectory = directory;
    }
    public Boolean getDirectory() {
        return isDirectory;
    }

    //comparator
    @Override
    public int compareTo(FileListEntry entry) {
        String UppercaseEntry=entry.getFileName().toUpperCase();

        String UppercaseCompare=this.fileName.toUpperCase();

        //return this.fileName.compareTo(entry.getFileName());
        return UppercaseCompare.compareTo(UppercaseEntry);
    }
}
