package com.alibaba.druid;

import java.sql.*;

/**
 */
public class TestJdbc {

    public static void show_tables(String url, String name, String password,String sql)  {
        // 固定使用host : 172.22.96.76:8888

        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;


        try {
            Class.forName("com.mysql.jdbc.Driver");
            //Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(url, name, password);
            stmt = con.createStatement();
            rs = stmt.executeQuery(sql);
            ResultSetMetaData metaData = rs.getMetaData();
            int colCount = metaData.getColumnCount();
            int lineNum = 1;
            while (rs.next()) {
                System.out.println(lineNum + " ");

                for (int i = 1;i <= colCount; ++i) {//这里注意序号从1开始 getColumnName和getColumnLabel是一样的
                    System.out.print(rs.getMetaData().getColumnName(i) +"=" + rs.getObject(i).toString() + " ");
//                    System.out.print("**********************");
//                    System.out.print(rs.getMetaData().getColumnLabel(i) +"=" + rs.getObject(i).toString() + " ");
                    System.out.println();
                }
                System.out.println();
                lineNum++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        String url = "jdbc:mysql://127.0.0.1:3306/test?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8";
        String name = "root";
        String pwd = "123456";
        String sql = "select * from user";
        show_tables(url,name,pwd,sql);
    }

}
