package org.garry.quasar;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test correct execution of a  finally statement
 */
public class FinallyTest implements CoroutineProto{

    private ArrayList<String> results = new ArrayList<>();

    @Override
    public void coExecute() throws SuspendExecution {
        results.add("A");
        Coroutine.yield();
        try {
            results.add("C");
            Coroutine.yield();
            results.add("E");
        }finally {
            results.add("F");
        }
        results.add("G");
        Coroutine.yield();
        results.add("I");
    }

    @Test
    public void testFinally() {
        results.clear();

        try {
            Coroutine co = new Coroutine(this);
            co.run();
            results.add("B");
            co.run();
            results.add("D");
            co.run();
            results.add("H");
            co.run();
        } finally {
            System.out.println(results);
        }

        assertEquals(9, results.size());
        assertEquals("A", results.get(0));
        assertEquals("B", results.get(1));
        assertEquals("C", results.get(2));
        assertEquals("D", results.get(3));
        assertEquals("E", results.get(4));
        assertEquals("F", results.get(5));
        assertEquals("G", results.get(6));
        assertEquals("H", results.get(7));
        assertEquals("I", results.get(8));
    }
}
