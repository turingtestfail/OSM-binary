package uk.me.mjt.osmpbf.planetproc;

import crosby.binary.BinaryParser;
import crosby.binary.Osmformat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SimplePbfParser extends BinaryParser {

    @Override
    protected void parseRelations(List<Osmformat.Relation> rels) { }

    @Override
    protected void parse(Osmformat.HeaderBlock header) { }

    public void complete() { }
    
    @Override
    protected void parseDense(Osmformat.DenseNodes nodes) { }

    @Override
    protected void parseNodes(List<Osmformat.Node> nodes) { }
    
    @Override
    protected void parseWays(List<Osmformat.Way> ways) { }
    
    static List<Long> unDeltaRefs(List<Long> input) {
        List<Long> output = new ArrayList(input.size());
        long thisRef = 0;
        for (Long deltaRef : input) {
            thisRef += deltaRef;
            output.add(thisRef);
        }
        return Collections.unmodifiableList(output);
    }
    
    String getTagByKey(String key, Osmformat.Way w) {
        for (int i = 0; i < w.getKeysCount(); i++) {
            if (key.equals(getStringById(w.getKeys(i)))) {
                return getStringById(w.getVals(i));
            }
        }
        return null;
    }
    
    
    // See http://wiki.openstreetmap.org/wiki/Key:highway
    private static final Set<String> navigableHighwayTypes = new HashSet<String>() {{
        add("motorway");
        add("motorway_link");
        add("trunk");
        add("trunk_link");
        add("primary");
        add("primary_link");
        add("secondary");
        add("secondary_link");
        add("tertiary");
        add("tertiary_link");
        add("tertiary");
        add("living_street");
        add("residential");
        add("unclassified");
        add("service");
        add("road");
        add("services");
        add("access");
    }};
    
    public boolean isNavigable(Osmformat.Way w) {
        String highwayType = getTagByKey("highway", w);
        return (highwayType != null && navigableHighwayTypes.contains(highwayType));
    }

    boolean isNavigableForwards(Osmformat.Way w) {
        return (getNavigableDirection(w) >= 0);
    }

    boolean isNavigableBackwards(Osmformat.Way w) {
        return (getNavigableDirection(w) <= 0);
    }

    int getNavigableDirection(Osmformat.Way w) {
        String oneway = getTagByKey("oneway", w);
        String highway = getTagByKey("highway", w);
        String junction = getTagByKey("junction", w);

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
