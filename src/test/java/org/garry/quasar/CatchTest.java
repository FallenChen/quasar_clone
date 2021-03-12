package org.garry.quasar;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Check that a generic catch all does not affect the suspendtion of a method
 */
public class CatchTest implements CoroutineProto{

    private ArrayList<String> results = new ArrayList<>();

    int cnt = 0;

    private void throwOnSecondCall() throws SuspendExecution {
        results.add("cnt=" + cnt);
        Coroutine.yield();
        if(++cnt >=2)
        {
            throw new IllegalStateException("called second time");
        }
        results.add("not thrown");
    }

    @Override
    public void coExecute() throws SuspendExecution {
        results.add("A");
        Coroutine.yield();
        try {
            results.add("C");
            Coroutine.yield();
            throwOnSecondCall();
            Coroutine.yield();
            throwOnSecondCall();
            results.add("never reached");
        }catch (Throwable ex)
        {
            results.add(ex.getMessage());
        }
        results.add("H");
    }

    @Test
    public void testCatch()
    {
        results.clear();

        try {
            Coroutine co = new Coroutine(this);
            co.run();
            results.add("B");
            co.run();
            results.add("D");
            co.run();
            results.add("E");
            co.run();
            results.add("F");
            co.run();
            results.add("G");
            co.run();
            results.add("I");
        }finally {
            System.out.println(results);
        }

        assertEquals(13,results.size());
        Iterator<String> iter = results.iterator();
        assertEquals("A",iter.next());
        assertEquals("B",iter.next());
        assertEquals("C",iter.next());
        assertEquals("D",iter.next());
        assertEquals("cnt=0",iter.next());
        assertEquals("E",iter.next());
        assertEquals("not thrown",iter.next());
        assertEquals("F",iter.next());
        assertEquals("cnt=1",iter.next());
        assertEquals("G",iter.next());
        assertEquals("called second time",iter.next());
        assertEquals("H",iter.next());
        assertEquals("I",iter.next());
    }
}
