package org.xpm.test;

import com.alibaba.fastjson.JSON;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xpm.taskpool.DataSource;
import org.xpm.taskpool.TaskToken;
import org.xpm.taskpool.exception.TaskCommitException;
import org.xpm.taskpool.impl.DefaultTaskPool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by xupingmao on 2017/11/1.
 */
public class TaskPoolTest {

    private static java.sql.Connection connection;
    private java.sql.PreparedStatement preparedStatement;

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


    @Test
    public void submitTasks() throws Exception {
        taskPool.put("TEST", "TEST_" + new Date().toGMTString(), null, 0L, 0L);
    }

    static class Consumer implements Runnable {
        @Override
        public void run() {
            TaskToken taskToken = taskPool.tryGet("TEST");
            System.out.println(Thread.currentThread().getName() + ":" + JSON.toJSONString(taskToken, true));
        }
    }

    @Test
    public void consumeTask() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        executorService.submit(new Consumer());
        executorService.submit(new Consumer());
        executorService.submit(new Consumer());
        executorService.submit(new Consumer());
        executorService.submit(new Consumer());

        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void consumeAndCommit() throws TaskCommitException {
        TaskToken taskToken = taskPool.tryGet("TEST");
        if (taskToken != null) {
            System.out.println("Process task " + JSON.toJSONString(taskToken, true));
            taskPool.commit(taskToken);
        }
    }
}
