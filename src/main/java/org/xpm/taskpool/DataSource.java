package org.xpm.taskpool;

import java.sql.Connection;

/**
 * Created by xupingmao on 2017/11/1.
 */
public interface DataSource {
    Connection getConnection();
}
