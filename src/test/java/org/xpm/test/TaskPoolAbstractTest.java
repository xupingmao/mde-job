package org.xpm.test;

import com.alibaba.fastjson.JSON;
import org.junit.Assert;
import org.junit.Test;
import org.xpm.taskpool.TaskToken;
import org.xpm.taskpool.exception.TaskCommitException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xupingmao on 2017/11/1.
 */
public class TaskPoolAbstractTest extends AbstractTest {

    public void putTask(String type) throws Exception {
        String dateTime = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        taskPool.put(type, "TEST_" + dateTime, null, 6000L, 0L);
    }

    static class Consumer implements Runnable {
        private AtomicInteger counter;
        public Consumer(AtomicInteger counter) {
            this.counter = counter;
        }

        @Override
        public void run() {
            try {
                TaskToken taskToken = taskPool.tryGet("Consumer-Test");
                if (taskToken == null) {
                    System.out.println("No job to do, quit");
                    return;
                }
                counter.getAndIncrement();
                System.out.println(Thread.currentThread().getName() + ":" + JSON.toJSONString(taskToken, true));
                System.out.println("Do the Job");
                taskPool.commit(taskToken);
            } catch (TaskCommitException e) {
                e.printStackTrace();
                counter.decrementAndGet();
            }
        }
    }

    @Test
    public void consumeTask() throws Exception {
        putTask("Consumer-Test");
        AtomicInteger counter = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 20; i++) {
            executorService.submit(new Consumer(counter));
        }

        executorService.awaitTermination(10, TimeUnit.SECONDS);
        Assert.assertEquals(1, counter.get());
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
