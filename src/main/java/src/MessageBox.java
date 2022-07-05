import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * EXAME PC 2021V_1
 */

public class MessageBox <T> {

    private final Lock lock = new ReentrantLock();
    private List<Box> msgBox = new LinkedList<>();
    private Condition condition = lock.newCondition();

    class Box {
        T msg;
        long timeout;
        Condition cond;
        public Box(long timeout) {
            this.timeout = timeout;
            cond = condition;
        }
    }

    public int sendToAll(T message) {
        int counter = 0;
        if (msgBox.size() > 0) {
            for (Box b : msgBox) {
                b.msg = message;
                counter++;
            }
            condition.signalAll();
        }
        return counter;
    }

    public Optional<T> waitForMessage(long timeout, TimeUnit unit) throws InterruptedException {
        try {
            lock.lock();
            //FAST PATH
            long remainingTime = unit.toNanos(timeout);
            if (remainingTime <= 0) {
                return Optional.empty();
            }
            Box box = new Box(timeout);
            msgBox.add(box);
            if (box.msg != null) {
                msgBox.remove(box);
                return Optional.of(box.msg);
            }
        } finally {
            lock.unlock();
        }
        // SLOW PATH
        while (true) {
            Box box = null;
            try {
                condition.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                box = new Box(timeout);
                msgBox.remove(box);
                throw e;
            }
            long remainingTime = unit.toNanos(timeout);
            if (remainingTime <= 0) {
                return Optional.empty();
            }
            assert false;
            if (box.msg != null) {
                msgBox.remove(box);
                return Optional.of(box.msg);
            }
        }
    }
}