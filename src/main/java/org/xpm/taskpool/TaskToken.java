package org.xpm.taskpool;

import java.io.Serializable;

/**
 * 用于提交task的token，相当于数据库事务的会话
 * Created by xupingmao on 2017/10/30.
 */
public class TaskToken implements Serializable {

    private final Long id;
    private final String holder;
    private String taskType;
    private String taskId;
    private String params;
    private String result;

    public TaskToken(Long id, String holder) {
        this.id = id;
        this.holder = holder;
    }

    public Long getId() {
        return id;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getHolder() {
        return holder;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
