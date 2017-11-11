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

    public static <T> T resultSetToEntity(ResultSet resultSet, Class<T> clazz) throws SQLException, IllegalAccessException, InstantiationException {
        if (resultSet.next()) {
            int columnCount = resultSet.getMetaData().getColumnCount();
            T task = null;
            task = clazz.newInstance();
            for (int i = 1; i <= columnCount; i++) {
                String columnLabel = resultSet.getMetaData().getColumnLabel(i);
                String fieldName = ReflectionUtils.toCamel(columnLabel);
                Object value = resultSet.getObject(columnLabel);
                ReflectionUtils.setAttr(task, fieldName, value);
            }
            return task;
        } else {
            return null;
        }
    }
}
