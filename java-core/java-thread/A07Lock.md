2020年5月22日 10:38:33

### 1. 锁简介

锁：Lock接口，ReentrantLock，ReentrantReadWriteLock。可重入锁又名递归锁，是指 同一个线程在外层方法获取了锁，在进入内层方法会自动获取锁。ReentrantReadWriteLock 其写锁是独享锁，其读锁是共享锁。读锁是共享锁使得并发读是非常高效的,读写,写读,写写的过程是互斥的。ReentrantReadWriteLock大多数场景下，读操作比写操作频繁，只要保证每个线程都能读取到最新数据，并且在读数据时不会有其它线程在修改数据，那么就不会出现线程安全问题。这种策略减少了互斥同步，自然也提升了并发性能，ReentrantReadWriteLock 就是这种策略的具体实现。允许多个读操作并发执行，但每次只允许一个写操作。



```java
// 使用可重入锁实现一个多线程缓存
static class UnboundedCache<K,V>{
    private final Map<K,V> map = new WeakHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public boolean put(K key, V value){
        //            写，只能独享锁，单线程操作
        lock.writeLock().lock();
        try {
            map.put(key, value);
            System.out.println("put "+key+" : "+value);
        } finally {
            lock.writeLock().unlock();
        }
        return true;
    }

    public V get(K key){
        //          读，可共享锁，多线程操作
        lock.readLock().lock();
        V val;
        try{
            val = map.get(key);
            System.out.println("get: "+val);
        } finally {
            lock.readLock().unlock();
        }
        return val;
    }

    public boolean remove(K key){
        lock.writeLock().lock();
        try{
            map.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
        return true;
    }

    public boolean clear(){
        lock.writeLock().lock();
        try{
            map.clear();
        } finally {
            lock.writeLock().unlock();
        }
        return true;
    }
}
```



### 2. Condition版生产者消费者模式

```java
public class A12ConditionCP {
    private static Lock lock = new ReentrantLock();
    public static Queue<Integer> queue = new PriorityQueue<>();
    private static Condition isEmpty = lock.newCondition();
    private static Condition isFull = lock.newCondition();
    private static volatile boolean isClose = false;

    public static void main(String[] args) throws InterruptedException {
        new Thread(new Producer()).start();
        new Thread(new Customer("AAA")).start();
        new Thread(new Customer("BBB")).start();
        new Thread(new Customer("CCC")).start();

        Thread.sleep(100);
        isClose = true;
        System.out.println("END : queue size = "+queue.size());
    }

    static class Producer implements Runnable{
        String name = "produce";
        @Override
        public void run() {
            while(!isClose){
                lock.lock();
                try {
                    while(queue.size()==3){
                        try {
                            isFull.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            isEmpty.signal();
                        }
                    }
                    queue.add(new Random().nextInt(10));
                    System.out.println(name+" produce 1");
                    isEmpty.signal();
                }finally {
                    lock.unlock();
                }
            }
        }
    }
    static class Customer implements Runnable{
        String name;
        public Customer(String name){
            this.name = name;
        }
        @Override
        public void run() {
            while(!isClose){
                lock.lock();
                try{
                    while(queue.size()==0){
                        try {
                            isEmpty.await();
                        } catch (InterruptedException e) {
                            isFull.signal();
                        }
                    }
                    queue.poll();
                    System.out.println(name+" queue poll");
                    isFull.signal();
                } finally {
                    lock.unlock();
                }
            }
        }
    }
}
```