
package uk.me.mjt.osmpbf.planetproc;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;


public class ProcessPlanet {
    public static final long UNCONTRACTED = Long.MAX_VALUE;
    public static final int OUTPUT_BUFFER_SIZE = 32 * 1024;
    
    //String filename = "/home/mtandy/Documents/contraction hierarchies/hertfordshire-latest.osm.pbf";
    //String filename ="/home/mtandy/Documents/contraction hierarchies/osm-pbf-files/planet-150309.osm.pbf";
    //String filename = "/home/mtandy/Documents/contraction hierarchies/osm-pbf-files/greater-london-150813.osm.pbf";
    String filename ="/home/mtandy/Documents/contraction hierarchies/osm-pbf-files/great-britain-150409.osm.pbf";
    //String outFilePrefix = "/home/mtandy/Documents/contraction hierarchies/binary-test/hertfordshire";
    //String outFilePrefix = "/home/mtandy/Documents/contraction hierarchies/binary-test/planet";
    //String outFilePrefix = "/tmp/osm-bin-planet";
    //String outFilePrefix = "/home/mtandy/Documents/contraction hierarchies/binary-test/greater-london";
    String outFilePrefix = "/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain";
    
    //HashSet<Long> turnRestrictionRelatedNodes = new HashSet();
    HashSet<Long> turnRestrictionRelatedWayIds = new HashSet();
    HashMap<Long,OsmWay> turnRestrictionRelatedWaysById = new HashMap();
    
    BigNodeStore nodesWithCounts = null;
    DataOutputStream waysOutput = null;
    DataOutputStream nodesOutput = null;
    DataOutputStream turnRestrictionOutput = null;
    
    long wayId = 1;
    
    long directedRoadSegmentCount = 0;
    long nodeCount = 0;
    long turnRestrictionCount = 0;
    
