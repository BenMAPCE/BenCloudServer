package gov.epa.bencloud.api.model;

import java.time.LocalDateTime;

import gov.epa.bencloud.api.util.ApiUtil;
import gov.epa.bencloud.server.BenCloudServer;

public class TaskLog {
	private String appVersion = null;
	private int dbVersion = 0;
	private LocalDateTime dtStart = null;
	private LocalDateTime dtEnd = null;
	private boolean success = false;
	
	
	
	public TaskLog() {
		super();
		
		this.appVersion = ApiUtil.appVersion;
		this.dbVersion = ApiUtil.getDatabaseVersion();
		
	}
	
	public String getAppVersion() {
		return appVersion;
	}
	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}
	public int getDbVersion() {
		return dbVersion;
	}
	public void setDbVersion(int dbVersion) {
		this.dbVersion = dbVersion;
	}
	public LocalDateTime getDtStart() {
		return dtStart;
	}
	public void setDtStart(LocalDateTime dtStart) {
		this.dtStart = dtStart;
	}
	public LocalDateTime getDtEnd() {
		return dtEnd;
	}
	public void setDtEnd(LocalDateTime dtEnd) {
		this.dtEnd = dtEnd;
	}
	public boolean isSuccess() {
		return success;
	}
	public void setSuccess(boolean success) {
		this.success = success;
	}
	
	
}
