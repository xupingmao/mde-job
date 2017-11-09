package org.xpm.taskpool.exception;

/**
 * Created by xupingmao on 2017/11/9.
 */
public class TaskRuntimeException extends RuntimeException {

    public TaskRuntimeException(Exception e) {
        super(e);
    }

}
