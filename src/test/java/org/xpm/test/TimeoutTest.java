package org.xpm.test;

import com.alibaba.fastjson.JSON;
import org.junit.Assert;
import org.junit.Test;
import org.xpm.taskpool.Task;
import org.xpm.taskpool.TaskToken;
import org.xpm.taskpool.exception.TaskCommitException;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by xupingmao on 2017/11/2.
 */
public class TimeoutTest extends AbstractTest {

    static final String taskType = "TIMEOUT";

    static class PlayThread implements Runnable {
        @Override
        public void run() {
            try {
                TaskToken taskToken = taskPool.get(taskType);
                logger.info("get task {}", JSON.toJSONString(taskToken));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static class WorkerThread implements Runnable {
        @Override
        public void run() {
            try {
                // wait for others
                Thread.sleep(100);
                TaskToken taskToken = taskPool.get(taskType);
                taskToken.setResult("Job done by WorkerThread");
                taskPool.commit(taskToken);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (TaskCommitException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void timeout() throws Exception {
        String taskId = UUID.randomUUID().toString();
        taskPool.put(taskType, taskId, null, 10L);
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        executorService.submit(new PlayThread());
        executorService.submit(new WorkerThread());
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        Task task = taskPool.find(taskType, taskId);
        logger.info("{}", JSON.toJSONString(task, true));
        Assert.assertEquals("Job done by WorkerThread", task.getResult());
    }

}
