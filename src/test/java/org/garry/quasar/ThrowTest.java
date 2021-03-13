package org.garry.quasar;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThrowTest implements CoroutineProto {

    private ArrayList<String> results = new ArrayList<>();


    @Override
    public void coExecute() throws SuspendExecution {
        results.add("A");
        Coroutine.yield();
        try {
            results.add("C");
            Coroutine.yield();
            if("".length() == 0)
            {
                throw new IllegalStateException("bla");
            }
            results.add("E");
        }finally {
            results.add("F");
        }
        results.add("G");
    }

    @Test
    public void testThrow()
    {
        results.clear();

        Coroutine co = new Coroutine(this);
        try {
            co.run();
            results.add("B");
            co.run();
            results.add("D");
            co.run();
            assertTrue(false);
        }catch (IllegalStateException es)
        {
            assertEquals("bla",es.getMessage());
            assertEquals(Coroutine.State.FINISHED, co.getState());
        }finally {
            System.out.println(results);
        }

        assertEquals(5, results.size());
        assertEquals("A", results.get(0));
        assertEquals("B", results.get(1));
        assertEquals("C", results.get(2));
        assertEquals("D", results.get(3));
        assertEquals("F", results.get(4));
    }
}
