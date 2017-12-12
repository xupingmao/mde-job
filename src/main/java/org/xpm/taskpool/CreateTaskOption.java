package org.xpm.taskpool;

/**
 * Created by xupingmao on 2017/12/6.
 */
public class CreateTaskOption {

    private String taskType;
    private String taskId;
    private String params;
    private Long timeoutMillis;
    private Long delayMillis;
    private String holder;
    /** 是否占有 */
    private boolean take;

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

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public Long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(Long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public Long getDelayMillis() {
        return delayMillis;
    }

    public void setDelayMillis(Long delayMillis) {
        this.delayMillis = delayMillis;
    }

    public boolean getTake() {
        return take;
    }

    public void setTake(boolean take) {
        this.take = take;
    }

    public String getHolder() {
        return holder;
    }

    public void setHolder(String holder) {
        this.holder = holder;
    }
}
