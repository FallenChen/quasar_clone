package org.garry.quasar;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

public class MergeTest implements CoroutineProto{

    public static void throwsIO() throws IOException
    {

    }

    @Override
    public void coExecute() throws SuspendExecution {
       try {
           throwsIO();
       }catch (FileNotFoundException e)
       {
           e.printStackTrace();
       }catch (IOException e)
       {
           e.printStackTrace();
       }
    }

    @Test
    public void testMerge()
    {
        Coroutine c = new Coroutine(new MergeTest());
        c.run();
    }
}
