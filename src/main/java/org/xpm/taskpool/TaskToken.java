package org.xpm.taskpool;

import java.io.Serializable;

/**
 * 用于提交task的token，相当于数据库事务的会话
 * Created by xupingmao on 2017/10/30.
 */
public class TaskToken implements Serializable {

    private Long id;
    private String taskType;
    private String taskId;
    private String holder;
    private String params;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public void setHolder(String holder) {
        this.holder = holder;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }
}
