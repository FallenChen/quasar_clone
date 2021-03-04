package org.garry.quasar;

import java.io.Serializable;

/**
 * A Coroutine is used to run a CoroutineProto
 * It also provides a function to suspend a running Coroutine
 *
 * A Coroutine can be serialized if it's not running and all involved
 * classes and data types are also {@link Serializable}
 */
public class Coroutine implements Runnable, Serializable {

    /**
     * Default stack size for the data stack
     */
    public static final int DEFAULT_STACK_SIZE = 16;

    public enum State
    {
        // The Coroutine has not yet been executed
        NEW,
        // The Coroutine is currently executing
        RUNNING,
        // The Coroutine has suspended it's execution
        SUSPENDED,
        // The Coroutine has finished it's run method
        FINISHED
    }

    private final CoroutineProto proto;
    private final Stack stack;
    private State state;
    CoroutineLocal.CoroutineLocalMap coroutineLocals;
    /**
     * Suspend the currently running Coroutine on the calling thread
     * @throws SuspendException
     * @throws IllegalStateException
     */
    public static void yield() throws SuspendException, IllegalStateException
    {
        throw new Error("Calling function not instrumented");
    }

    /**
     * Creates a new Coroutine from the given CoroutineProto. A CoroutineProto
     * can be used in several Coroutines at the same time - but then the normal
     * multi threading rules apply to the member state
     * @param proto
     */
    public Coroutine(CoroutineProto proto)
    {
        this(proto,DEFAULT_STACK_SIZE);
    }

    public Coroutine(CoroutineProto proto, int stackSize)
    {
        this.proto = proto;
        this.stack = new Stack(this, stackSize);
        this.state = State.NEW;

        if(proto == null)
        {
            throw new NullPointerException("proto");
        }
        assert isInstrumented(proto) : "Not instrumented";
    }

    /**
     * Returns the active Coroutine on this thread or NULL if no coroutine is running
     * @return
     */
    public static Coroutine getActiveCoroutine()
    {
        Stack s = Stack.getStack();
        if(s != null)
        {
            return s.co;
        }
        return null;
    }

    /**
     * Returns the CoroutineProto that is used for this Coroutine
     * @return
     */
    public CoroutineProto getProto()
    {
        return proto;
    }

    /**
     * Returns the current state of this Coroutine. May be called by the Coroutine
     * itself but should not be called by another thread
     *
     * The Coroutine starts in the state NEW then changes to RUNNING. From
     * RUNNING it may change to FINISHED or SUSPENDED. SUSPENDED can only change
     * to RUNNING by calling run() again
     * @return
     */
    public State getState()
    {
        return state;
    }

    /**
     * Runs the Coroutine until it is finished or suspended. This method must only
     * be called when the Coroutine is in the states NEW or SUSPENDED. It is not
     * multi threading safe.
     */
    public void run()
    {
        if(state != State.NEW && state != State.SUSPENDED)
        {
            throw new IllegalStateException("Not new or suspended");
        }
        State result = State.FINISHED;
        Stack oldStack = Stack.getStack();
        try {
            state = State.RUNNING;
            Stack.setStack(stack);
            try {
                proto.coExecute();
            }catch (SuspendException ex)
            {
                assert ex == SuspendException.instance;
                result = State.SUSPENDED;
                stack.resumeStack();
            }
        }finally {
            Stack.setStack(oldStack);
            state = result;
        }
    }


    private boolean isInstrumented(CoroutineProto proto)
    {
        try {
            Class clz = Class.forName("org.garry.quasar.instrument.AlreadyInstrumented.java");
            return proto.getClass().isAnnotationPresent(clz);
        }catch (ClassNotFoundException ex)
        {
            return true;
        }catch (Throwable ex)
        {
            return true; //
        }
    }
}
