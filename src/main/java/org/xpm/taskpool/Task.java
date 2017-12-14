package org.xpm.taskpool;

import javax.persistence.Column;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Created by xupingmao on 2017/10/30.
 */
public class Task implements Serializable {

    @Column(name = "id")
    private Long id;

    /** 0初始化状态,1执行成功,-1异常（重试n次依然失败）*/
    @Column(name = "status")
    private Integer status;

    @Column(name = "version")
    private Long version;

    /** 持有者token， UUID */
    @Column(name = "holder")
    private String holder;

    @Column(name = "task_id")
    private String taskId;

    @Column(name = "task_type")
    private String taskType;

    @Column(name = "timeout_millis")
    private Long timeoutMillis;

    @Column(name = "avail_time")
    private Timestamp availTime;

    /** 开始处理时间 */
    @Column(name = "start_time")
    private Timestamp startTime;

    /** 结束时间 */
    @Column(name = "finish_time")
    private Timestamp finishTime;

    /** 尝试次数 */
    @Column(name = "retry_times")
    private Integer retryTimes;

    /** 参数 */
    @Column(name = "params")
    private String params;

    /** 结果 */
    @Column(name = "result")
    private String result;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
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

    public Integer getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(Integer retryTimes) {
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
