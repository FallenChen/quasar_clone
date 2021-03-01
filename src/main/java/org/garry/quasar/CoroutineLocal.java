package org.garry.quasar;

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
     * ThreadLocalMap is a customized hash map suitable only for
     * maintaining thread local values. No operations are exported outside of the ThreadLocal class.
     * The class is package private to allow declaration of fields in class Thread. To help deal with
     * very large and long-lived usages,the hash table entries use WeakReferences for keys.However, since
     * reference queues are not used, stale entries are guaranteed to be removed only when the table starts
     * running out of space.
     */
    static class CoroutineLocalMap
    {

    }
}
