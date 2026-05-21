package com.gk.devtools.utils;

/**
 * 数据库类型
 *
 * @author Lowen
 */
public enum DbType {
	/**
	 * 支持MySQL、Oracle、SQLServer、PostgreSQL
	 */
	MySQL("com.mysql.cj.jdbc.Driver"),
	Oracle("oracle.jdbc.driver.OracleDriver"),
	SQLServer("com.microsoft.sqlserver.jdbc.SQLServerDriver"),
	PostgreSQL("org.postgresql.Driver"),
	DM("dm.jdbc.driver.DmDriver");

	private final String driverClass;

	DbType(String driverClass) {
		this.driverClass = driverClass;
	}

	public String getDriverClass() {
		return driverClass;
	}
}