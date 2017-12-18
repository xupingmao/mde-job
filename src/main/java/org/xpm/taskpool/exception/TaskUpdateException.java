package org.xpm.taskpool.exception;

/**
 * Created by xupingmao on 2017/12/14.
 */
public class TaskUpdateException extends Exception {
    public TaskUpdateException() {
        super();
    }

    public TaskUpdateException(String message) {
        super(message);
    }

    public TaskUpdateException(Throwable cause) {
        super(cause);
    }
}
