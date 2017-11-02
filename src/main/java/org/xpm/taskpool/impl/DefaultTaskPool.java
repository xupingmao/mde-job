package org.xpm.taskpool.impl;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xpm.taskpool.DataSource;
import org.xpm.taskpool.Task;
import org.xpm.taskpool.TaskPool;
import org.xpm.taskpool.TaskToken;
import org.xpm.taskpool.exception.TaskCommitException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import sun.reflect.misc.ReflectUtil;

import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by xupingmao on 2017/10/30.
 */
public class DefaultTaskPool implements TaskPool {

    private final DataSource dataSource;
    private ExecutorService service = Executors.newSingleThreadExecutor();
    private final String tableName = TaskPoolConfig.getTableName();
    private final int GET_INTERVAL = TaskPoolConfig.getGetInterval();
    private volatile boolean stopped = false;
    private Logger LOG = LoggerFactory.getLogger(DefaultTaskPool.class);

    public DefaultTaskPool(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void put(String taskType, String taskId, String params, long timeoutMillis, long delayMillis) throws Exception {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must > 0");
        }
        if (delayMillis < 0) {
            throw new IllegalArgumentException("delayMillis must >= 0");
        }
        Task task = new Task();
        task.setTaskId(taskId);
        task.setTaskType(taskType);
        task.setTimeoutMillis(timeoutMillis);
        Timestamp time = new Timestamp(System.currentTimeMillis() + delayMillis);
        task.setAvailTime(time);
        task.setParams(params);

        Future<Void> future = service.submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                Connection connection = dataSource.getConnection();
                try {
                    String sql = String.format("INSERT INTO `%s` (task_type, task_id, params, avail_time, timeout_millis) VALUES (?, ?, ?, ?, ?)", tableName);
                    PreparedStatement preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setObject(1, task.getTaskType());
                    preparedStatement.setObject(2, task.getTaskId());
                    preparedStatement.setObject(3, task.getParams());
                    preparedStatement.setObject(4, task.getAvailTime());
                    preparedStatement.setObject(5, task.getTimeoutMillis());
                    preparedStatement.execute();
                    if (!connection.getAutoCommit()) {
                        connection.commit();
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("put task {}", JSON.toJSONString(task, true));
                    }
                } catch (Exception e) {
                    if (!connection.getAutoCommit()) {
                        connection.rollback();
                    }
                    throw e;
                } finally {
                    if (connection != null) {
                        connection.close();
                    }
                }
                return null;
            }
        });
        future.get();
    }

    @Override
    public TaskToken tryGet(String taskType) {
        Future<TaskToken> future = service.submit(new Callable<TaskToken>() {
            @Override
            public TaskToken call() throws Exception {
                Connection connection = dataSource.getConnection();
                try {
                    // 查出一条未超时的任务记录
                    // 条件更新holder值
                    String sql = String.format("SELECT * FROM `%s` WHERE task_type = ? AND status = ? AND avail_time < NOW() ORDER BY avail_time DESC", tableName);
                    PreparedStatement callableStatement = connection.prepareStatement(sql);
                    callableStatement.setObject(1, taskType);
                    callableStatement.setObject(2, 0);
                    callableStatement.setMaxRows(1);
                    callableStatement.execute();
                    ResultSet resultSet = callableStatement.getResultSet();
                    Task task = resultToTask(resultSet, Task.class);
                    if (LOG.isDebugEnabled()) {
                        // 优化JSON
                        LOG.debug("get task {}", JSON.toJSONString(task, true));
                    }
                    if (task == null) {
                        return null;
                    }
                    TaskToken taskToken = new TaskToken();
                    taskToken.setId(task.getId());
                    taskToken.setTaskId(task.getTaskId());
                    taskToken.setTaskType(task.getTaskType());
                    taskToken.setHolder(UUID.randomUUID().toString());

                    // 更新holder，版本，超时时间
                    sql = String.format("UPDATE `%s` SET holder = ?, version=version+1, avail_time = ?, start_time = NOW() WHERE id = ? AND version = ? ", tableName);
                    PreparedStatement updateStatement = connection.prepareStatement(sql);
                    updateStatement.setObject(1, taskToken.getHolder());
                    updateStatement.setObject(2, new Timestamp(System.currentTimeMillis() + task.getTimeoutMillis()));
                    updateStatement.setObject(3, task.getId());
                    updateStatement.setObject(4, task.getVersion());
                    int resultRows = updateStatement.executeUpdate();
                    if (!connection.getAutoCommit()) {
                        connection.commit();
                    }
                    if (resultRows > 0) {
                        return taskToken;
                    }
                } catch (Exception e) {
                    if (!connection.getAutoCommit()) {
                        connection.rollback();
                    }
                    throw e;
                } finally {
                    if (connection != null) {
                        connection.close();
                    }
                }
                return null;
            }
        });
        try {
            return future.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        } catch (InterruptedException e) {
            return null;
        }
    }

    private <T> T resultToTask(ResultSet resultSet, Class<T> clazz) throws SQLException, IllegalAccessException, InstantiationException {
        if (resultSet.next()) {
            int columnCount = resultSet.getMetaData().getColumnCount();
            T task = null;
            task = clazz.newInstance();
            for (int i = 1; i <= columnCount; i++) {
                String columnLabel = resultSet.getMetaData().getColumnLabel(i);
                String fieldName = ReflectionUtils.toCamel(columnLabel);
                Object value = resultSet.getObject(columnLabel);
                ReflectionUtils.setAttr(task, fieldName, value);
            }
            return task;
        } else {
            return null;
        }
    }

    @Override
    public TaskToken get(String taskType) throws InterruptedException {
        while (!stopped) {
            TaskToken taskToken = tryGet(taskType);
            if (taskToken != null) {
                return taskToken;
            }
            Thread.sleep(GET_INTERVAL);
        }
        throw new InterruptedException("taskpool stoped");
    }

    @Override
    public void commit(TaskToken task) throws TaskCommitException {
        if (task == null) {
            return;
        }
        Future<Void> future = service.submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                Connection connection = dataSource.getConnection();
                try {
                    String sql = String.format("UPDATE `%s` SET status = 1, finish_time = NOW() WHERE id = ? AND holder = ?", tableName);
                    PreparedStatement preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setObject(1, task.getId());
                    preparedStatement.setObject(2, task.getHolder());
                    int executeUpdate = preparedStatement.executeUpdate();
                    if (!connection.getAutoCommit()) {
                        connection.commit();
                    }
                    if (executeUpdate == 0) {
                        throw new TaskCommitException();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!connection.getAutoCommit()) {
                        connection.rollback();
                    }
                    throw e;
                } finally {
                    if (connection != null) {
                        connection.close();
                    }
                }
                return null;
            }
        });
        try {
            future.get();
        } catch (Exception e) {
            throw new TaskCommitException(e);
        }
    }

    @Override
    public void close() {
        stopped = true;
        service.shutdownNow();
    }
}
