package org.xpm.taskpool.impl;

/**
 * Created by xupingmao on 2017/11/1.
 */
public class TaskPoolConfig {
    /**

     CREATE TABLE `task_pool` (
     `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '无意义主键',
     `status` int(11) DEFAULT '0',
     `version` bigint(20) DEFAULT '0' COMMENT '乐观锁版本',
     `holder` varchar(36) DEFAULT NULL COMMENT '任务持有者',
     `task_id` varchar(36) DEFAULT NULL,
     `task_type` varchar(36) DEFAULT NULL,
     `timeout_millis` bigint(20) DEFAULT '0' COMMENT '超时毫秒数',
     `avail_time` datetime DEFAULT NULL COMMENT '可用时间',
     `start_time` datetime DEFAULT NULL,
     `finish_time` datetime DEFAULT NULL,
     `retry_times` int(11) DEFAULT '0',
     `params` text,
     `result` text,
     PRIMARY KEY (`id`),
     KEY `idx_task_id` (`task_id`),
     KEY `idx_task_type` (`task_type`),
     KEY `idx_avail_time` (`avail_time`)
     ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

     */

    private static int GET_INTERVAL = 50;
    private static String tableName = "task_pool";

    public static int getGetInterval() {
        return GET_INTERVAL;
    }

    public static String getTableName() {
        return tableName;
    }
}
