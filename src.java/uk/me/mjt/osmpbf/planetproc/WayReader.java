
package uk.me.mjt.osmpbf.planetproc;

import crosby.binary.Osmformat;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class WayReader extends AbstractReader<SimpleWay> {
    
    public WayReader(String fileToRead) {
        super(fileToRead);
    }
    
    public Iterator<SimpleWay> iterator() {
        return new WayReaderIterator();
    }
    
    private class WayReaderIterator extends ReaderIterator {
        
        @Override
        PbfReader makeReader(File f) {
            return new WayFileIterator(f);
        }
        
    }
    
    private class WayFileIterator extends PbfReader {
        
        private long readSoFar = 0;
        private long startTime = -1;

        public WayFileIterator(File f) {
            super(f);
        }
        
        protected void parseWays(List<Osmformat.Way> ways) {
            if (startTime == -1) startTime = System.currentTimeMillis();
            
            ArrayList<SimpleWay> out = new ArrayList(ways.size());
            for (Osmformat.Way w : ways) {
                long wayId = w.getId();
                List<Long> nodeIds = unDeltaRefs(w.getRefsList());
                String oneway = getTagByKey("oneway", w);
                String highway = getTagByKey("highway", w);
                String junction = getTagByKey("junction", w);
                
                out.add(new SimpleWay(wayId, nodeIds, oneway, highway, junction));
                
                readSoFar++;
                if (readSoFar % 5000000L == 0 || readSoFar <= 10) {
                    long procTime = System.currentTimeMillis()-startTime;
                    double nodesPerSecond = (1000.0 * readSoFar) / procTime;
                    System.out.println("Read " + readSoFar + " ways in " + procTime + "ms, " + nodesPerSecond + " per second");
                }
            }
            offer(out);
        }
    }
}
