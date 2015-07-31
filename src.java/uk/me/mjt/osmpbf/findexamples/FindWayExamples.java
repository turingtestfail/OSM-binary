
package uk.me.mjt.osmpbf.findexamples;

import crosby.binary.Osmformat;
import crosby.binary.Osmformat.DenseNodes;
import crosby.binary.file.BlockInputStream;
import crosby.binary.file.BlockReaderAdapter;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import uk.me.mjt.osmpbf.planetproc.SimplePbfParser;
import uk.me.mjt.osmpbf.planetproc.SimpleWay;


public class FindWayExamples extends SimplePbfParser {
    
    public static void main(String[] args) throws Exception {
        String filename = "/home/mtandy/Documents/contraction hierarchies/hertfordshire-latest.osm.pbf";
        
        InputStream input = new BufferedInputStream(new FileInputStream(filename));
        BlockReaderAdapter brad = new FindWayExamples();
        new BlockInputStream(input, brad).process();
    }
    
    private final String targetKey = "motor_vehicle";
    private final TreeMap<String,Long> valueCount = new TreeMap();
    private final TreeMap<String,Long> examples = new TreeMap();
    
    protected void parseWays(List<Osmformat.Way> ways) {
        for (Osmformat.Way w : ways) {
            long wayId = w.getId();
            
            String value = getTagByKey(targetKey, w);
            if (value != null && getTagByKey("highway", w)!=null) {
                if (valueCount.containsKey(value)) {
                    valueCount.put(value, valueCount.get(value) + 1);
                } else {
                    valueCount.put(value, 1L);
                    examples.put(value, wayId);
                }
            }
        }
    }
    
    public void complete() {
        System.out.println("For key: " + targetKey);
        System.out.println("Value counts: " + valueCount);
        System.out.println("Examples: " + examples);
    }

}

