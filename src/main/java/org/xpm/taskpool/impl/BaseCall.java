package org.xpm.taskpool.impl;

import java.sql.Connection;
import java.util.concurrent.Callable;

/**
 * Created by xupingmao on 2017/12/4.
 */
public abstract class BaseCall<V> implements Callable<V> {

    private final Connection connection;

    public BaseCall(Connection connection) {
        this.connection = connection;
    }

    /**
     * 安全的call方法
     * 框架会处理善后
     * @param connection 传入的连接池参数
     * @return
     */
    public abstract V doCall(Connection connection) throws Exception;

    @Override
    public V call() throws Exception {
        try {
            V v = doCall(connection);
            if (connection != null && !connection.getAutoCommit()) {
                connection.commit();
            }
            return v;
        } catch (Exception ex) {
            if (connection != null && !connection.getAutoCommit()) {
                connection.rollback();
            }
            throw ex;
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }
}
