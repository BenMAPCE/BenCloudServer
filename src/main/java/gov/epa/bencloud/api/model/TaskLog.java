package gov.epa.bencloud.api.model;

import java.time.LocalDateTime;

import gov.epa.bencloud.api.util.ApiUtil;

/*
 *	Representation of a task log
 */
public class TaskLog {
	private String appVersion = null;
	private int dbVersion = 0;
	private LocalDateTime dtStart = null;
	private LocalDateTime dtEnd = null;
	private boolean success = false;
	private String userId = null;
	
	
	/*
 	 *	Default constructor
 	 */
	public TaskLog() {
		super();
		
		this.appVersion = ApiUtil.appVersion;
		this.dbVersion = ApiUtil.getDatabaseVersion();
		
	}
	
	/**
	 * 
	 * @return the app version
	 */
	public String getAppVersion() {
		return appVersion;
	}

	/**
	 * Sets the app version.
	 * @param appVersion
	 */
	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}

	/**
	 * 
	 * @return the database version
	 */
	public int getDbVersion() {
		return dbVersion;
	}

	/**
	 * Sets the database version.
	 * @param dbVersion
	 */
	public void setDbVersion(int dbVersion) {
		this.dbVersion = dbVersion;
	}

	/**
	 * 
	 * @return the start date and time of the task
	 */
	public LocalDateTime getDtStart() {
		return dtStart;
	}

	/**
	 * Sets the start date and time of the task
	 * @param dtStart
	 */
	public void setDtStart(LocalDateTime dtStart) {
		this.dtStart = dtStart;
	}

	/**
	 * 
	 * @return the end date and time of the task
	 */
	public LocalDateTime getDtEnd() {
		return dtEnd;
	}

	/**
	 * Sets the end date and time of the task.
	 * @param dtEnd
	 */
	public void setDtEnd(LocalDateTime dtEnd) {
		this.dtEnd = dtEnd;
	}

	/**
	 * 
	 * @return true if the task is successful, false if not
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * Sets the success parameter of the task log.
	 * @param success
	 */
	public void setSuccess(boolean success) {
		this.success = success;
	}

	/**
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	
}
