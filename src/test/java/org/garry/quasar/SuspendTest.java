package org.garry.quasar;


import org.junit.jupiter.api.Test;

public class SuspendTest  implements CoroutineProto {

    @Test
    public void testSuspend() {
        SuspendTest test = new SuspendTest();
        Coroutine co = new Coroutine(test);

        while (co.getState() != Coroutine.State.FINISHED) {
            System.out.println("State=" + co.getState());
            co.run();
        }
        System.out.println("State= " + co.getState());
    }

    @Override
    public void coExecute() throws SuspendException {
        int i0 = 0, i1 = 1;
        for (int j = 0; j < 10; j++) {
            i1 = i1 + i0;
            i0 = i1 - i0;
            print("bla %d %d\n", i0, i1);
        }
    }

    private static void print(String fmt, Object... args) throws SuspendException {
        System.out.printf(fmt, args);
        Coroutine.yield();

    }
}
