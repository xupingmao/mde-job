# TaskPool
基于数据库的分布式可靠任务池

# 特性
- 分布式，支持分布式系统
- 可靠性，仿照数据库事务，一个任务只能被一个处理程序成功处理提交
- 延迟任务，可以延迟任务的获取
- 易用性，简单直观的API


# 表设计

## 任务表(task\_pool)

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
result      | text | 任务执行结果

# API设计

## TaskToken
- taskType
- taskId
- params
- holder
- result

## TaskPool

- `void put` 添加新任务，delayMillis延迟获取时间，timeoutMillis超时时间
- `TaskToken get` 获取任务，阻塞方式
- `TaskToken tryGet` 获取任务，非阻塞方式
- `TaskToken tryLock` 获取分布式锁，非阻塞方式
- `TaskToken lock` 获取分布式锁，阻塞方式
- `TaskToken release` 释放锁
- `void commit` 提交任务，失败抛出异常
- `void close` 关闭任务池

# 调用方式

- 参考测试用例

