
package uk.me.mjt.osmpbf.planetproc;

import java.util.*;


public class SimpleTurnRestriction {
    // See https://wiki.openstreetmap.org/wiki/Relation:restriction
    private static final Set<String> WELL_KNOWN_TURN_RESTRICTIONS = new HashSet<String>() {{
        add("no_left_turn");
        add("no_right_turn");
        add("no_u_turn");
        add("no_straight_on");
        add("only_left_turn");
        add("only_right_turn");
        add("only_straight_on");
        
    }};
    
    long turnRestrictionId;
    String restrictionType;
    final List<Long> fromWayIds = new ArrayList();
    final List<Long> viaWayIds = new ArrayList();
    final List<Long> toWayIds = new ArrayList();
    final List<Long> viaNodeIds = new ArrayList();
    
    public boolean isWellKnownType() {
        return WELL_KNOWN_TURN_RESTRICTIONS.contains(restrictionType);
    }
}
