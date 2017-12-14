package org.xpm.test.lock;

import org.xpm.taskpool.exception.TaskCommitException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.TransactionBlock;
import redis.clients.jedis.exceptions.JedisException;

import java.util.UUID;

/**
 * 基于Redis的锁，开发中
 * Created by xupingmao on 2017/11/9.
 */
@Deprecated
public class RedisLock {

    protected Jedis jedis;

    private String LUA_LOCK = "redis.call('get', '%s')";
    private String LUA_RELEASE = "";

    public RedisLock(Jedis jedis) {
        this.jedis = jedis;
    }

    public LockHolder tryLock(String key, int expire) {
        String uuid = UUID.randomUUID().toString();
        Long setnx = jedis.setnx(key, uuid);
        if (setnx > 0) {
            jedis.expire(key, expire);
            return new LockHolder(key, uuid);
        }
        return null;
    }

    public void commit(LockHolder lockHolder) throws TaskCommitException {
        if (lockHolder == null) {
            throw new TaskCommitException("lockHolder is null");
        }
        String key = lockHolder.getKey();

        jedis.watch(key);
        Transaction multi = jedis.multi();
        Response<String> stringResponse = multi.get("");
        stringResponse.get();
        multi.exec();

        String holder = jedis.get(key);
        if (key.equals(holder)) {
            // TODO 这里可能出现的问题是瞬间锁失效了，如果删除会有问题
            // 已知的方案
            // 1. 使用lua脚本来完成条件更新
            // 2. 设置一个安全期，在安全期内才去执行删除锁操作（不是完全可靠）
            // 3. getset可以探知之前的holder，需要考虑回滚操作和对占用锁的对象的再处理，比较复杂
        } else {
            throw new TaskCommitException("lockHolder is lost");
        }
    }

    public void close() {
        if (jedis != null) {
            jedis.close();
        }
    }

}
