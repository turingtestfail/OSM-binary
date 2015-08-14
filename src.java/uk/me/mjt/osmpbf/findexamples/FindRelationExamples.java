
package uk.me.mjt.osmpbf.findexamples;

import crosby.binary.Osmformat;
import crosby.binary.Osmformat.DenseNodes;
import crosby.binary.file.BlockInputStream;
import crosby.binary.file.BlockReaderAdapter;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.TreeMap;
import uk.me.mjt.osmpbf.planetproc.SimplePbfParser;


public class FindRelationExamples extends SimplePbfParser {
    
    public static void main(String[] args) throws Exception {
        //String filename = "/home/mtandy/Documents/contraction hierarchies/hertfordshire-latest.osm.pbf";
        String filename ="/home/mtandy/Documents/contraction hierarchies/osm-pbf-files/great-britain-150409.osm.pbf";
        
        InputStream input = new BufferedInputStream(new FileInputStream(filename));
        BlockReaderAdapter brad = new FindRelationExamples();
        new BlockInputStream(input, brad).process();
    }
    
    //private final String targetKey = "barrier";
    private final TreeMap<String,Long> valueCount = new TreeMap();
    private final TreeMap<String,Long> exampleNode = new TreeMap();
    
    protected void parseRelations(List<Osmformat.Relation> rels) {
        for (Osmformat.Relation r : rels) {
            if ("restriction".equals(getTagByKey("type", r))) {
                processRestriction(r);
            }
        }
    }
    
    void processRestriction(Osmformat.Relation r) {
        System.out.println("Restriction " + r.getId());
        
        for (int i = 0; i < r.getKeysCount(); i++) {
            String key = getStringById(r.getKeys(i));
            String value = getStringById(r.getVals(i));
            System.out.println("  " + key + " -> " + value);
            
            if ("restriction".equals(key)) {
                String valueAndMemberCount = value + "/" + r.getMemidsCount();
                if (valueCount.containsKey(valueAndMemberCount)) {
                    valueCount.put(valueAndMemberCount, valueCount.get(valueAndMemberCount) + 1);
                } else {
                    valueCount.put(valueAndMemberCount, 1L);
                    exampleNode.put(valueAndMemberCount, r.getId());
                }
            }
        }
        
        
        
        int viaNodeCount = 0;
        int viaWayCount = 0;
        int fromWayCount = 0;
        int toWayCount = 0;
        int otherThingsCount = 0;
        
        long lastMemId=0;
        for (int i=0 ; i<r.getMemidsCount() ; i++) {
            long thisMemId = lastMemId+r.getMemids(i);
            Osmformat.Relation.MemberType memType = r.getTypes(i);
            String role = getStringById(r.getRolesSid(i));
            System.out.println("  " + memType + " " + thisMemId + " " + role);
            
            if (memType == Osmformat.Relation.MemberType.WAY) {
                if ("via".equals(role)) {
                    viaWayCount++;
                } else if ("to".equals(role)) {
                    toWayCount++;
                } else if ("from".equals(role)) {
                    fromWayCount++;
                } else {
                    otherThingsCount++;
                }
            } else if (memType == Osmformat.Relation.MemberType.NODE) {
                if ("via".equals(role)) {
                    viaNodeCount++;
                } else {
                    otherThingsCount++;
                }
            } else {
                otherThingsCount++;
            }
            
            if (fromWayCount>=1 && toWayCount>=1 && viaWayCount > 0 /*&& viaNodeCount>viaWayCount /*&& otherThingsCount==0*/) {
                System.out.println("!!!");
            }
            
            lastMemId=thisMemId;
        }
    }
    
    public String getTagByKey(String key, Osmformat.Relation r) {
        for (int i = 0; i < r.getKeysCount(); i++) {
            if (key.equals(getStringById(r.getKeys(i)))) {
                return getStringById(r.getVals(i));
            }
        }
        return null;
    }
    
    public void complete() {
        //System.out.println("For key: " + targetKey);
        System.out.println("Value counts: " + valueCount);
        System.out.println("Example nodes: " + exampleNode);
    }

}

