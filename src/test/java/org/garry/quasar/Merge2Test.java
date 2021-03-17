package org.garry.quasar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Merge2Test implements CoroutineProto{

    public interface Interface
    {
        public void method();
    }

    public static Interface getInterface()
    {
        return null;
    }

    public static void suspendable() throws SuspendExecution
    {

    }

    @Override
    public void coExecute() throws SuspendExecution {
        try {
            Interface iface = getInterface();
            iface.method();
        }catch (IllegalStateException ise)
        {
            suspendable();
        }
    }

    @Test
    public void testMerge2()
    {
        try {
            Coroutine c = new Coroutine(new Merge2Test());
            c.run();
            assertEquals("Should not reach here", false);
        }catch (NullPointerException ex)
        {

        }
    }
}
