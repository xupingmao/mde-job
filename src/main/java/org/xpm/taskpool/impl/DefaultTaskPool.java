package org.xpm.taskpool.impl;

import com.alibaba.fastjson.JSON;
import org.xpm.taskpool.CreateTaskOption;
import org.xpm.taskpool.TaskToken;
import org.xpm.taskpool.exception.TaskCommitException;
import org.xpm.taskpool.Task;
import org.xpm.taskpool.TaskPool;
import org.xpm.taskpool.util.Utils;
import org.xpm.taskpool.exception.TaskRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.sql.DataSource;

import java.sql.*;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

/**
 *
 * Created with IDEA
 * @since 2017/10/30
 * @author xupingmao
 */
public class DefaultTaskPool implements TaskPool {

    private final DataSource dataSource;
    private ExecutorService service = Executors.newSingleThreadExecutor();

    private String tableName = TaskPoolConfig.getTableName();
    /** 轮询检查的时间分片，单位毫秒 */
    private long checkInterval = TaskPoolConfig.getGetInterval();
    /** 检查表结构 */
    private boolean checkTable = false;
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

    public void setCheckTable(boolean checkTable) {
        this.checkTable = checkTable;
    }

    public void init() throws ExecutionException, InterruptedException, SQLException {
        if (checkTable) {
            Set<String> columnNames = Utils.getColumnNames(Task.class);
            String sql = String.format("SELECT %s FROM %s LIMIT 1", Utils.join(columnNames, ","), tableName);
            Future<Task> submit = service.submit(new BaseCall<Task>(dataSource) {
                @Override
                public Task doCall(Connection connection) throws Exception {
                    PreparedStatement preparedStatement = connection.prepareStatement(sql);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    return Utils.resultSetToEntity(resultSet, Task.class);
                }
            });
            submit.get();
        }
    }

    @Override
    public Task put(CreateTaskOption createOption) throws Exception {
        if (createOption == null) {
            throw new IllegalArgumentException("createOption is null!");
        }
        if (createOption.getTimeoutMillis() == null) {
            throw new IllegalArgumentException("timeoutMillis can not be null!");
        }
        if (createOption.getTimeoutMillis() < 0) {
            throw new IllegalArgumentException("timeoutMillis can not bellow 0!");
        }
        if (createOption.getDelayMillis() != null && createOption.getDelayMillis() < 0) {
            throw new IllegalArgumentException("invalid delayMillis, can not bellow 0!");
        }

        if (createOption.getTaskId() == null) {
            createOption.setTaskId(Utils.getUid());
        }
        Future<Task> future = service.submit(new BaseCall<Task>(dataSource) {

            @Override
            public Task doCall(Connection connection) throws Exception {
                String sql = String.format("INSERT INTO `%s` (task_type, task_id, params, avail_time, timeout_millis, holder, start_time) VALUES (?, ?, ?, ?, ?, ?, ?)", tableName);
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                Timestamp availTime = new Timestamp(System.currentTimeMillis());
                Timestamp startTime = null;
                if (createOption.getTake()) {
                    availTime = new Timestamp(System.currentTimeMillis() + createOption.getTimeoutMillis());
                    createOption.setHolder(Utils.getUid());
                    startTime = new Timestamp(System.currentTimeMillis());
                }
                preparedStatement.setObject(1, createOption.getTaskType());
                preparedStatement.setObject(2, createOption.getTaskId());
                preparedStatement.setObject(3, createOption.getParams());
                preparedStatement.setObject(4, availTime);
                preparedStatement.setObject(5, createOption.getTimeoutMillis());
                preparedStatement.setObject(6, createOption.getHolder());
                preparedStatement.setObject(7, startTime);
                preparedStatement.execute();
                String lastInsertIdSql = "SELECT LAST_INSERT_ID()";
                PreparedStatement lastInsertIdStatement = connection.prepareStatement(lastInsertIdSql);
                lastInsertIdStatement.execute();
                ResultSet resultSet = lastInsertIdStatement.getResultSet();
                resultSet.next();
                long insertId = resultSet.getLong(1);
                Task task = new Task();
                task.setId(insertId);
                task.setTaskType(createOption.getTaskType());
                task.setTaskId(createOption.getTaskId());
                task.setHolder(createOption.getHolder());
                task.setAvailTime(availTime);
                task.setParams(createOption.getParams());
                if (logger.isDebugEnabled()) {
                    logger.debug("put task {}", JSON.toJSONString(task, true));
                }
                return task;
            }
        });
        return future.get();
    }

    @Override
    public Task put(String taskType, String params, long timeoutMillis) throws Exception {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must > 0");
        }
        CreateTaskOption createOption = new CreateTaskOption();
        createOption.setTaskType(taskType);
        createOption.setTimeoutMillis(timeoutMillis);
        createOption.setDelayMillis(0L);
        createOption.setParams(params);
        return put(createOption);
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
                    Task task = Utils.resultSetToEntity(resultSet, Task.class);
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
        try {
            Future<Void> future = service.submit(new BaseCall<Void>(dataSource) {

                @Override
                public Void doCall(Connection connection) throws Exception {
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
                    return null;
                }
            });
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
    public boolean cancel(Long id) {
        try {
            Future<Boolean> future = service.submit(new BaseCall<Boolean>(dataSource) {
                @Override
                public Boolean doCall(Connection connection) throws Exception {
                    // 更新其他字段
                    String sql = String.format("UPDATE `%s` SET status = -1, holder = 'canceled' WHERE id = ? AND status = 0", tableName);
                    PreparedStatement preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setObject(1, id);
                    int executeUpdate = preparedStatement.executeUpdate();
                    return executeUpdate > 0;
                }
            });
            return future.get();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void release(TaskToken token) throws TaskCommitException {
        innerCommit(token, false);
    }

    @Override
    public Task find(String taskType, String taskId) {
        Future<Task> future = null;
        try {
            future = service.submit(new BaseCall<Task>(dataSource) {
                @Override
                public Task doCall(Connection connection) throws Exception {
                    String sql = String.format("SELECT * FROM `%s` WHERE task_type = ? AND task_id = ?", tableName);
                    PreparedStatement preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setObject(1, taskType);
                    preparedStatement.setObject(2, taskId);
                    preparedStatement.executeQuery();
                    ResultSet resultSet = preparedStatement.getResultSet();
                    return Utils.resultSetToEntity(resultSet, Task.class);
                }
            });
            return future.get();
        } catch (Exception e) {
            throw new TaskRuntimeException(e);
        }
    }

    @Override
    public Task find(Long id) {
        Future<Task> future = null;
        try {
            future = service.submit(new BaseCall<Task>(dataSource) {
                @Override
                public Task doCall(Connection connection) throws Exception {
                    String sql = String.format("SELECT * FROM `%s` WHERE id = ?", tableName);
                    PreparedStatement preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setObject(1, id);
                    preparedStatement.executeQuery();
                    ResultSet resultSet = preparedStatement.getResultSet();
                    return Utils.resultSetToEntity(resultSet, Task.class);
                }
            });
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
