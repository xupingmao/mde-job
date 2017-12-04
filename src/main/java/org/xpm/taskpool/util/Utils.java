package org.xpm.taskpool.util;

import javafx.scene.control.TableColumn;
import org.xpm.taskpool.Task;

import javax.persistence.Column;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by xupingmao on 2017/11/9.
 */
public class Utils {

    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static String join(Collection<String> list, String separator) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            String item = iterator.next();
            sb.append(item);
            if (iterator.hasNext()) {
                sb.append(separator);
            }
        }
        return sb.toString();
    }

    /**
     * 获取对象属性
     * @param obj
     * @param name
     * @param type
     * @param <E>
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public <E> E getAttr(Object obj, String name, Class<E> type) throws Exception {
        Field target = obj.getClass().getDeclaredField(name);
        if (target == null) {
            return null;
        }
        if (!target.isAccessible()) {
            target.setAccessible(true);
        }
        return (E) target.get(obj);
    }

    /**
     * 设置对象属性
     * @param instance
     * @param fieldName
     * @param value
     */
    public static void setAttr(Object instance, String fieldName, Object value) {
        if (value == null) {
            return;
        }
        Class cl = instance.getClass();
        try {
            while (cl != null) {
                Field[] fields = cl.getDeclaredFields(); // cl.getField(fieldName)和cl.getFields()默认只取public的属性
                for (Field field : fields) {
                    // base.isAssignableFrom(inherits)
                    if (field.getName().equals(fieldName)
                            && field.getType().isAssignableFrom(value.getClass())) {
                        field.setAccessible(true);
                        field.set(instance, value);
                        return;
                    }
                }
                cl = cl.getSuperclass();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取方法堆栈，不包括ReflectionUtils中的本方法，当前函数是第一个元素
     * @param limit 返回的堆栈限制，为0不限制
     * @return
     */
    public static List<StackTraceElement> getStackTraceList(int limit) {
        StackTraceElement[] stackTrace = new Exception().getStackTrace();
        List<StackTraceElement> traceList = new ArrayList<StackTraceElement>(stackTrace.length-1);
        if (limit <= 0) {
            limit = stackTrace.length;
        }
        // 第一个是本函数
        for (int i = 1; i < stackTrace.length && i <= limit; i++) {
            StackTraceElement element = stackTrace[i];
            traceList.add(element);
        }
        return traceList;
    }

    public static String getFormattedStackTrace(int limit) {
        List<StackTraceElement> stackTraceList = getStackTraceList(limit);
        StringBuilder sb = new StringBuilder();
        for (int scope = stackTraceList.size()-1; scope >= 0; scope--) {
            StackTraceElement element = stackTraceList.get(scope);
            String className = element.getClassName();
            String methodName = element.getMethodName();
            String fileName = element.getFileName();
            int    lineNumber = element.getLineNumber();
            sb.append(className);
            sb.append(".");
            sb.append(methodName);
            sb.append("(");
            sb.append(fileName);
            sb.append(":");
            sb.append(lineNumber);
            sb.append(")\n");
        }
        return sb.toString();
    }


    /**
     * 生成JDK动态代理
     * @param interfaceType
     * @param handler
     * @param <T>
     * @return
     */
    public static <T> T newProxy(Class<T> interfaceType, InvocationHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null!");
        }
        if (!interfaceType.isInterface()) {
            throw new IllegalArgumentException("interfaceType must be interface!");
        }
        Object object =
                Proxy.newProxyInstance(
                        interfaceType.getClassLoader(), new Class<?>[] {interfaceType}, handler);
        return interfaceType.cast(object);
    }

    public static String toCamelName(String name) {
        boolean upper = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_') {
                upper = true;
            } else if (upper) {
                sb.append(Character.toUpperCase(c));
                upper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String toUnderscoreName(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append("_").append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

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
                String fieldName = toCamelName(columnLabel);
                Object value = resultSet.getObject(columnLabel);
                setAttr(task, fieldName, value);
            }
            return task;
        } else {
            return null;
        }
    }

    public static Set<String> getColumnNames(Class clazz) {
        if (clazz == null || Object.class.equals(clazz)) {
            return Collections.emptySet();
        }
        Field[] fields = clazz.getDeclaredFields();
        Set<String> names = new HashSet<>();
        for (Field field: fields) {
            Column annotation = field.getAnnotation(Column.class);
            if (annotation != null) {
                if (isNotEmpty(annotation.name())) {
                    names.add(annotation.name());
                } else {
                    names.add(toUnderscoreName(field.getName()));
                }
            }
        }
        names.addAll(getColumnNames(clazz.getSuperclass()));
        return names;
    }
}
