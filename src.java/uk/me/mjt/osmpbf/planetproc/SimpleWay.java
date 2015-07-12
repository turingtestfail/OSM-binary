
package uk.me.mjt.osmpbf.planetproc;

import crosby.binary.Osmformat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class SimpleWay {
    private final long wayId;
    private final List<Long> nodeIds;
    private final String oneway;
    private final String highway;
    private final String junction;
    
    // See http://wiki.openstreetmap.org/wiki/Key:highway
    private static final Set<String> NAVIGABLE_HIGHWAY_TYPES = new HashSet<String>() {{
        add("motorway");
        add("motorway_link");
        add("trunk"); // Euston rd - green on OSM
        add("trunk_link");
        add("primary"); // Grays inn rd - red on OSM
        add("primary_link");
        add("secondary"); // Judd st - orange on OSM
        add("secondary_link");
        add("tertiary"); // Cartwright gardens - yellow on OSM
        add("tertiary_link");
        add("living_street");
        add("residential");
        add("unclassified");
        add("service");
        add("road");
        add("services");
        add("access");
    }};
    
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
    
    public boolean isNavigable() {
        return (highway != null && NAVIGABLE_HIGHWAY_TYPES.contains(highway));
    }
    
    public boolean isNavigableForwards() {
        return isNavigable() && (getNavigableDirection() >= 0);
    }

    public boolean isNavigableBackwards() {
        return isNavigable() && (getNavigableDirection() <= 0);
    }

    private int getNavigableDirection() {
        // https://wiki.openstreetmap.org/wiki/OSM_tags_for_routing#Oneway
        if ("no".equals(oneway)) {
            return 1;
        } else if ("yes".equals(oneway) || "true".equals(oneway) || "1".equals(oneway)
                || "roundabout".equals(junction) || "motorway".equals(highway)
                || "motorway_link".equals(highway)) {
            return 1;
        } else if ("-1".equals(oneway)) {
            return -1;
        } else {
            return 0;
        }
    }
    
}
