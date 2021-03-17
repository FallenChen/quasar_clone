package org.garry.quasar;

import org.garry.quasar.instrument.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.assertEquals;


@AlreadyInstrumented
public class BlockingTest {

    @Test
    public void testSuspend() throws IOException {
        final String className = BlockingTest.class.getName().replace('.', '/');
        final HashSet<String> msgs = new HashSet<>();
        msgs.add("Method "+className+"#t_wait()V contains potentially blocking call to java/lang/Object#wait()V");
        msgs.add("Method "+className+"#t_sleep1()V contains potentially blocking call to java/lang/Thread#sleep(J)V");
        msgs.add("Method "+className+"#t_sleep2()V contains potentially blocking call to java/lang/Thread#sleep(JI)V");
        msgs.add("Method "+className+"#t_join1(Ljava/lang/Thread;)V contains potentially blocking call to java/lang/Thread#join()V");
        msgs.add("Method "+className+"#t_join2(Ljava/lang/Thread;)V contains potentially blocking call to java/lang/Thread#join(J)V");
        msgs.add("Method "+className+"#t_join3(Ljava/lang/Thread;)V contains potentially blocking call to java/lang/Thread#join(JI)V");
        msgs.add("Method "+className+"#t_lock1(Ljava/util/concurrent/locks/Lock;)V contains potentially blocking call to java/util/concurrent/locks/Lock#lock()V");
        msgs.add("Method "+className+"#t_lock2(Ljava/util/concurrent/locks/Lock;)V contains potentially blocking call to java/util/concurrent/locks/Lock#lockInterruptibly()V");

        MethodDatabase db = new MethodDatabase(BlockingTest.class.getClassLoader());
        db.setAllowBlocking(true);
        db.setLog(new Log() {
            @Override
            public void log(LogLevel level, String msg, Object... args) {
                if(level == LogLevel.WARNING)
                {
                    msg = String.format(Locale.ENGLISH, msg, args);
                    assertEquals("Unexpected message: " + msg, msgs.remove(msg));
                }
            }

            @Override
            public void error(String msg, Exception ex) {
                throw new AssertionError(msg,ex);
            }
        });

        InputStream in = BlockingTest.class.getResourceAsStream("BlockingTest.class");
        try {
            ClassReader r = new ClassReader(in);
            ClassWriter cw = new ClassWriter(0);
            InstrumentClass ic = new InstrumentClass(cw,db,true);
            r.accept(ic,ClassReader.SKIP_FRAMES);
        }finally {
            in.close();
        }
        assertEquals("Expected messages not generated: "+msgs.toString(), msgs.isEmpty());
    }

    public void t_wait() throws InterruptedException {
        synchronized (this)
        {
            wait();
        }
    }

    public void t_notify()
    {
        synchronized (this)
        {
            notify();
        }
    }

    public void t_sleep1() throws InterruptedException {
        Thread.sleep(1000);
    }

    public void t_sleep2() throws InterruptedException {
        Thread.sleep(1000,100);
    }

    public void t_join1(Thread t) throws SuspendExecution, InterruptedException {
        t.join();
    }

    public void t_join2(Thread t) throws SuspendExecution, InterruptedException {
        t.join(1000);
    }

    public void t_join3(Thread t) throws SuspendExecution, InterruptedException {
        t.join(1, 100);
    }

    public void t_lock1(Lock lock) throws SuspendExecution {
        lock.lock();
    }

    public void t_lock2(Lock lock) throws SuspendExecution, InterruptedException {
        lock.lockInterruptibly();
    }

    public void t_lock3() throws SuspendExecution {
        lock();
    }

    public void lock() {
        System.out.println("Just a method which have similar signature");
    }


}
