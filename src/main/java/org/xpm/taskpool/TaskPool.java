package org.xpm.taskpool;

import org.xpm.taskpool.exception.TaskCommitException;

import java.util.concurrent.ExecutionException;

/**
 * Created by xupingmao on 2017/10/30.
 */
public interface TaskPool {

    /**
     * 添加任务
     * @param taskType
     * @param taskId taskId为空生成UUID
     * @param params 任务参数
     * @param timeoutMillis 任务超时时间
     * @param delayMillis 任务可获取的延迟时间
     */
    void put(String taskType, String taskId, String params, long timeoutMillis, long delayMillis) throws Exception;

    /**
     * 获取任务，生成一个唯一的token
     * 这是一个非阻塞的接口
     * @param taskType
     * @return
     */
    TaskToken tryGet(String taskType);

    /**
     * 获取任务，阻塞式接口，使用轮询的策略，轮询时间参考系统配置
     * @param taskType
     * @return
     */
    TaskToken get(String taskType) throws InterruptedException;

    /**
     * 提交任务，这里会帮助检查任务是否超时，是否被抢占，如果失败抛出异常
     * @param task
     * @throws TaskCommitException
     */
    void commit(TaskToken task) throws TaskCommitException;

    void close();
}