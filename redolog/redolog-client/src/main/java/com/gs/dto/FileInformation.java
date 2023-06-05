package com.gs.dto;

public class FileInformation {
    private String hostname;
    private String username;
    private String password;
    private String filename;
    private long size;

    public FileInformation(String hostname, String username, String password, String filename, long size) {
        this.hostname = hostname;
        this.username = username;
        this.password = password;
        this.filename = filename;
        this.size = size;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "FileInformation{" +
                "hostname='" + hostname + '\'' +
                ", filename='" + filename + '\'' +
                ", size=" + size +
                '}';
    }

}
