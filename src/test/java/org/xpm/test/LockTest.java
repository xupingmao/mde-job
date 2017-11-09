package org.xpm.test;

import org.junit.Assert;
import org.junit.Test;
import org.xpm.taskpool.Task;
import org.xpm.taskpool.TaskToken;
import org.xpm.taskpool.exception.TaskCommitException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by xupingmao on 2017/11/9.
 */
public class LockTest extends AbstractTest {


    static class LockRunnable implements Runnable {

        private String name;
        public LockRunnable(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            try {
                TaskToken taskToken = taskPool.get("Lock");
                String result = taskToken.getResult() + " " + name;
                taskToken.setResult(result);
                taskPool.release(taskToken);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (TaskCommitException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void lockAndRelease() throws Exception {
        Task lock = taskPool.find("Lock", "001");
        if (lock == null) {
            taskPool.put("Lock", "001", null, 100L, 0L);
        }

        TaskToken taskToken = taskPool.get("Lock");
        taskToken.setResult("");
        taskPool.release(taskToken);

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        executorService.submit(new LockRunnable("lock1"));
        executorService.submit(new LockRunnable("lock2"));
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        taskToken = taskPool.get("Lock");
        Assert.assertTrue(taskToken.getResult().contains("lock1"));
        Assert.assertTrue(taskToken.getResult().contains("lock2"));
    }

}
