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
public final class SuspendException extends Exception{

    static final SuspendException instance = new SuspendException();

    private SuspendException()
    {

    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
