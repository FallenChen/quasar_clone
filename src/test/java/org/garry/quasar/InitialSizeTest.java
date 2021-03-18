package org.garry.quasar;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InitialSizeTest implements CoroutineProto{

    @Test
    public void test1()
    {
        testWithSize(1);
    }

    @Test
    public void test2()
    {
        testWithSize(2);
    }

    @Test
    public void test3()
    {
        testWithSize(3);
    }

    private void testWithSize(int stackSize)
    {
        Coroutine c = new Coroutine(this,stackSize);
        assertEquals(getStackSize(c),stackSize);
        c.run();
        assertEquals(Coroutine.State.SUSPENDED, c.getState());
        c.run();
        assertEquals(Coroutine.State.FINISHED, c.getState());
        assertTrue(getStackSize(c) > 10);
    }


    @Override
    public void coExecute() throws SuspendExecution {
        assertEquals(3628800, factorial(10));
    }

    private int factorial(Integer a) throws SuspendExecution {
        if(a == 0)
        {
            Coroutine.yield();
            return 1;
        }
        return a * factorial(a - 1);
    }

    private int getStackSize(Coroutine c)
    {
        try {
            Field stackField = Coroutine.class.getDeclaredField("stack");
            stackField.setAccessible(true);
            Object stack = stackField.get(c);
            Field dataObjectField = Stack.class.getDeclaredField("dataObject");
            dataObjectField.setAccessible(true);
            Object[] dataObject = (Object[]) dataObjectField.get(stack);
            return dataObject.length;
        }catch (Throwable ex)
        {
            throw new AssertionError(ex);
        }
    }
}
