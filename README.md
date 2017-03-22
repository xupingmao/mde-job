# DbQueue
Queue based on database

基于数据库的可靠任务队列

# 特性
- 可靠性，任务会保持执行到成功或者超过重试次数
- 延迟执行，可以延迟执行任务
- 并发控制，保证任务在超时之前不被并发执行

# 表设计

字段名    |  类型   | 说明
---------|---------|----------
id       |  varchar(36) | UUID
version  | int          | 乐观锁，用来抢占任务
timeout  |  datetime   | 超时时间
execute_time |datetime  | 执行时间
task_id   |  text   | 任务主键
task_type |  varchar(36) | 任务类型
retry_times | int  | 重试次数
params      | varchar(4096) | 其他参数
status      | int   | 任务状态，0是未执行，1是执行成功，负数执行异常
