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
        Coroutine c = Coroutine.getActiveCoroutine();
        CoroutineLocalMap map = getMap(c);
        if(map !=null)
        {

        }
    }

    /**
     * Get the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal
     * @param c
     * @return
     */
    CoroutineLocalMap getMap(Coroutine c)
    {
        return c.coroutineLocals;
    }

    /**
     * Method childValue is visibly defined in subclass
     * InheritableThreadLocal, but is internally defined here for
     * the sake of providing createInheritedMap factory method without
     * needing to subclass the map class in InheritableThreadLocal.
     * This technique is preferable to the alternative of embedding
     * instance of tests in methods
     * @param parentValue
     * @return
     */
    T childValue(T parentValue)
    {
        throw new UnsupportedOperationException();
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

        /**
         * The number of entries in the table
         */
        private int size = 0;

        /**
         * The next size value at which to resize
         */
        private int threshold;

        /**
         * Set the resize threshold to maintain at worst a 2/3 load factor
         * @param len
         */
        private void setThreshold(int len)
        {
            threshold = len * 2/3;
        }

        /**
         * Increment i
         * @param i
         * @param len
         * @return
         */
        private static int nextIndex(int i, int len)
        {
            return ((i + 1 < len) ? i + 1 : 0);
        }

        /**
         * Decrement i
         * @param i
         * @param len
         * @return
         */
        private static int prevIndex(int i, int len)
        {
            return ((i-1 >=0) ? i-1 : len -1);
        }

        /**
         * Construct a new map initially containing (firstKey, firstValue).
         * ThreadLocalMaps are constructed lazily, so we only create
         * one when we have at least one entry to put in it
         * @param firstKey
         * @param firstValue
         */
        CoroutineLocalMap(CoroutineLocal firstKey, Object firstValue)
        {
            table = new Entry[INITIAL_CAPACITY];
            int i = firstKey.coroutineLocalHashCode & (INITIAL_CAPACITY - 1);
            table[i] = new Entry(firstKey,firstValue);
            size = 1;
            setThreshold(INITIAL_CAPACITY);
        }

        /**
         * Construct a new map including all Inheritable ThreadLocals
         * from given parent map. Called only by createInheritedMap
         *
         * @param parentMap the map associated with parent thread
         */
        private CoroutineLocalMap(CoroutineLocalMap parentMap)
        {
            Entry[] parentTable = parentMap.table;
            int len = parentTable.length;
            setThreshold(len);
            table = new Entry[len];

            for(int j = 0; j <len; j++)
            {
                Entry e = parentTable[j];
                if(e != null)
                {
                    CoroutineLocal key = e.get();
                    if(key != null)
                    {
                        // todo 这不会报错？？？
                        Object value = key.childValue(e.value);
                        Entry c = new Entry(key,value);
                        int h = key.coroutineLocalHashCode & (len -1);
                        while (table[h] !=null)
                            h = nextIndex(h,len);
                        table[h] = c;
                        size++;
                    }
                }
            }
        }

        /**
         * Get the entry associated with key. This method
         * itself handles only the fast path: a direct hit of existing
         * key.It otherwise relays te getEntryAfterMiss.This is
         * designed to maximize performance for direct hits, in part
         * by making this method readily inlinable.
         * @param key
         * @return
         */
        private Entry getEntry(CoroutineLocal key)
        {
            int i = key.coroutineLocalHashCode & (table.length - 1);
            Entry e = table[i];
            if(e != null && e.get() == key)
                return e;
            else
                return getEntryAfterMiss(key,i,e);
        }

        /**
         * Version of getEntry method for use when key is not found in
         * its direct hash slot
         * @param key the coroutine local object
         * @param i the table index for key's hash code
         * @param e the entry at table[i]
         * @return the entry associated with key, or null if no such
         */
        private Entry getEntryAfterMiss(CoroutineLocal key, int i, Entry e)
        {
           Entry[] tab = table;
           int len = tab.length;

           while (e != null)
           {
               CoroutineLocal k = e.get();
               if(k == key)
                   return e;
               if(k == null)
                   expungeStaleEntry(i);
               else
                   i = nextIndex(i,len);
               e = tab[i];
           }
           return null;
        }

        /**
         * Set the value associated with key
         * @param key the thread local object
         * @param value the value to be set
         */
        private void set(CoroutineLocal key, Object value)
        {
            // todo 对比 java.lang.ThreadLocal.set
            // We don't use a fast path as with get() because it is at
            // least as common to use set() to create new entries as
            // it is to replace existing ones, in which case, a fast
            // path would fail more often than not.
            Entry[] tab = table;
            int len = tab.length;
            int i = key.coroutineLocalHashCode & (len - 1);

            for(Entry e = tab[i]; e != null; e = tab[i = nextIndex(i,len)])
            {
                CoroutineLocal k = e.get();
                if(k == key)
                {
                    e.value = value;
                    return;
                }
                if(k == null)
                {
                    replaceStaleEntry(key,value,i);
                    return;
                }
            }
            tab[i] = new Entry(key,value);
            int sz = ++size;
            if(!cleanSomeSlots(i,sz) && sz >= threshold)
                rehash();
        }

        /**
         * Expunge all stale entries in the table
         */
        private void expungeStaleEntries()
        {
            Entry[] tab = table;
            int len = tab.length;
            for (int j=0; j<len; j++)
            {
                Entry e = tab[j];
                if(e != null && e.get() == null)
                    expungeStaleEntry(j);
            }
        }

        /**
         * Expunge a stale entry by rehashing any possibly colliding entries
         * lying between staleSlot and the next null slot.  This also expunges
         * any other stale entries encountered before the trailing null
         *
         * @param staleSlot
         * @return
         */
        private int expungeStaleEntry(int staleSlot)
        {
            Entry[] tab = table;
            int len = tab.length;

            // expunge entry at staleSlot
            tab[staleSlot].value = null;
            tab[staleSlot] = null;
            size--;

            //Rehash until we encounter null
            Entry e;
            int i;
            for(i = nextIndex(staleSlot,len); (e = tab[i]) != null; i = nextIndex(i,len))
            {
                CoroutineLocal k = e.get();
                if(k == null)
                {
                    e.value = null;
                    tab[i] = null;
                    size--;
                }else {
                    int h = k.coroutineLocalHashCode & (len - 1);
                    if(h != i)
                    {
                        tab[i] = null;

                        // Unlike Knuth 6.4 Algorithm R
                        while (tab[h] != null)
                            h = nextIndex(h,len);
                        tab[h] = e;
                    }
                }
            }
            return i;
        }
    }
}
