
package uk.me.mjt.osmpbf.planetproc;

import java.util.List;


public class SimpleWay {
    private final long wayId;
    private final List<Long> nodeIds;
    private final String oneway;
    private final String highway;
    private final String junction;

    public SimpleWay(long wayId, List<Long> nodeIds, String oneway, String highway, String junction) {
        this.wayId = wayId;
        this.nodeIds = nodeIds;
        this.oneway = oneway;
        this.highway = highway;
        this.junction = junction;
    }

    public long getWayId() {
        return wayId;
    }

    public List<Long> getNodeIds() {
        return nodeIds;
    }

    public String getOneway() {
        return oneway;
    }

    public String getHighway() {
        return highway;
    }

    public String getJunction() {
        return junction;
    }
    
}
