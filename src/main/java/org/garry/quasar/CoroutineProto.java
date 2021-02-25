package org.garry.quasar;

/**
 * A class that implements this interface can be run as a Coroutine
 */
public interface CoroutineProto {

    /**
     * Entry point for Coroutine execution
     *
     * This method should never be called directly
     *
     * @throws SuspendException
     */
    void coExecute() throws SuspendException;
}
