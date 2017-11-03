package org.xpm.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.xpm.taskpool.DataSource;
import org.xpm.taskpool.impl.DefaultTaskPool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by xupingmao on 2017/11/2.
 */
public abstract class AbstractTest {

    private static String URL = "jdbc:mysql://localhost:3306/test";

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static DefaultTaskPool taskPool;

    @BeforeClass
    public static void init() {
        taskPool = new DefaultTaskPool(new DataSource() {
            @Override
            public Connection getConnection() {
                try {
                    return DriverManager.getConnection(URL, "root", null);
                } catch (SQLException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
    }

    @AfterClass
    public static void destroy() {
        taskPool.close();
    }

}
