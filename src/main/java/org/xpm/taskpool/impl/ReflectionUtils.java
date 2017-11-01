package org.xpm.taskpool.impl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by xupingmao on 2017/11/1.
 */
public class ReflectionUtils {
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

    public static String toCamel(String name) {
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
}
