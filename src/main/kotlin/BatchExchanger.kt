import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * EXAME PC 2021V_2
 */

class BatchExchanger<T>(private val batchSize: Int) {
    private val lock: Lock = ReentrantLock()
    private val messages: MutableList<T> = LinkedList()
    private val condition = lock.newCondition()

    @Throws(InterruptedException::class)
    fun deliverAndWait(msg: T, timeout: Long, unit: TimeUnit): Optional<List<T>> {
        try {
            lock.lock()
            val remainingTime = unit.toNanos(timeout)
            if (remainingTime <= 0) {
                return Optional.empty()
            }
            messages.add(msg)
            if (messages.size == batchSize) {
                condition.signal()
                val list = Optional.of<List<T>>(messages)
                messages.clear()
                return list
            }
        } finally {
            lock.unlock()
        }
        while (true) {
            try {
                condition.await()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                if (messages.size == batchSize) {
                    val list = Optional.of<List<T>>(messages)
                    messages.clear()
                    return list
                } else {
                    messages.remove(msg)
                }
                throw e
            }
            val remainingTime = unit.toNanos(timeout)
            if (remainingTime <= 0) {
                return Optional.empty()
            }
            if (messages.size == batchSize) {
                val list = Optional.of<List<T>>(messages)
                messages.clear()
                return list
            }
        }
    }
}