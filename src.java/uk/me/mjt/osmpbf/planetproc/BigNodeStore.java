
package uk.me.mjt.osmpbf.planetproc;

import java.lang.reflect.Field;
import sun.misc.Unsafe;


public class BigNodeStore {
    
    private static final int SIZE_OF_INT_IN_BYTES = 4;
    private static final int SIZE_OF_BYTE_IN_BYTES = 1; // obv
    private static final int SIZE_OF_NODE_IN_BYTES = 2*SIZE_OF_INT_IN_BYTES+SIZE_OF_BYTE_IN_BYTES;
    
    private final long maxIndex;
    private final Unsafe unsafe = getUnsafe();
    private final long pointer;
    
    public BigNodeStore(long maxIndex) {
        this.maxIndex = maxIndex;
        long memoryRequired = SIZE_OF_NODE_IN_BYTES * (maxIndex+1);
        System.out.println("Attempting to allocate " + memoryRequired + " bytes.");
        pointer = unsafe.allocateMemory(memoryRequired);
        unsafe.setMemory(pointer, memoryRequired, (byte)0x0);
        System.out.println("Memory allocated and zeroed.");
    }
    
    public SimpleNode get(long index) {
        if (index<0 || index > maxIndex) 
            throw new IllegalArgumentException("Index out of range?");
        
        long startOffset = indexToOffset(index);
        int latMillionths = unsafe.getInt(startOffset);
        int lonMillionths = unsafe.getInt(startOffset+SIZE_OF_INT_IN_BYTES);
        byte flag = unsafe.getByte(startOffset+2*SIZE_OF_INT_IN_BYTES);
        
        if (flag == 0) {
            return null;
        } else {
            SimpleNode sn = new SimpleNode(index, latMillionths, lonMillionths, false);
            sn.setCountflag(flag);
            return sn;
        }
    }
    
    public void put(SimpleNode node) {
        if (node.getId()<0 || node.getId() > maxIndex) 
            throw new IllegalArgumentException("Index out of range? Asked to put " + node.getId());
        
        long startOffset = indexToOffset(node.getId());
        unsafe.putInt(startOffset, node.getLatMillionths());
        unsafe.putInt(startOffset+SIZE_OF_INT_IN_BYTES, node.getLonMillionths());
        unsafe.putByte(startOffset+2*SIZE_OF_INT_IN_BYTES, node.getCountflag());
    }
    
    private long indexToOffset(long index) {
        return pointer + (index * SIZE_OF_NODE_IN_BYTES);
    }
    
    private static Unsafe getUnsafe() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe unsafe = (Unsafe) theUnsafe.get(null);
            return unsafe;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
