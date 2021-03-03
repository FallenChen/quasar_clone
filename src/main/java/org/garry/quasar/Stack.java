package org.garry.quasar;

import java.io.Serializable;

public final class Stack implements Serializable {

    private static final ThreadLocal<Stack> tls = new ThreadLocal<>();

    final Coroutine co;

    private int methodTOS = -1;
    private int[] method;

    private long[] dataLong;
    private Object[] dataObject;

    transient int curMethodSP;// ???

    Stack(Coroutine co, int stackSize)
    {
        if(stackSize <=0)
        {
            throw new IllegalArgumentException("stackSize");
        }
        this.co = co;
        this.method = new int[8];
        this.dataLong = new long[stackSize];
        this.dataObject = new Object[stackSize];
    }

    public static Stack getStack()
    {
        return tls.get();
    }

    static void setStack(Stack s)
    {
        tls.set(s);
    }

    /**
     * Called before a method is called
     * @param entry the entry point in the method for resume
     * @param numSlots the number of required stack slots for storing the state
     */
    public final void pushMethodAndReserveSpace(int entry, int numSlots)
    {
       final int methodIdx = methodTOS;

       if(method.length - methodIdx < 2)
       {
           growMethodStack();
       }

       curMethodSP = method[methodIdx - 1];
       int dataTos = curMethodSP + numSlots;

       method[methodIdx] = entry;
       method[methodIdx + 1] = dataTos;

       if(dataTos > dataObject.length)
       {
           growDataStack(dataTos);
       }
    }

    /**
     * Called at the end of a method
     * Undoes the effects of nextMethodEntry() and clears the dataObject[] array
     * to allow the values to be GCed
     */
    public final void popMethod()
    {
        int idx = methodTOS;
        method[idx] = 0;
        int oldSP = curMethodSP;
        int newSP = method[idx-1];
        curMethodSP = newSP;
        methodTOS = idx - 2;
        for(int i=newSP; i<oldSP; i++)
        {
            dataObject[i] = null;
        }
    }

    /**
     * called at the begin of a method
     * @return the entry point of this method
     */
    public final int nextMethodEntry()
    {
        int idx = methodTOS;
        curMethodSP = method[++idx];
        methodTOS = ++idx;
        return method[idx];
    }

    public static void push(int value, Stack s, int idx)
    {
        s.dataLong[s.curMethodSP + idx] = value;
    }
    public static void push(float value, Stack s, int idx)
    {
        s.dataLong[s.curMethodSP + idx] = Float.floatToRawIntBits(value);
    }
    public static void push(long value,Stack s, int idx)
    {
        s.dataLong[s.curMethodSP + idx] = value;
    }
    public static void push(double value, Stack s, int idx) {
        s.dataLong[s.curMethodSP + idx] = Double.doubleToRawLongBits(value);
    }
    public static void push(Object value, Stack s, int idx) {
        s.dataObject[s.curMethodSP + idx] = value;
    }

    public final int getInt(int idx)
    {
        return (int)dataLong[curMethodSP + idx];
    }
    public final float getFloat(int idx) {
        return Float.intBitsToFloat((int)dataLong[curMethodSP + idx]);
    }
    public final long getLong(int idx) {
        return dataLong[curMethodSP + idx];
    }
    public final double getDouble(int idx) {
        return Double.longBitsToDouble(dataLong[curMethodSP + idx]);
    }
    public final Object getObject(int idx) {
        return dataObject[curMethodSP + idx];
    }


    /**
     * called when resuming a stack
     */
    final void resumeStack()
    {
        methodTOS = -1;
    }

    private void growDataStack(int required)
    {
        int newSize = dataObject.length;
        do {
            newSize *= 2;
        }while (newSize < required);

        dataLong = Util.copyOf(dataLong,newSize);
        dataObject = Util.copyOf(dataObject,newSize);
    }

    private void growMethodStack()
    {
        int newSize = method.length * 2;
        method = Util.copyOf(method, newSize);
    }
}
