package org.garry.quasar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NullTest implements CoroutineProto{

    Object result = "b";

    @Test
    public void testNull()
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
       Object x = null;

       Object y = getProperty("a");
       if(y !=null)
       {
           x = y;
       }
       return x;
    }

    private Object getProperty(String string) throws SuspendExecution {
       Coroutine.yield();
       return string;
    }

}
