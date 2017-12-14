package org.xpm.test;

import com.alibaba.fastjson.JSON;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xpm.taskpool.impl.DefaultTaskPool;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;

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
            public PrintWriter getLogWriter() throws SQLException {
                return null;
            }

            @Override
            public void setLogWriter(PrintWriter out) throws SQLException {

            }

            @Override
            public void setLoginTimeout(int seconds) throws SQLException {

            }

            @Override
            public int getLoginTimeout() throws SQLException {
                return 0;
            }

            @Override
            public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
                return null;
            }

            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                return null;
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return false;
            }

            @Override
            public Connection getConnection() {
                try {
                    Connection connection = DriverManager.getConnection(URL, "root", null);
                    /*
                    PreparedStatement preparedStatement = connection.prepareStatement("show status like 'threads_connected'");
                    preparedStatement.executeQuery();
                    ResultSet resultSet = preparedStatement.getResultSet();
                    if (resultSet.next()) {
                        // 只有taskpool用了一个数据库连接
                        Map<String, Object> resultMap = Utils.getResultMap(resultSet);
                        logger.info("threads_connected: {}", resultMap.values().iterator().next());
                    }*/
                    return connection;
                } catch (SQLException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return null;
            }
        });
    }

    @AfterClass
    public static void destroy() throws InterruptedException {
        taskPool.close();
    }

    public void log(Object object) {
        System.out.println(JSON.toJSONString(object, true));
    }

}
