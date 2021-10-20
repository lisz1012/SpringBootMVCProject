package com.lisz.test;

import java.sql.*;

public class HiveJDBCClient {
	private static final String DRIVER_NAME = "org.apache.hive.jdbc.HiveDriver";
	private static final String URL = "jdbc:hive2://hadoop-03:10000/default";
	private static final String USERNAME = "root";
	private static final String PASSWORD = "";

	public static void main(String[] args) {
		try {
			Class.forName(DRIVER_NAME);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		try {
			Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
			PreparedStatement pstmt = connection.prepareStatement("select * from psn");
			final ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				System.out.println(rs.getInt("id") + "-" + rs.getString("name") + "-" + rs.getObject("likes") + "-" + rs.getObject(4));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
