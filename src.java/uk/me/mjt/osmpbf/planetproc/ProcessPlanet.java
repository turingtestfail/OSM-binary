
package uk.me.mjt.osmpbf.planetproc;

import java.util.TreeMap;


public class ProcessPlanet {
    
    public void doIt() {
        //String filename = "/home/mtandy/Documents/contraction hierarchies/osm-pbf-files/planet-150309.osm.pbf";
        String filename = "/home/mtandy/Documents/contraction hierarchies/hertfordshire-latest.osm.pbf";
        
        //NodeReader nr = new NodeReader(filename);
        
        //storeNodes(nr);
        //countNodes(nr);
        
        //WayReader wr = new WayReader(filename);
        //countWays(wr);
        waysPerNode(filename);
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
    
    private void waysPerNode(String filename) {
        NodeReader nr = new NodeReader(filename);
        
        BigNodeStore bns = new BigNodeStore(3400000000L);
        for (SimpleNode node : nr) {
            bns.put(node);
        }
        
        WayReader wr = new WayReader(filename);
        for (SimpleWay w : wr) {
            if (w.getNodeIds().contains(104443L)) {
                System.out.println(w.getNodeIds());
            }
            for (long nodeId : w.getNodeIds()) {
                SimpleNode node = bns.get(nodeId);
                node.incrementWayCount();
                bns.put(node);
                if (nodeId == 104443 ) {
                    System.out.println("Way " + w.getWayId() + " Node " + node);
                }
            }
        }
        
        TreeMap<Integer,Integer> distribution = new TreeMap();
        TreeMap<Integer,Long> example = new TreeMap();
        
        for (SimpleNode node : nr) {
            SimpleNode readback = bns.get(node.getId());
            int wayCount = readback.getWayCount();
            if (distribution.containsKey(wayCount)) {
                distribution.put(wayCount, distribution.get(wayCount)+1);
            } else {
                distribution.put(wayCount, 1);
                example.put(wayCount, readback.getId());
            }
            //System.out.println("Node: " + readback.getId() + " ways: " + readback.getWayCount());
        }
        
        System.out.println(distribution);
        System.out.println(example);
    }
    
    public static void main(String[] args) {
        new ProcessPlanet().doIt();
    }

}
