package org.garry.quasar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class CoIteratorTest {

    @Test
    public void testCoIterator()
    {
        CoIterator<String> iter = new CoIterator<>() {
            @Override
            protected void run() throws SuspendExecution {
                for (int j = 0; j < 3; j++) {
                    produce("Hugo " + j);
                    produce("Test");
                    for (int i = 1; i < 10; i++) {
                        produce("Number " + i);
                    }
                    produce("Nix");
                }
            }
        };

        for(int j=0; j<3; j++)
        {
            assertEquals("Hugo " + j, iter.next());
            assertEquals("Test", iter.next());
            for(int i=1; i<10; i++)
            {
                assertEquals("Number " + i, iter.next());
            }
            assertEquals("Nix ",iter.next());
        }
        assertFalse(iter.hasNext());
    }
}
