package org.xpm.test;

import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.xpm.taskpool.TaskToken;
import org.xpm.taskpool.exception.TaskCommitException;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by xupingmao on 2017/11/1.
 */
public class TaskPoolAbstractTest extends AbstractTest {

    @Test
    public void putTask() throws Exception {
        taskPool.put("TEST", "TEST_" + new Date().toGMTString(), null, 6000L, 0L);
    }

    static class Consumer implements Runnable {
        @Override
        public void run() {
            try {
                TaskToken taskToken = taskPool.tryGet("TEST");
                System.out.println(Thread.currentThread().getName() + ":" + JSON.toJSONString(taskToken, true));
                System.out.println("Do the Job");
                taskPool.commit(taskToken);
            } catch (TaskCommitException e) {
                e.printStackTrace();
            }
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
