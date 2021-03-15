package org.garry.quasar;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

public class SerializeTest {

    @Test
    public void testSerialize() throws IOException, ClassNotFoundException {
        TestIterator iter1 = new TestIterator();

        assertEquals("A",iter1.next());
        assertEquals("B",iter1.next());
        assertEquals("C0",iter1.next());
        assertEquals("C1",iter1.next());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(iter1);
        oos.close();

        byte[] bytes = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Iterator<String> iter2 = (Iterator<String>) ois.readObject();

        assertNotSame(iter1,iter2);

        assertEquals("C2",iter2.next());
        assertEquals("C3",iter2.next());
        assertEquals("D",iter2.next());
        assertEquals("E",iter2.next());
        assertFalse(iter2.hasNext());

        assertEquals("C2",iter1.next());
        assertEquals("C3",iter1.next());
        assertEquals("D",iter1.next());
        assertEquals("E",iter1.next());
        assertFalse(iter1.hasNext());

    }

    private static class TestIterator extends CoIterator<String> implements Serializable
    {
        @Override
        protected void run() throws SuspendExecution {
            produce("A");
            produce("B");
            for(int i=0; i< 4; i++)
            {
                produce("C" + i);
            }
            produce("D");
            produce("E");
        }
    }
}
