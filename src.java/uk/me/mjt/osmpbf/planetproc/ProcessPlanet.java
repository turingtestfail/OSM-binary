
package uk.me.mjt.osmpbf.planetproc;


public class ProcessPlanet {
    
    public void doIt() {
        //String filename = "/home/mtandy/Documents/contraction hierarchies/osm-pbf-files/planet-150309.osm.pbf";
        String filename = "/home/mtandy/Documents/contraction hierarchies/hertfordshire-latest.osm.pbf";
        
        NodeReader nr = new NodeReader(filename);
        
        storeNodes(nr);
        //countNodes(nr);
        
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
    
    public static void main(String[] args) {
        new ProcessPlanet().doIt();
    }

}
