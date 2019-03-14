package com.oo0o0o.colorphoria;

import java.util.List;
import java.util.Date;

/**
 * Created by caitoo0o on 1/28/2019.
 */
public class UploadedMediaEntity {
    String username;
	String downloadUrl;
	String filename;
	String privacy;
	String comment;
	Date timestamp;
	
	public void setUsername(String username) {
		this.username = username;
	}
	public String getUsername() {
		return username;
	}
	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}
	public String getDownloadUrl() {
		return downloadUrl;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getFilename() {
		return filename;
	}
	public void setPrivacy(String privacy) {
		this.privacy = privacy;
	}
	public String getPrivacy() {
		return privacy;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public String getComment() {
		return comment;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public Date getTimestamp() {
		return timestamp;
	}
}
