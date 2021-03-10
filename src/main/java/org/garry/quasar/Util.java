package org.garry.quasar;

public class Util {

    public static int[] copyOf(int[] src, int size) {
        int[] dst = new int[size];
        System.arraycopy(src, 0, dst, 0, Math.min(src.length, size));
        return dst;
    }

    public static long[] copyOf(long[] src, int size) {
        long[] dst = new long[size];
        System.arraycopy(src, 0, dst, 0, Math.min(src.length, size));
        return dst;
    }

    public static Object[] copyOf(Object[] src, int size) {
        Object[] dst = new Object[size];
        System.arraycopy(src, 0, dst, 0, Math.min(src.length, size));
        return dst;

    }
}
