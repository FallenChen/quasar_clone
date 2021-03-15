package org.garry.quasar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UninitializedTest implements CoroutineProto{

    Object result = "b";

    @Test
    public void testUninitialized()
    {
        int count = 0;
        Coroutine co = new Coroutine(this);
        while (co.getState() != Coroutine.State.FINISHED)
        {
            ++count;
            co.run();
        }
        assertEquals(2,count);
        assertEquals("a",result);
    }

    @Override
    public void coExecute() throws SuspendExecution {
        result = getProperty();
    }

    private Object getProperty() throws SuspendExecution {
        Object x;

        Object y = getProperty("a");
        if(y != null)
        {
            x = y;
        }else
        {
            x = getProperty("c");
        }
        return x;
    }

    private Object getProperty(String string) throws SuspendExecution {
        Coroutine.yield();
        return string;
    }
}
