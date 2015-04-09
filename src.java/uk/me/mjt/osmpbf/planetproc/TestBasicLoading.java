
package uk.me.mjt.osmpbf.planetproc;

import java.util.List;
import java.util.TreeMap;


public class TestBasicLoading {
    
    public void doIt() {
        //String filename = "/home/mtandy/Documents/contraction hierarchies/osm-pbf-files/planet-150309.osm.pbf";
        String filename = "/home/mtandy/Documents/contraction hierarchies/hertfordshire-latest.osm.pbf";
        
        NodeReader nr = new NodeReader(filename);
        
        countNodes(nr);
        storeNodes(nr);
        
        WayReader wr = new WayReader(filename);
        countWays(wr);
    }
    
    private void countNodes(NodeReader nr) {
        long nodeCount = 0;
        long maxNodeId = 0;

        for (SimpleNode node : nr) {
            maxNodeId = Math.max(maxNodeId, node.getId());
            nodeCount++;
        }

        System.out.println("Node count: " + nodeCount);
        System.out.println("Max node ID: " + maxNodeId);
    }
    
    private void storeNodes(NodeReader nr) {
        BigNodeStore bns = new BigNodeStore(3400000000L);
        for (SimpleNode node : nr) {
            bns.put(node);
        }
        
        for (SimpleNode node : nr) {
            SimpleNode copy = bns.get(node.getId());
            if (!copy.equals(node)) {
                System.out.println("Node and copy unequal?! " + node + " vs " + copy);
            }
        }
    }
    
    private void countWays(WayReader wr) {
        long count = 0;
        long maxId = 0;

        for (SimpleWay w : wr) {
            maxId = Math.max(maxId, w.getWayId());
            count++;
        }

        System.out.println("Count: " + count);
        System.out.println("Max ID: " + maxId);
    }
    
    
    public static void main(String[] args) {
        new TestBasicLoading().doIt();
    }

}
