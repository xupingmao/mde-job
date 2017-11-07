# TaskPool
基于数据库的分布式可靠任务池

# 特性
- 分布式，支持分布式系统
- 可靠性，仿照数据库事务，一个任务只能被一个处理程序成功处理提交
- 延迟任务，可以延迟任务的获取
- 易用性，简单直观的API


# 表设计

## 任务表(prefix_task_pool)

字段名    |  类型   | 说明
---------|---------|----------
id       |  varchar(36) | UUID
version  | bigint(20)   | 乐观锁，用来抢占任务
status   | int          | 任务状态，0是未执行，1是执行成功，-1执行异常
timeout  |  bigint(20)   | 超时时间毫秒数
avail_time | datetime | 任务可用时间
start_time |datetime  | 开始执行时间
finish_time | datetime | 执行结束时间
task_id   |  varchar(36)   | 任务主键
task_type |  varchar(36) | 任务类型
holder    |  varchar(36)  | 任务持有者
retry_times | int  | 重试次数
params      | text | 任务参数

## 锁表(prefix_lock)

字段名    |  类型   | 说明
---------|---------|----------
id       | varchar(36) | UUID
version  | bigint(20)  | 乐观锁
task_type | varchar(36) | 任务类型
lock_holder    | varchar(255) | 持有锁的对象, 机器名+线程名
lock_timeout   | datetime     | 锁超时时间


# API设计

## TaskToken
- taskType
- taskId
- params
- holder

## TaskPool

- `void put` 添加新任务，delayMillis延迟获取时间
- `TaskToken get` 获取任务阻塞接口
- `TaskToken tryGet` 获取任务非阻塞接口
- `void commit` 提交任务，失败抛出异常



