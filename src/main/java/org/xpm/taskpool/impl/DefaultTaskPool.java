package org.xpm.taskpool.impl;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.sql.DataSource;
import org.xpm.taskpool.Task;
import org.xpm.taskpool.TaskPool;
import org.xpm.taskpool.TaskToken;
import org.xpm.taskpool.exception.TaskCommitException;
import org.xpm.taskpool.exception.TaskRuntimeException;
import org.xpm.taskpool.util.DBUtils;
import org.xpm.taskpool.util.ReflectionUtils;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Created by xupingmao on 2017/10/30.
 */
public class DefaultTaskPool implements TaskPool {

    private final DataSource dataSource;
    private ExecutorService service = Executors.newSingleThreadExecutor();
    private String tableName = TaskPoolConfig.getTableName();
    private long checkInterval = TaskPoolConfig.getGetInterval();
    private volatile boolean stopped = false;
    private Logger logger = LoggerFactory.getLogger(DefaultTaskPool.class);

    public DefaultTaskPool(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setCheckInterval(long checkInterval) {
        this.checkInterval = checkInterval;
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
                    if (logger.isDebugEnabled()) {
                        logger.debug("put task {}", JSON.toJSONString(task, true));
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

    public TaskToken innerTryGet(String taskType, String taskId) {
        Future<TaskToken> future = service.submit(new Callable<TaskToken>() {
            @Override
            public TaskToken call() throws Exception {
                Connection connection = dataSource.getConnection();
                try {
                    // 查出一条未超时的任务记录
                    // 条件更新holder值
                    PreparedStatement preparedStatement = null;
                    String sql = null;
                    if (taskId == null) {
                        sql = String.format("SELECT * FROM `%s` WHERE task_type = ? AND status = 0 AND avail_time < NOW() ORDER BY avail_time DESC", tableName);
                        preparedStatement = connection.prepareStatement(sql);
                        preparedStatement.setObject(1, taskType);
                    } else {
                        sql = String.format("SELECT * FROM `%s` WHERE task_type = ? AND task_id = ? AND status = 0 AND avail_time < NOW() ORDER BY avail_time DESC", tableName);
                        preparedStatement = connection.prepareStatement(sql);
                        preparedStatement.setObject(1, taskType);
                        preparedStatement.setObject(2, taskId);
                    }
                    preparedStatement.setMaxRows(1);
                    preparedStatement.execute();
                    ResultSet resultSet = preparedStatement.getResultSet();
                    Task task = DBUtils.resultSetToEntity(resultSet, Task.class);
                    if (logger.isDebugEnabled()) {
                        // 优化JSON
                        logger.debug("get task {}", JSON.toJSONString(task, true));
                    }
                    if (task == null) {
                        return null;
                    }
                    String uuid = UUID.randomUUID().toString();
                    TaskToken taskToken = new TaskToken(task.getId(), uuid);
                    taskToken.setTaskId(task.getTaskId());
                    taskToken.setTaskType(task.getTaskType());
                    taskToken.setParams(task.getParams());
                    taskToken.setResult(task.getResult());

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
                    } else {
                        logger.debug("fail to take the job");
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

    @Override
    public TaskToken tryGet(String taskType) {
        return innerTryGet(taskType, null);
    }

    @Override
    public TaskToken get(String taskType) throws InterruptedException {
        while (!stopped) {
            TaskToken taskToken = tryGet(taskType);
            if (taskToken != null) {
                return taskToken;
            }
            Thread.sleep(checkInterval);
        }
        throw new InterruptedException("taskpool stopped");
    }

    @Override
    public TaskToken tryLock(String lockType, String lockId) {
        return innerTryGet(lockType, lockId);
    }

    @Override
    public TaskToken lock(String lockType, String id) throws InterruptedException {
        while (!stopped) {
            TaskToken taskToken = tryLock(lockType, id);
            if (taskToken != null) {
                return taskToken;
            }
            Thread.sleep(checkInterval);
        }
        throw new InterruptedException("taskpool stopped");
    }

    public void innerCommit(TaskToken task, boolean updateStatus) throws TaskCommitException {
        if (task == null) {
            return;
        }
        Future<Void> future = service.submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                Connection connection = dataSource.getConnection();
                try {
                    // 更新其他字段
                    String sql = String.format("UPDATE `%s` SET status = 1, finish_time = NOW(), result = ? WHERE id = ? AND holder = ?", tableName);
                    if (!updateStatus) {
                        // 不更新状态，用于分布式锁
                        // 更新可用时间
                        sql = String.format("UPDATE `%s` SET finish_time = NOW(), avail_time = NOW(), result = ? WHERE id = ? AND holder = ?", tableName);
                    }
                    PreparedStatement preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setObject(1, task.getResult());
                    preparedStatement.setObject(2, task.getId());
                    preparedStatement.setObject(3, task.getHolder());
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
    public void commit(TaskToken task) throws TaskCommitException {
        innerCommit(task, true);
    }

    @Override
    public void release(TaskToken token) throws TaskCommitException {
        innerCommit(token, false);
    }

    @Override
    public Task find(String taskType, String taskId) {
        Future<Task> future = service.submit(new Callable<Task>() {

            @Override
            public Task call() throws Exception {
                Connection connection = dataSource.getConnection();
                try {
                    String sql = String.format("SELECT * FROM `%s` WHERE task_type = ? AND task_id = ?", tableName);
                    PreparedStatement preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setObject(1, taskType);
                    preparedStatement.setObject(2, taskId);
                    preparedStatement.executeQuery();
                    ResultSet resultSet = preparedStatement.getResultSet();
                    return DBUtils.resultSetToEntity(resultSet, Task.class);
                } finally {
                    if (connection != null) {
                        connection.close();
                    }
                }
            }
        });
        try {
            return future.get();
        } catch (Exception e) {
            throw new TaskRuntimeException(e);
        }
    }

    @Override
    public void close() throws InterruptedException {
        stopped = true;
        service.awaitTermination(10, TimeUnit.SECONDS);
    }
}
