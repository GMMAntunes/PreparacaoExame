import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * EXAME PC 2021V_2
 */

public class NAryMessageQueue<T> {

    private final List<T> messages;
    private final Lock lock = new ReentrantLock();
    private AtomicInteger count;
    private final List<GetQueue> getQueue;

    public NAryMessageQueue() {
        this.messages = new LinkedList<>();
        this.count = new AtomicInteger();
        this.getQueue = new LinkedList<>();
    }

    class GetQueue {
        public int n;
        public long timeout;
        public final Condition condition;

        public GetQueue(int n, long timeout, Lock lock) {
            this.n = n;
            this.timeout = timeout;
            this.condition = lock.newCondition();
        }
    }

    public void put(T msg) {
        messages.add(msg);
        count.incrementAndGet();
        if(getQueue.size() > 0) {
            GetQueue elem = getQueue.get(0);
            elem.condition.signal();
        }
    }

    public Optional<List<T>> get(int n, long timeout, TimeUnit unit) throws InterruptedException {
        try {
            lock.lock();
            long remainingTime = unit.toNanos(timeout);
            if (remainingTime <= 0) {
                return Optional.empty();
            }
            if (count.get() >= n && getQueue.size() == 0) {
                List<T> list = messages.subList(0, n);
                messages.remove(list);
                count.getAndAdd(-n);
                return Optional.of(list);
            }
            GetQueue elem = new GetQueue(n, timeout, lock);
            getQueue.add(elem);
        } finally {
            lock.unlock();
        }
        return Optional.empty();
    }
}
