
package uk.me.mjt.osmpbf.planetproc;

import java.util.HashMap;


public class HashNodeStore implements NodeStore {
    HashMap<Long, SimpleNode> hm = new HashMap(1000000);

    public SimpleNode get(long index) {
        return hm.get(index);
    }

    public void put(SimpleNode node) {
        hm.put(node.getId(), node);
    }

}
