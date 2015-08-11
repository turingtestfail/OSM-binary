
package uk.me.mjt.osmpbf.planetproc;

import crosby.binary.Osmformat;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class NodeReader extends AbstractReader<SimpleNode> {
    
    public NodeReader(String fileToRead) {
        super(fileToRead);
    }
    
    public Iterator<SimpleNode> iterator() {
        return new NodeReaderIterator();
    }
    
    private class NodeReaderIterator extends ReaderIterator {
        
        @Override
        PbfReader makeReader(File f) {
            return new NodeFileReader(f);
        }
        
    }
    
    private class NodeFileReader extends PbfReader {
        
        private long nodesReadSoFar = 0;
        private long startTime = -1;

        public NodeFileReader(File f) {
            super(f);
        }
        
        @Override
        protected void parseDense(Osmformat.DenseNodes nodes) {
            if (startTime == -1) startTime = System.currentTimeMillis();
            long lastId = 0;
            long lastLat = 0;
            long lastLon = 0;
            int keyValIdx = 0;
            
            ArrayList<SimpleNode> out = new ArrayList(nodes.getIdCount());
            for (int i = 0; i < nodes.getIdCount(); i++) {
                lastId += nodes.getId(i);
                lastLat += nodes.getLat(i);
                lastLon += nodes.getLon(i);
                int latMillionths = (int)Math.round(1000000*parseLat(lastLat));
                int lonMillionths = (int)Math.round(1000000*parseLon(lastLon));
                
                boolean barrier=false;
                
                if (nodes.getKeysValsCount() > 0) {
                
                    while (nodes.getKeysVals(keyValIdx) != 0) {
                        int keyId = nodes.getKeysVals(keyValIdx++);
                        int valueId = nodes.getKeysVals(keyValIdx++);
                        String key = getStringById(keyId);
                        String value = getStringById(valueId);
                        if (key.equals("barrier") && (value.equals("gate") || value.equals("bollard") || value.equals("lift_gate"))) {
                            barrier=true;
                        }
                    }
                    keyValIdx++; // Zero delimiter

                }
                
                
                SimpleNode sn = new SimpleNode(lastId, latMillionths, lonMillionths, barrier);
                out.add(sn);
                
                nodesReadSoFar++;
                if (nodesReadSoFar % 5000000L == 0 || nodesReadSoFar <= 10) {
                    long procTime = System.currentTimeMillis()-startTime;
                    double nodesPerSecond = (1000.0 * nodesReadSoFar) / procTime;
                    System.out.println("Read " + nodesReadSoFar + " nodes in " + procTime + "ms, " + nodesPerSecond + " per second");
                }
            }
            offer(out);
        }

        @Override
        protected void parseNodes(List<Osmformat.Node> nodes) {
            if (!nodes.isEmpty()) System.out.println("Non-dense nodes seen!");
            if (startTime == -1) startTime = System.currentTimeMillis();
            ArrayList<SimpleNode> out = new ArrayList(nodes.size());
            for (Osmformat.Node n : nodes) {
                int latMillionths = (int)Math.round(1000000*parseLat(n.getLat()));
                int lonMillionths = (int)Math.round(1000000*parseLon(n.getLon()));
                SimpleNode sn = new SimpleNode(n.getId(), latMillionths, lonMillionths, false /*fixme*/);
                out.add(sn);
                
                nodesReadSoFar++;
                if (nodesReadSoFar % 5000000L == 0 || nodesReadSoFar <= 10) {
                    long procTime = System.currentTimeMillis()-startTime;
                    double nodesPerSecond = (1000.0 * nodesReadSoFar) / procTime;
                    System.out.println("Read " + nodesReadSoFar + " nodes in " + procTime + "ms, " + nodesPerSecond + " per second");
                }
            }
            offer(out);
        }
    }

}
