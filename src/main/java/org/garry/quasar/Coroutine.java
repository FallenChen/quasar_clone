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


    /**
     * Suspend the currently running Coroutine on the calling thread
     * @throws SuspendException
     * @throws IllegalStateException
     */
    public static void yield() throws SuspendException, IllegalStateException
    {
        throw new Error("Calling function not instrumented");
    }
}
