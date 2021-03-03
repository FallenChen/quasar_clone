package org.garry.quasar;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class provides coroutine-local variables. These variables differ from
 * their normal counterparts in that each coroutine that accesses one ( get or set method)
 * has its own, independently initialized copy of the variable.CoroutineLocal instances are typically
 * private static fields in classes that wish to associate state with a coroutine
 *
 * For example, the class below generates unique identifiers local to each thread
 *
 * A thread's id is assigned the first time it invokes ThreadId.get() and remains unchanged on subsequent calls.
 *
 * Each coroutine holds an implicit reference to its copy of a coroutine-local variable as long as the coroutine is
 * alive and the CoroutineLocal instance is accessible; after a coroutine goes away, all of its copies of
 * coroutine-local instances are subject to garbage collection(unless other references to these copies exist)
 */
public class CoroutineLocal<T> {

    /**
     * CoroutineLocals rely on per-coroutine linear-probe hash maps attached
     * to each coroutine. The CoroutineLocal objects act as keys, searched via
     * coroutineLocalHashCode. This is a custom hash code (useful only within CoroutineLocalMaps)
     * that eliminates collisions in the common case where consecutively constructed ThreadLocals
     * are used by the same threads, while remaining well-behaved in less common cases
     */
    private final int coroutineLocalHashCode = nextHashCode();

    /**
     * The next hash code to be given out.Updated atomically.Starts at zero
     */
    private static AtomicInteger nextHashCode =
            new AtomicInteger();

    /**
     * The difference between successively generated hash codes
     * turns implicit sequential thread-local IDs into near-optimally spread
     * multiplicative hash values for power-of-two-sized tables
     */
    private static final int HASH_INCREMENT = 0x61c88647;

    /**
     * Returns the next hash code
     * @return
     */
    private static int nextHashCode()
    {
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }

    /**
     * Returns the current thread's "initial value" for this
     * thread-local variable.This method will be invoked the first
     * time a thread accesses the variable with the {@link #get}
     * method, unless the thread previously invoked the {@link #set}
     * method, in which case the <tt>initialValue</tt> method will not
     * be invoked for the thread. Normally, this method is invoked at
     * most once per thread, but it may be invoked again in case of
     * subsequent invocations of {@link #remove} followed by {@link #get}
     *
     * This implementation simply returns null; if the
     * programmer desires thread-local variables to have an initial
     * value other than null, ThreadLocal must be subclassed, and this method overridden
     * Typically, an anonymous inner calls will be used.
     *
     */
    protected T initialValue()
    {
        return null;
    }

    public CoroutineLocal()
    {

    }

    /**
     * Returns the value in the current coroutine's copy of this
     * thread-local variable.If the variable has no value for the
     * current thread, it is first initialized to the value returned
     * by an invocation of the {@link #initialValue()} method
     *
     * @return the current coroutine's value of this thread-local
     */
    public T get()
    {

    }


    /**
     * ThreadLocalMap is a customized hash map suitable only for
     * maintaining thread local values. No operations are exported outside of the ThreadLocal class.
     * The class is package private to allow declaration of fields in class Thread. To help deal with
     * very large and long-lived usages,the hash table entries use WeakReferences for keys.However, since
     * reference queues are not used, stale entries are guaranteed to be removed only when the table starts
     * running out of space.
     */
    static class CoroutineLocalMap
    {
        /**
         * The entries in this hash map extend WeakReference, using
         * its main ref field as the key (which is always a ThreadLocal object).
         * Note that null keys(i.e. entry.get() == null) mean that the key is no longer referenced,
         * so the entry can be expunged from table. Such entries are referred to as "stale entries"
         * int the code that follows
         */
        static class Entry extends WeakReference<CoroutineLocal>
        {
            Object value;
            // The value associated with this ThreadLocal
            Entry(CoroutineLocal k, Object v)
            {
                super(k);
                value = v;
            }
        }

        /**
         * The initial capacity -- MUST be a power of two
         */
        private static final int INITIAL_CAPACITY = 16;

        /**
         * The table, resized as necessary
         * table.length MUST always be a power of two
         */
        private Entry[] table;
    }
}
