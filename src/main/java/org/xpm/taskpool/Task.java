package org.xpm.taskpool;

import javax.persistence.Column;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Created by xupingmao on 2017/10/30.
 */
public class Task implements Serializable {

    private Long id;

    /** 0初始化状态,1执行成功,-1异常（重试n次依然失败）*/
    private int status;

    @Column(name = "version")
    private Long version;

    /** 持有者token， UUID */
    private String holder;

    private String taskId;

    private String taskType;

    private Long timeoutMillis;

    private Timestamp availTime;

    /** 开始处理时间 */
    private Timestamp startTime;
    /** 结束时间 */
    private Timestamp finishTime;

    /** 尝试次数 */
    private int retryTimes;
    /** 参数 */
    private String params;
    /** 结果 */
    private String result;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getHolder() {
        return holder;
    }

    public void setHolder(String holder) {
        this.holder = holder;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public Long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(Long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public Timestamp getAvailTime() {
        return availTime;
    }

    public void setAvailTime(Timestamp availTime) {
        this.availTime = availTime;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Timestamp finishTime) {
        this.finishTime = finishTime;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
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
