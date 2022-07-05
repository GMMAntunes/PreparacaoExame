import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BatchExchanger<T> {

    private final Lock lock = new ReentrantLock();
    private final List<T> messages = new LinkedList<>();
    private final int batchSize; private final Condition condition = lock.newCondition();

    public BatchExchanger(int batchSize) {
        this.batchSize = batchSize;
    }

    public Optional<List<T>> deliverAndWait(T msg, long timeout, TimeUnit unit) throws InterruptedException {
        try {
            lock.lock();
            long remainingTime = unit.toNanos(timeout);
            if (remainingTime <= 0) {
                return Optional.empty();
            }
            messages.add(msg);
            if (messages.size() == batchSize) {
                condition.signal();
                Optional<List<T>> list = Optional.of(messages);
                messages.clear();
                return list;
            }
        } finally {
            lock.unlock();
        }
        while(true) {
            try {
                condition.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (messages.size() == batchSize) {
                    Optional<List<T>> list = Optional.of(messages);
                    messages.clear();
                    return list;
                } else {
                    messages.remove(msg);
                }
                throw e;
            }
            long remainingTime = unit.toNanos(timeout);
            if (remainingTime <= 0) {
                return Optional.empty();
            }
            if (messages.size() == batchSize) {
                Optional<List<T>> list = Optional.of(messages);
                messages.clear();
                return list;
            }
        }
    }
}
