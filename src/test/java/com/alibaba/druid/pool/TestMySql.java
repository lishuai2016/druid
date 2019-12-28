package com.alibaba.druid.pool;

import java.sql.Connection;

import junit.framework.TestCase;

public class TestMySql extends TestCase {

    private DruidDataSource dataSource = new DruidDataSource();

    protected void setUp() throws Exception {
        String jdbcUrl = "jdbc:mysql://127.0.0.1:3306/test";
        String user = "root";
        String password = "123456";

        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setMinIdle(3);//最小链接数
        dataSource.setKeepAlive(true);
    }

    protected void tearDown() throws Exception {
        dataSource.close();
    }

    public void test_0() throws Exception {
        Connection conn = dataSource.getConnection();

        Thread.sleep(1000000000);
        conn.close();
        System.out.println();
    }
}
