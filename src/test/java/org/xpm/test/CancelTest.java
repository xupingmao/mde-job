package org.xpm.test;

import org.junit.Assert;
import org.junit.Test;
import org.xpm.taskpool.Task;
import org.xpm.taskpool.TaskToken;
import org.xpm.taskpool.exception.TaskCommitException;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by xupingmao on 2017/12/14.
 */
public class CancelTest extends AbstractTest {

    @Test(expected = TaskCommitException.class)
    public void cancel() throws Exception {
        String taskType = "cancelTest";
        Long id;

        taskPool.put(taskType, null, 1000*60);
        TaskToken token = taskPool.get(taskType);
        log(token);
        id = token.getId();
        taskPool.cancel(id);
        Task task = taskPool.find(id);
        Assert.assertEquals(Integer.valueOf(-1), task.getStatus());
        taskPool.commit(token);
    }

    @Test
    public void cancelFailed() throws Exception {
        String taskType = "cancelTest";
        Long id;

        taskPool.put(taskType, null, 1000*60);
        TaskToken token = taskPool.get(taskType);
        token.setResult("executed successfully");
        taskPool.commit(token);

        log(token);
        id = token.getId();
        boolean cancelResult = taskPool.cancel(id);
        Task task = taskPool.find(id);
        Assert.assertFalse(cancelResult);
        Assert.assertEquals(Integer.valueOf(1), task.getStatus());
    }

}
