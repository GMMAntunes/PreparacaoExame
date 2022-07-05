import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class MessageBox<T : Any> {
    private val lock: Lock = ReentrantLock()
    private val msgBox: MutableList<Box?> = LinkedList<Box?>()
    private val condition = lock.newCondition()

    internal inner class Box(var timeout: Long) {
        var msg: T? = null
        var cond: Condition = condition


    }

    fun sendToAll(message: T): Int {
        var counter = 0
        if (msgBox.size > 0) {
            for (b in msgBox) {
                if (b != null) {
                    b.msg = message
                }
                counter++
            }
            condition.signalAll()
        }
        return counter
    }

    @Throws(InterruptedException::class)
    fun waitForMessage(timeout: Long, unit: TimeUnit): Optional<T> {
        try {
            lock.lock()
            //FAST PATH
            val remainingTime = unit.toNanos(timeout)
            if (remainingTime <= 0) {
                return Optional.empty()
            }
            val box: Box = Box(timeout)
            msgBox.add(box)
            if (box.msg != null) {
                msgBox.remove(box)
                return Optional.of(box.msg!!)
            }
        } finally {
            lock.unlock()
        }
        // SLOW PATH
        while (true) {
            var box: Box? = null
            try {
                condition.await()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                box = Box(timeout)
                msgBox.remove(box)
                throw e
            }
            val remainingTime = unit.toNanos(timeout)
            if (remainingTime <= 0) {
                return Optional.empty()
            }
            if (box != null) {
                if (box.msg != null) {
                    msgBox.remove(box)
                    return Optional.of(box.msg!!)
                }
            }
        }
    }
}