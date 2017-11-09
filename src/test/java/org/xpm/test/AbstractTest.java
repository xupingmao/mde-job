package org.xpm.test;

import com.alibaba.fastjson.JSON;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xpm.taskpool.DataSource;
import org.xpm.taskpool.impl.DefaultTaskPool;
import org.xpm.taskpool.util.DBUtils;

import java.sql.*;
import java.util.Map;
import java.util.Properties;

/**
 * Created by xupingmao on 2017/11/2.
 */
public abstract class AbstractTest {

    private static String URL = "jdbc:mysql://localhost:3306/test?useSSL=false";

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static DefaultTaskPool taskPool;

    protected static Logger logger = LoggerFactory.getLogger(AbstractTest.class);

    @BeforeClass
    public static void init() {
        taskPool = new DefaultTaskPool(new DataSource() {
            @Override
            public Connection getConnection() {
                try {
                    Connection connection = DriverManager.getConnection(URL, "root", null);
                    // Properties clientInfo = connection.getClientInfo();
                    PreparedStatement preparedStatement = connection.prepareStatement("show status like 'threads_connected'");
                    preparedStatement.executeQuery();
                    ResultSet resultSet = preparedStatement.getResultSet();
                    if (resultSet.next()) {
                        // 只有taskpool用了一个数据库连接
                        Map<String, Object> resultMap = DBUtils.getResultMap(resultSet);
                        logger.info("threads_connected: {}", resultMap.values().iterator().next());
                    }
                    return connection;
                } catch (SQLException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
    }

    @AfterClass
    public static void destroy() throws InterruptedException {
        taskPool.close();
    }

}
