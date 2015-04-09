
package uk.me.mjt.osmpbf.findexamples;

import crosby.binary.Osmformat.DenseNodes;
import crosby.binary.file.BlockInputStream;
import crosby.binary.file.BlockReaderAdapter;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.TreeMap;
import uk.me.mjt.osmpbf.planetproc.SimplePbfParser;


public class FindNodeExamples extends SimplePbfParser {
    
    public static void main(String[] args) throws Exception {
        String filename = "/home/mtandy/Documents/contraction hierarchies/hertfordshire-latest.osm.pbf";
        
        InputStream input = new BufferedInputStream(new FileInputStream(filename));
        BlockReaderAdapter brad = new FindNodeExamples();
        new BlockInputStream(input, brad).process();
    }
    
    private final String targetKey = "barrier";
    private final TreeMap<String,Long> valueCount = new TreeMap();
    private final TreeMap<String,Long> exampleNode = new TreeMap();
    
    @Override
    protected void parseDense(DenseNodes nodes) {
        long lastId = 0;
        int keyValIdx = 0;
        
        for (int i = 0; i < nodes.getIdCount(); i++) {
            lastId += nodes.getId(i);
            if (nodes.getKeysValsCount() > 0) {
                
                while (nodes.getKeysVals(keyValIdx) != 0) {
                    int keyId = nodes.getKeysVals(keyValIdx++);
                    int valueId = nodes.getKeysVals(keyValIdx++);
                    String key = getStringById(keyId);
                    String value = getStringById(valueId);
                    //System.out.println("Node: " + lastId + " " + key + " / " + value);
                    if (targetKey.equalsIgnoreCase(key)) {
                        if (valueCount.containsKey(value)) {
                            valueCount.put(value, valueCount.get(value)+1);
                        } else {
                            valueCount.put(value, 1L);
                            exampleNode.put(value, lastId);
                        }
                    }
                }
                keyValIdx++; // Zero delimiter
                
            }
        }
    }
    
    public void complete() {
        System.out.println("For key: " + targetKey);
        System.out.println("Value counts: " + valueCount);
        System.out.println("Example nodes: " + exampleNode);
    }

}

