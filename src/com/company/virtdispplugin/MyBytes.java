package com.company.virtdispplugin;

public class MyBytes {
    byte[] buffer;
    int length;

    MyBytes (byte[] b) {
        buffer = b;
        length = b.length;
    }
    MyBytes (int l) {
        buffer = new byte[l];
        length = l;
    }
    byte[] getRaw() {
        return buffer;
    }

    byte[] get(int i, int l) {
        int ll;
        if (i + l  < length) ll = l;
        else ll = length - i;

        byte[] t = new byte[ll];
        System.arraycopy(buffer,i,t,0,ll);
        return t;
    }

    byte get(int i) {
        if (i < length) return buffer[i];
        else return 0;
    }

    byte set(int i, byte b) {
        if (i < length) {
            buffer[i] = b;
            return buffer[i];
        }
        else return 0;
    }

    public boolean containsAt(int idx, byte[] b) {
        if (idx + b.length > length) return false;
        for (int i = 0; i < b.length; i++)
            if (b[i] != buffer[idx + i]) return false;
        return true;
    }

    public  int remove(int n) {

        if (n >= length) {
            buffer = new byte[0];
            length = 0;
            return 0;
        }
        length -= n;
        byte[] t = new byte[length];
        System.arraycopy(buffer,n, t,0, length);
        buffer = t;
        return length;
    }
    public int append (byte[] b) {
        byte[] t = new byte[length + b.length];
        System.arraycopy(buffer,0, t,0, length);
        System.arraycopy(b,0, t, length, b.length);
        length += b.length;
        buffer = t;
        return length;
    }

}
