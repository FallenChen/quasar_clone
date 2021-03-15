package org.garry.quasar;

import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Test to checking suspendable method calls as constructor parameters
 */
public class SuspendConstructorArgumentTest {

    @Test
    public void testCalls()
    {
        Iterator<String> iter = new CoIterator<String>() {
            @Override
            protected void run() throws SuspendExecution {
                m1();
                m2();
                m3();
                m4();
            }

            private void m1() throws SuspendExecution {
                produce(new StringBuilder(str()).append(" Bla").toString());
            }

            private void m2() throws SuspendExecution {
                produce(new String(buf(), offset(), len()));
            }

            private void m3() throws SuspendExecution {
                produce(new Long(l()).toString());
            }

            private void m4() throws SuspendExecution {
                produce(new StringBuilder(new String(buf(), offset(), len())).append(str()).toString());
            }

            private String str() throws SuspendExecution {
                produce("str()");
                return "Test";
            }

            private char[] buf() throws SuspendExecution {
                produce("buf()");
                return "Hugo".toCharArray();
            }

            private int offset() throws SuspendExecution {
                produce("offset()");
                return 1;
            }

            private int len() throws SuspendExecution {
                produce("len()");
                return 3;
            }

            private long l() throws SuspendExecution {
                produce("l()");
                return 4711L << 32;
            }
        };

        assertEquals("str()",iter.next());
        assertEquals("Test Bla",iter.next());
        assertEquals("buf()",iter.next());
        assertEquals("offset()",iter.next());
        assertEquals("len()",iter.next());
        assertEquals("ugo",iter.next());
        assertEquals("l()",iter.next());
        assertEquals(Long.toString(4711L << 32),iter.next());
        assertEquals("buf()",iter.next());
        assertEquals("offset()",iter.next());
        assertEquals("len()",iter.next());
        assertEquals("str()",iter.next());
        assertEquals("ugoTest",iter.next());
        assertFalse(iter.hasNext());
    }
}
