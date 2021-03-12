package org.garry.quasar;

/**
 * An exception used to initiate the control transfer
 *
 * Methods which are declared to throw this exception are "suspendable". This
 * exception must always be propagated and never be caught
 *
 *
 *
 */
public final class SuspendExecution extends Exception{

    static final SuspendExecution instance = new SuspendExecution();

    private SuspendExecution()
    {

    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
