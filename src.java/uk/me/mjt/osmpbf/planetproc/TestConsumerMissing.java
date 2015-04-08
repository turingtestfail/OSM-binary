
package uk.me.mjt.osmpbf.planetproc;

import java.util.Iterator;


public class TestConsumerMissing {
    
    public void doIt() {
        String filename = "/home/mtandy/Documents/contraction hierarchies/hertfordshire-latest.osm.pbf";
        
        NodeReader nr = new NodeReader(filename);
        Iterator<SimpleNode> isn = nr.iterator();
        
    }
    
    public static void main(String[] args) {
        new TestConsumerMissing().doIt();
        
    }

}
