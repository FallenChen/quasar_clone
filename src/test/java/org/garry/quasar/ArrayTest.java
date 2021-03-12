package org.garry.quasar;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArrayTest  implements CoroutineProto{

    private static final PatchLevel l1 = new PatchLevel();
    private static final PatchLevel[] l2 = new PatchLevel[] {l1};
    private static final PatchLevel[][] l3 = new PatchLevel[][] {l2};

    @Test
    public void testArray()
    {
        Coroutine co = new Coroutine(this);
        co.run();
        assertEquals(42,l1.i);
    }

    @Override
    public void coExecute() throws SuspendExecution {
        PatchLevel[][] local_patch_levels = l3;
        PatchLevel patch_level = local_patch_levels[0][0];
        patch_level.setLevel(42);
    }

    public static class PatchLevel
    {
        int i;

        public void setLevel(int value)
        {
            i = value;
        }
    }
}
