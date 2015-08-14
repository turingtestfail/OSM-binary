
package uk.me.mjt.osmpbf.planetproc;

import crosby.binary.Osmformat;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class TurnRestrictionReader extends AbstractReader<SimpleTurnRestriction> {
    
    public TurnRestrictionReader(String fileToRead) {
        super(fileToRead);
    }
    
    public Iterator<SimpleTurnRestriction> iterator() {
        return new TurnRestrictionReaderIterator();
    }
    
    private class TurnRestrictionReaderIterator extends ReaderIterator {
        @Override
        PbfReader makeReader(File f) {
            return new TurnRestrictionIterator(f);
        }
    }
    
    private class TurnRestrictionIterator extends PbfReader {
        
        private long readSoFar = 0;
        private long startTime = -1;

        public TurnRestrictionIterator(File f) {
            super(f);
        }
        
        protected void parseRelations(List<Osmformat.Relation> rels) {
            if (!rels.isEmpty() && startTime == -1) {
                startTime = System.currentTimeMillis();
            }

            ArrayList<SimpleTurnRestriction> out = new ArrayList(rels.size());
            for (Osmformat.Relation r : rels) {
                int otherThingsCount = 0;
                
                if ("restriction".equals(getTagByKey("type", r))) {
                    SimpleTurnRestriction str = new SimpleTurnRestriction();
                    str.restrictionType = getTagByKey("restriction", r);
                    str.turnRestrictionId = r.getId();

                    long lastMemId = 0;
                    for (int i = 0; i < r.getMemidsCount(); i++) {
                        long thisMemId = lastMemId + r.getMemids(i);
                        Osmformat.Relation.MemberType memType = r.getTypes(i);
                        String role = getStringById(r.getRolesSid(i));
                        
                        if (memType == Osmformat.Relation.MemberType.WAY) {
                            if ("via".equals(role)) {
                                str.viaWayIds.add(thisMemId);
                            } else if ("to".equals(role)) {
                                str.toWayIds.add(thisMemId);
                            } else if ("from".equals(role)) {
                                str.fromWayIds.add(thisMemId);
                            } else {
                                otherThingsCount++;
                            }
                        } else if (memType == Osmformat.Relation.MemberType.NODE) {
                            if ("via".equals(role)) {
                                str.viaNodeIds.add(thisMemId);
                            } else {
                                otherThingsCount++;
                            }
                        } else {
                            otherThingsCount++;
                        }
                        
                        lastMemId = thisMemId;
                    }

                    out.add(str);

                    readSoFar++;
                    if (readSoFar % 5000000L == 0 || readSoFar <= 10) {
                        long procTime = System.currentTimeMillis() - startTime;
                        double nodesPerSecond = (1000.0 * readSoFar) / procTime;
                        System.out.println("Read " + readSoFar + " relations in " + procTime + "ms, " + nodesPerSecond + " per second");
                    }
                }
            }
            offer(out);
        }
    }
    
}
