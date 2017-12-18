package org.xpm.taskpool.exception;

/**
 * Created by xupingmao on 2017/10/30.
 */
public class TaskCommitException extends TaskUpdateException {

    public TaskCommitException() {
        super();
    }

    public TaskCommitException(String message) {
        super(message);
    }

    public TaskCommitException(Throwable cause) {
        super(cause);
    }
}
