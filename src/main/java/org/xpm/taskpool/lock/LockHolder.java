package org.xpm.taskpool.lock;

/**
 * Created by xupingmao on 2017/11/9.
 */
public class LockHolder {

    private final String key;
    private final String holder;

    public LockHolder(String key, String holder) {
        this.key = key;
        this.holder = holder;
    }

    public String getHolder() {
        return holder;
    }

    public String getKey() {
        return key;
    }
}