    public void doIt() throws IOException {
        nodesWithCounts = waysPerNode();
        
        identifyWaysInTurnRestrictions();
        
        waysOutput = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFilePrefix+"-ways.dat"),OUTPUT_BUFFER_SIZE));
        nodesOutput = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFilePrefix+"-nodes.dat"),OUTPUT_BUFFER_SIZE));
        extractRoadSegmentsAndNodes();
        waysOutput.close();
        nodesOutput.close();
        
        turnRestrictionOutput = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFilePrefix+"-turnrestrictions.dat"),OUTPUT_BUFFER_SIZE));
        extractReasonableLookingTurnRestrictions();
        turnRestrictionOutput.close();
        
        System.out.println("Directed road segments written: " + directedRoadSegmentCount);
        System.out.println("Nodes written: " + nodeCount);
        System.out.println("Turn restrictions written: " + turnRestrictionCount);
    }
    
    private void identifyWaysInTurnRestrictions() {
        
        TurnRestrictionReader trr = new TurnRestrictionReader(filename);
        for (SimpleTurnRestriction tr : trr) {
            turnRestrictionRelatedWayIds.addAll(tr.fromWayIds);
            turnRestrictionRelatedWayIds.addAll(tr.viaWayIds);
            turnRestrictionRelatedWayIds.addAll(tr.toWayIds);
        }
    }
    
    private BigNodeStore waysPerNode() {
        NodeReader nr = new NodeReader(filename);
        
        BigNodeStore bns = new BigNodeStore(3700000000L);
        for (SimpleNode node : nr) {
            bns.put(node);
        }
        
        WayReader wr = new WayReader(filename);
        for (SimpleWay w : wr) {
            if (w.isNavigable()) {
                List<Long> nodeIds = w.getNodeIds();
                for (int i=0 ; i<nodeIds.size() ; i++) {
                    SimpleNode node = bns.get(nodeIds.get(i));
                    node.incrementWayCount();
                    if (i>0 && i<nodeIds.size()-1) { // If not at the end of the way...
                        node.incrementWayCount();
                    }
                    bns.put(node);
                }
            }
        }
        
        return bns;
    }
    
    private void extractRoadSegmentsAndNodes() {
        
        WayReader wr = new WayReader(filename);
        for (SimpleWay w : wr) {
            long segmentLength = 0;
            long firstNodeId = -1;
            
            if (w.isNavigable()) {
                OsmWay turnRestrictionParent = null;
                if (turnRestrictionRelatedWayIds.contains(w.getWayId())) {
                    turnRestrictionParent = new OsmWay();
                    turnRestrictionParent.id = w.getWayId();
                    turnRestrictionParent.simpleWay = w;
                    turnRestrictionRelatedWaysById.put(turnRestrictionParent.id, turnRestrictionParent);
                }
                
                List<Long> nodeIds = w.getNodeIds();
                for (int i=0 ; i<nodeIds.size()-1 ; i++) {
                    boolean lastNode = (i==nodeIds.size()-2);
                    
                    SimpleNode from = nodesWithCounts.get(nodeIds.get(i));
                    SimpleNode to = nodesWithCounts.get(nodeIds.get(i+1));
                    
                    boolean barrier = to.isBarrier();
                    segmentLength += haversineMillimeters(from,to);
                    
                    if (firstNodeId == -1) {
                        firstNodeId = from.getId();
                        processNode(from);
                    }
                    
                    if (to.getWayCount() == 2 && !lastNode && !barrier) {
                        // Just a waypoint - not at the start or end of a way.
                    } else {
                        if (w.isNavigableForwards()) {
                            long graphEdgeId = processRoadSegment(firstNodeId, to.getId(), segmentLength, w.isAccessOnly());
                            if (turnRestrictionParent != null) {
                                WrittenRoadSegment wrs = new WrittenRoadSegment(graphEdgeId, firstNodeId, to.getId(), segmentLength, w.isAccessOnly());
                                turnRestrictionParent.forwardRoadSegments.add(wrs);
                            }
                        }
                        if (w.isNavigableBackwards()) {
                            long graphEdgeId = processRoadSegment(to.getId(), firstNodeId, segmentLength, w.isAccessOnly());
                            if (turnRestrictionParent != null) {
                                WrittenRoadSegment wrs = new WrittenRoadSegment(graphEdgeId, to.getId(), firstNodeId, segmentLength, w.isAccessOnly());
                                turnRestrictionParent.backwardRoadSegments.add(wrs);
                            }
                        }
                        
                        processNode(to);
                        segmentLength=0;
                        firstNodeId=to.getId();
                    }
                    
                }
                if (turnRestrictionParent != null) {
                    Collections.reverse(turnRestrictionParent.backwardRoadSegments);
                }
            }
        }
    }
    
    private long processRoadSegment(long from, long to, long segmentLengthMm, boolean isAccessOnly) {
        try {
            directedRoadSegmentCount++;
            long graphEdgeId = wayId++;
            waysOutput.writeLong(graphEdgeId);
            waysOutput.writeLong(from);
            waysOutput.writeLong(to);
            //waysOutput.writeLong(segmentLengthMm);
            int driveTimeMs = Math.round(segmentLengthMm/13.411f); // 30mph
            waysOutput.writeInt(driveTimeMs);
            // Contraction details (spoiler: It's uncontracted)
            // This is so we can use the same file format for contracted and uncontracted maps.
            boolean isContracted = false;
            int properties = (isContracted?0x01:0x00) | (isAccessOnly?0x02:0x00);
            waysOutput.writeByte(properties);
            
            waysOutput.writeLong(0);
            waysOutput.writeLong(0);
            
            return graphEdgeId;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void processNode(SimpleNode nodeInWay) {
        try {
            
            if (!nodeInWay.isWritten()) {
                nodeCount++;
                nodesOutput.writeLong(nodeInWay.getId());
                nodesOutput.writeLong(UNCONTRACTED);
                boolean isBorderNode = false;
                boolean isBarrier = nodeInWay.isBarrier();
                int properties = (isBorderNode?0x01:0x00) | (isBarrier?0x02:0x00);
                nodesOutput.writeByte(properties);
                nodesOutput.writeDouble(nodeInWay.getLat());
                nodesOutput.writeDouble(nodeInWay.getLon());
                
                nodeInWay.setWritten(true);
                nodesWithCounts.put(nodeInWay);
            }
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void extractReasonableLookingTurnRestrictions() {
        
        TurnRestrictionReader trr = new TurnRestrictionReader(filename);
        for (SimpleTurnRestriction tr : trr) {
            try {
                List<WrittenRoadSegment> turnRestriction = turnRestrictionToGraphEdges(tr);
                writeTurnRestriction(tr.turnRestrictionId, tr.restrictionType, turnRestriction);
                System.out.println("Success for " + tr.turnRestrictionId);
                turnRestrictionCount++;
            } catch (IndecipherableTurnRestrictionException e) {
                System.out.println(e.getMessage() + " for " + tr.turnRestrictionId);
            }
        }
    }
    
    private void writeTurnRestriction(long turnRestrictionId, String restrictionType, List<WrittenRoadSegment> turnRestriction) {
        try {
            turnRestrictionOutput.writeLong(turnRestrictionId);
            turnRestrictionOutput.writeBoolean(restrictionType.startsWith("no_"));
            turnRestrictionOutput.writeInt(turnRestriction.size());
            for (WrittenRoadSegment segment : turnRestriction) {
                turnRestrictionOutput.writeLong(segment.graphEdgeId);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private List<WrittenRoadSegment> turnRestrictionToGraphEdges(SimpleTurnRestriction tr) throws IndecipherableTurnRestrictionException {
        if (!tr.isWellKnownType())
            throw new IndecipherableTurnRestrictionException("Not well known type? " + tr.restrictionType);
        
        if (tr.fromWayIds.isEmpty() || tr.toWayIds.isEmpty())
            throw new IndecipherableTurnRestrictionException("From or to way ID missing?");
        
        if (tr.fromWayIds.size()>1 || tr.toWayIds.size() > 1)
            throw new IndecipherableTurnRestrictionException("More than one from or to way?");
        
        
        OsmWay fromWay = turnRestrictionRelatedWaysById.get(tr.fromWayIds.get(0));
        OsmWay toWay = turnRestrictionRelatedWaysById.get(tr.toWayIds.get(0));
        
        if (fromWay==null || toWay==null)
            throw new IndecipherableTurnRestrictionException("From or two way not navigable?");
        
        List<OsmWay> viaWays = new ArrayList();
        for (long viaWayId : tr.viaWayIds) {
            OsmWay thisVia = turnRestrictionRelatedWaysById.get(viaWayId);
            if (thisVia == null)
                throw new IndecipherableTurnRestrictionException("Via way not navigable?");
            viaWays.add(thisVia);
        }
        
        List<WrittenRoadSegment> result = new ArrayList();
        
        if (viaWays.isEmpty()) {
            long sharedNodeId = getSharedNodeIdIfExactlyOne(fromWay, toWay);
            List<WrittenRoadSegment> startCandidates = findStartSegmentCandidates(fromWay, sharedNodeId);
            List<WrittenRoadSegment> endCandidates = findEndSegmentCandidates(toWay, sharedNodeId);
            WrittenRoadSegmentPair wrsp = resolveAmbiguity(startCandidates, endCandidates, tr.restrictionType);
            result.add(wrsp.from);
            result.add(wrsp.to);
            
        } else {
            List<OsmWay> allWays = new ArrayList();
            allWays.add(fromWay);
            allWays.addAll(viaWays);
            allWays.add(toWay);
            
            List<WrittenRoadSegment> viaRoadSegments = new ArrayList();

            for (int i = 1; i < allWays.size() - 1; i++) {
                OsmWay before = allWays.get(i - 1);
                OsmWay current = allWays.get(i);
                OsmWay after = allWays.get(i + 1);
                
                long idSharedWithBefore = getSharedNodeIdIfExactlyOne(fromWay, current);
                long idSharedWithAfter = getSharedNodeIdIfExactlyOne(current, after);
                
                List<WrittenRoadSegment> segmentsForThisEdge = findSegmentsBetween(current, idSharedWithBefore, idSharedWithAfter);
                if (segmentsForThisEdge==null || segmentsForThisEdge.isEmpty()) {
                    throw new IndecipherableTurnRestrictionException("Difficulty finding segments for edge?");
                }
                
                viaRoadSegments.addAll(segmentsForThisEdge);
            }
            
            long sharedNodeId = getSharedNodeIdIfExactlyOne(fromWay, viaWays.get(0));
            WrittenRoadSegment firstVia = viaRoadSegments.get(0);
            List<WrittenRoadSegment> startCandidates = findStartSegmentCandidates(fromWay, sharedNodeId);
            WrittenRoadSegment startSegment = resolveAmbiguity(startCandidates, Collections.singletonList(firstVia), tr.restrictionType).from;
            
            sharedNodeId = getSharedNodeIdIfExactlyOne(viaWays.get(viaWays.size()-1), toWay);
            WrittenRoadSegment lastVia = viaRoadSegments.get(viaRoadSegments.size()-1);
            List<WrittenRoadSegment> endCandidates = findEndSegmentCandidates(toWay, sharedNodeId);
            WrittenRoadSegment endSegment = resolveAmbiguity(Collections.singletonList(lastVia), endCandidates, tr.restrictionType).to;
            
            result.add(startSegment);
            result.addAll(viaRoadSegments);
            result.add(endSegment);
            
            /*System.out.println("Has vias: " + tr.turnRestrictionId + (viaWays.size()>1?" !!!":""));
            for (WrittenRoadSegment wws : result) {
                SimpleNode from = nodesWithCounts.get(wws.from);
                SimpleNode to = nodesWithCounts.get(wws.to);
                System.out.println("   " + from.getLat()+","+from.getLon() + " -> " + to.getLat() + "," + to.getLon());
            }*/
        }
        
        return result;
    }
    
    private List<WrittenRoadSegment> findStartSegmentCandidates(OsmWay way, long endNodeId) {
        ArrayList<WrittenRoadSegment> result = new ArrayList();
        for (WrittenRoadSegment w : way.allRoadSegments()) {
            if (w.to == endNodeId) {
                result.add(w);
            }
        }
        return result;
    }
    
    private List<WrittenRoadSegment> findEndSegmentCandidates(OsmWay way, long startNodeId) {
        ArrayList<WrittenRoadSegment> result = new ArrayList();
        for (WrittenRoadSegment w : way.allRoadSegments()) {
            if (w.from == startNodeId) {
                result.add(w);
            }
        }
        return result;
    }
    
    private List<WrittenRoadSegment> findSegmentsBetween(OsmWay way, long startNodeId, long endNodeId) {
        List<WrittenRoadSegment> result = findSegmentsBetween(way.forwardRoadSegments, startNodeId, endNodeId);
        if (result == null)
            result = findSegmentsBetween(way.backwardRoadSegments, startNodeId, endNodeId);
        return result;
    }
    
    private List<WrittenRoadSegment> findSegmentsBetween(List<WrittenRoadSegment> segments, long startNodeId, long endNodeId) {
        ArrayList<WrittenRoadSegment> result = new ArrayList();
        
        boolean startSeen = false;
        boolean endSeen = false;
        
        for (WrittenRoadSegment w : segments) {
            if ((startSeen && !endSeen) || (endSeen && !startSeen))
                result.add(w);
            if (w.from == startNodeId) {
                startSeen = true;
                if (!endSeen) result.add(w);
            }
            if (w.to == endNodeId) {
                endSeen = true;
                if (!startSeen) result.add(w);
            }
        }
        
        if (startSeen && endSeen) {
            return result;
        } else {
            return null;
        }
    }
    
    private WrittenRoadSegmentPair resolveAmbiguity(List<WrittenRoadSegment> startCandidates,List<WrittenRoadSegment> endCandidates, String restrictionType) throws IndecipherableTurnRestrictionException {
        if (startCandidates.isEmpty() || endCandidates.isEmpty()) {
            // Indicates does-nothing restriction, like no right turn into the exit of a one-way street.
            throw new IndecipherableTurnRestrictionException("Empty candidate list?");
        }
        if (startCandidates.size()>1 && endCandidates.size()>1) {
            throw new IndecipherableTurnRestrictionException("Unresolvably ambiguous junction?");
        } else if (startCandidates.size()==1 && endCandidates.size()==1) {
            return new WrittenRoadSegmentPair(startCandidates.get(0), endCandidates.get(0));
        } else {
            double bestScoreSoFar = Double.POSITIVE_INFINITY;
            WrittenRoadSegmentPair bestPairSoFar = null;
            
            for (WrittenRoadSegment from : startCandidates) {
                for (WrittenRoadSegment to : endCandidates) {
                    double score = scoreMove(from, to, restrictionType);
                    if (score < bestScoreSoFar) {
                        bestScoreSoFar = score;
                        bestPairSoFar = new WrittenRoadSegmentPair(from, to);
                    }
                }
            }
            
            return bestPairSoFar;
        }
    }
    
    private double scoreMove(WrittenRoadSegment from, WrittenRoadSegment to, String restrictionType) {
        SimpleNode fromNode = nodesWithCounts.get(from.from);
        SimpleNode viaNode = nodesWithCounts.get(from.to);
        SimpleNode toNode = nodesWithCounts.get(to.to);
        
        double angleA = angleBetween(fromNode,viaNode);
        double angleB = angleBetween(viaNode, toNode);
        double delta = plusMinus180(angleB-angleA);
        
        double expectedAngle;
        if (restrictionType.contains("left_turn")) {
            expectedAngle=-90;
        } else if (restrictionType.contains("right_turn")) {
            expectedAngle=90;
        } else if (restrictionType.contains("u_turn")) {
            expectedAngle=180;
        } else {
            expectedAngle=0;
        }
        
        return plusMinus180(delta-expectedAngle);
    }
    
    private long getSharedNodeIdIfExactlyOne(OsmWay a, OsmWay b) throws IndecipherableTurnRestrictionException {
        Set<Long> shared = getSharedNodeIds(a.simpleWay.getNodeIds(), b.simpleWay.getNodeIds());
        if (shared.size() != 1) {
            throw new IndecipherableTurnRestrictionException("Ways disconnected, or not in connected order?");
        }
        return shared.iterator().next();
    }
    
    private Set<Long> getSharedNodeIds(List<Long> a, List<Long> b) {
        Set<Long> result = new HashSet();
        for (Long first : a) {
            if (b.contains(first)) {
                result.add(first);
            }
        }
        return result;
    }
    
    private class WrittenRoadSegmentPair {
        final WrittenRoadSegment from, to;
        public WrittenRoadSegmentPair(WrittenRoadSegment from, WrittenRoadSegment to) {
            this.from = from;
            this.to = to;
        }
    }
    
    private class OsmWay {
        long id;
        SimpleWay simpleWay;
        final List<WrittenRoadSegment> forwardRoadSegments = new ArrayList();
        final List<WrittenRoadSegment> backwardRoadSegments = new ArrayList();
        List<WrittenRoadSegment> allRoadSegments() {
            ArrayList<WrittenRoadSegment> result = new ArrayList(forwardRoadSegments);
            result.addAll(backwardRoadSegments);
            return result;
        }
    }
    
    private class WrittenRoadSegment {
        final long graphEdgeId, from, to, segmentLengthMm;
        final boolean isAccessOnly;

        public WrittenRoadSegment(long graphEdgeId, long from, long to, long segmentLengthMm, boolean isAccessOnly) {
            this.graphEdgeId = graphEdgeId;
            this.from = from;
            this.to = to;
            this.segmentLengthMm = segmentLengthMm;
            this.isAccessOnly = isAccessOnly;
        }
    }
    
    private class IndecipherableTurnRestrictionException extends Exception {
        IndecipherableTurnRestrictionException(String message) {
            super(message);
        }
    }
    
    private long haversineMillimeters(SimpleNode a, SimpleNode b) {
        double haversineMeters = haversine(a.getLat(), a.getLon(), 
                                           b.getLat(), b.getLon());
        return Math.round(haversineMeters * 1000.0);
    }
    
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = (lat2 - lat1);
        while (dLat > 180) {
            dLat -= 360;
        }
        while (dLat < -180) {
            dLat += 360;
        }

        double dLon = (lon2 - lon1);
        while (dLon > 180) {
            dLon -= 360;
        }
        while (dLon < -180) {
            dLon += 360;
        }

        dLat = (dLat * Math.PI / 180);
        dLon = ((lon2 - lon1) * Math.PI / 180);
        lat1 = (lat1 * Math.PI / 180);
        lat2 = (lat2 * Math.PI / 180);

        double a = (float) (Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2));
        double c = (float) (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));

        double R = 6371000;

        return R * c;
    }
    
    private double angleBetween(SimpleNode a, SimpleNode b) {
        double result = azimuth(a.getLat(), a.getLon(), b.getLat(), b.getLon());
        return plusMinus180(result);
    }
    
    private double azimuth(double lat1, double lon1, double lat2, double lon2) {
        // From http://www.movable-type.co.uk/scripts/latlong.html
        double φ1 = lat1*(Math.PI/180.0);
        double λ1 = lon1*(Math.PI/180.0);
        double φ2 = lat2*(Math.PI/180.0);
        double λ2 = lon2*(Math.PI/180.0);
        
        double y = Math.sin(λ2-λ1) * Math.cos(φ2);
        double x = Math.cos(φ1)*Math.sin(φ2) - Math.sin(φ1)*Math.cos(φ2)*Math.cos(λ2-λ1);
        return (180.0/Math.PI)*Math.atan2(y, x);
    }
    
    private double plusMinus180(double d) {
        while (d < -180) d += 360;
        while (d > 180) d -= 360;
        return d;
    }
    
    public static void main(String[] args) throws IOException {
        new ProcessPlanet().doIt();
    }

}
