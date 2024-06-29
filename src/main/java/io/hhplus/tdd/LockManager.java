package io.hhplus.tdd;


import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class LockManager {
    private static ConcurrentMap<Object, ReentrantLock> lockMap = new ConcurrentHashMap<>();
    private static Lock lock;

    public static void lock(Object object) {
        lock = lockMap.get(object);
        if (lock == null) {
            lock = new ReentrantLock();
            lockMap.put(object, (ReentrantLock) lock);
        }
        lock.lock();
//        lock = lockMap.computeIfAbsent(object, k -> new ReentrantLock());
//        lock.lock();
    }

    public static void unLock(Object object) {
       lock = lockMap.get(object);
       if(lock != null) lock.unlock();
    }
}
