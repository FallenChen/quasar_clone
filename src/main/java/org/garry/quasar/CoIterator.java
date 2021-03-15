package org.garry.quasar;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A coroutine based iterator
 * @param <E>
 */
public abstract class CoIterator<E> implements Iterator<E>, Serializable {

    private static final long serialVersionUID = 351278561539L;

    private final Coroutine co;

    private E element;

    private boolean hasElement;

    protected CoIterator()
    {
        co = new Coroutine(new DelegateExecute());
    }

    public boolean hasNext()
    {
        while (!hasElement && co.getState() != Coroutine.State.FINISHED)
        {
            co.run();
        }
        return hasElement;
    }

    public E next()
    {
        if(!hasNext())
        {
            throw new NoSuchElementException();
        }

        E result = element;
        hasElement = false;
        element = null;
        return result;
    }

    /**
     * Always throws UnsupportedOperationException
     * @throws UnsupportedOperationException
     */
    public void remove() throws UnsupportedOperationException
    {
       throw new UnsupportedOperationException("Not supported");
    }

    /**
     * Produces the next value to be returned by the {@link #next()} method
     * @param element
     * @throws SuspendExecution
     */
    protected void produce(E element) throws SuspendExecution {
        if(hasElement)
        {
            throw new IllegalStateException("hasElement = true");
        }
        this.element = element;
        hasElement = true;
        Coroutine.yield();
    }


    /**
     * This is the body of the Iterator.This method is executed as a
     * Coroutine to produce the values of the Iterator
     *
     * Note that this method is suspended each time it calls produce. And if
     * the consumer does not consume all values of the Iterator then this
     * method does not get the change to finish it's execution. This also
     * includes the finally blocks.
     *
     * This method must only suspend by calling produce. Any other reason
     * for suspension will cause a busy loop in the Iterator
     * @throws SuspendExecution
     */
    protected abstract void run() throws SuspendExecution;


    private class DelegateExecute implements CoroutineProto, Serializable
    {
        private static final long serialVersionUID = 12784529515412L;

        @Override
        public void coExecute() throws SuspendExecution {
            CoIterator.this.run();
        }
    }
}
