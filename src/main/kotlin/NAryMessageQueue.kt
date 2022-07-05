import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * EXAME PC 2021V_2
 */
class NAryMessageQueue<T> {
    private val messages: MutableList<T>
    private val lock: Lock = ReentrantLock()
    private val count: AtomicInteger
    private val getQueue: MutableList<GetQueue>

    init {
        messages = LinkedList()
        count = AtomicInteger()
        getQueue = LinkedList<GetQueue>()
    }

    internal inner class GetQueue(var n: Int, var timeout: Long, lock: Lock) {
        val condition: Condition

        init {
            condition = lock.newCondition()
        }
    }

    fun put(msg: T) {
        messages.add(msg)
        count.incrementAndGet()
        if (getQueue.size > 0) {
            val elem: GetQueue = getQueue[0]
            elem.condition.signal()
        }
    }

    @Throws(InterruptedException::class)
    operator fun get(n: Int, timeout: Long, unit: TimeUnit): Optional<List<T>> {
        try {
            lock.lock()
            val remainingTime = unit.toNanos(timeout)
            if (remainingTime <= 0) {
                return Optional.empty()
            }
            if (count.get() >= n && getQueue.size == 0) {
                val list: List<T> = messages.subList(0, n)
                //messages.remove(list)
                count.getAndAdd(-n)
                return Optional.of(list)
            }
            val elem: GetQueue = GetQueue(n, timeout, lock)
            getQueue.add(elem)
        } finally {
            lock.unlock()
        }
        return Optional.empty()
    }
}