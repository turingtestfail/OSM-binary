package uk.me.mjt.osmpbf.planetproc;

import crosby.binary.BinaryParser;
import crosby.binary.Osmformat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SimplePbfParser extends BinaryParser {

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
    
    public String getTagByKey(String key, Osmformat.Way w) {
        for (int i = 0; i < w.getKeysCount(); i++) {
            if (key.equals(getStringById(w.getKeys(i)))) {
                return getStringById(w.getVals(i));
            }
        }
        return null;
    }
    
}
