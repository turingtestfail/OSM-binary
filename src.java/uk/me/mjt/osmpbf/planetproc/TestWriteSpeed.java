
package uk.me.mjt.osmpbf.planetproc;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Benchmarks the impact of different sizes of BufferedOutputStream. Turns out
 * (for my OS and SSD) there's not much benefit to going above 32kb. See
 * https://docs.google.com/spreadsheets/d/1RlisyiFGYeQpJzBkTEq53huecXbUwXnVj_WsfFDuSvo/edit?usp=sharing
 * for graphs.
 * @author mtandy
 */
public class TestWriteSpeed {
    public static final long UNCONTRACTED = Long.MAX_VALUE;
    
    String outFilePrefix = "/home/mtandy/Documents/contraction hierarchies/binary-test/planet";
    
    DataOutputStream waysOutput = null;
    DataOutputStream nodesOutput = null;
    
    
    public void doIt(int kblocks) throws IOException {
        waysOutput = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFilePrefix+"-ways.dat"),1024*kblocks));
        nodesOutput = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFilePrefix+"-nodes.dat"),1024*kblocks));
        
        long startTime = System.currentTimeMillis();
        
        for (int i=0 ; i<1000000 ; i++) {
            waysOutput.writeLong(i);
            waysOutput.writeLong(i);
            waysOutput.writeLong(i);
            waysOutput.writeLong(i);
            // Contraction details (spoiler: It's uncontracted)
            waysOutput.writeBoolean(false);
            waysOutput.writeLong(0);
            waysOutput.writeLong(0);
            
            nodesOutput.writeLong(i);
            nodesOutput.writeLong(i);
            nodesOutput.writeBoolean(false);
            nodesOutput.writeDouble(0);
            nodesOutput.writeDouble(0);
        }
        
        waysOutput.close();
        nodesOutput.close();
        
        long duration = System.currentTimeMillis()-startTime;
        
        System.out.println(kblocks + " " + duration);
    }
    
    public static void main(String[] args) throws IOException {
        for (int j=0 ; j<4 ; j++) {
            for (int i=128 ; i>0 ; i-=2) {
                new TestWriteSpeed().doIt(i);
            }
        }
    }

}
