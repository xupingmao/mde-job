# DbQueue
Distributed queue based on database

基于数据库的分布式可靠任务队列

# 特性
- 分布式，支持分布式系统
- 可靠性，任务会保持执行到成功或者超过重试次数
- 延迟执行，可以延迟执行任务
- 并发控制，保证任务在超时之前不被并发执行
- 独占性，支持阻塞的方式取出任务

# 表设计

## 任务表(prefix_job)

字段名    |  类型   | 说明
---------|---------|----------
id       |  varchar(36) | UUID
version  | bigint(20)   | 乐观锁，用来抢占任务
timeout  |  datetime   | 超时时间
start_time |datetime  | 开始执行时间
finish_time | datetime | 执行结束时间
task_id   |  varchar(36)   | 任务主键
task_type |  varchar(36) | 任务类型
retry_times | int  | 重试次数
params      | text | 任务参数
status      | int   | 任务状态，0是未执行，1是执行中，2执行成功，-1执行异常

## 锁表(prefix_lock)

字段名    |  类型   | 说明
---------|---------|----------
id       | varchar(36) | UUID
version  | bigint(20)  | 乐观锁
task_type | varchar(36) | 任务类型
lock_holder    | varchar(255) | 持有锁的对象, 机器名+线程名
lock_timeout   | datetime     | 锁超时时间


# API设计

## DbQueue

- `void put(String taskId, String params)` 添加新任务
- `Task get(boolean blocking)` 获取任务，可能为空，blocking为true时只允许一个线程获取任务，blocking为false允许并发获取任务
- `void commit(String taskId)` 提交任务，失败抛出异常

