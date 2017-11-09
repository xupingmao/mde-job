package org.xpm.taskpool.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by xupingmao on 2017/11/9.
 */
public class DBUtils {

    public static Map<String, Object> getResultMap(ResultSet resultSet) throws SQLException {
        int columnCount = resultSet.getMetaData().getColumnCount();
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < columnCount; i++) {
            String key = resultSet.getMetaData().getColumnLabel(i+1);
            Object object = resultSet.getObject(i+1);
            map.put(key, object);
        }
        return map;
    }
}
