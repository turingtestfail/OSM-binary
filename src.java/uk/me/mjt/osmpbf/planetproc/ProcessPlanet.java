
package uk.me.mjt.osmpbf.planetproc;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.TreeMap;


public class ProcessPlanet {
    public static final long UNCONTRACTED = Long.MAX_VALUE;
    public static final int OUTPUT_BUFFER_SIZE = 32 * 1024;
    
    //String filename = "/home/mtandy/Documents/contraction hierarchies/hertfordshire-latest.osm.pbf";
    //String filename ="/home/mtandy/Documents/contraction hierarchies/osm-pbf-files/planet-150309.osm.pbf";
    String filename ="/home/mtandy/Documents/contraction hierarchies/osm-pbf-files/great-britain-150409.osm.pbf";
    //String outFilePrefix = "/home/mtandy/Documents/contraction hierarchies/binary-test/hertfordshire";
    //String outFilePrefix = "/home/mtandy/Documents/contraction hierarchies/binary-test/planet";
    //String outFilePrefix = "/tmp/osm-bin-planet";
    String outFilePrefix = "/home/mtandy/Documents/contraction hierarchies/binary-test/great-britain";
    
    BigNodeStore nodesWithCounts = null;
    DataOutputStream waysOutput = null;
    DataOutputStream nodesOutput = null;
    long wayId = 1;
    
    long directedRoadSegmentCount = 0;
    long nodeCount = 0;
    
    public void doIt() throws IOException {
        nodesWithCounts = waysPerNode();
        
        waysOutput = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFilePrefix+"-ways.dat"),OUTPUT_BUFFER_SIZE));
        nodesOutput = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFilePrefix+"-nodes.dat"),OUTPUT_BUFFER_SIZE));
        extractRoadSegmentsAndNodes();
        waysOutput.close();
        nodesOutput.close();
        
        System.out.println("Directed road segments written: " + directedRoadSegmentCount);
        System.out.println("Nodes written: " + nodeCount);
    }
    
    private BigNodeStore waysPerNode() {
        NodeReader nr = new NodeReader(filename);
        
        BigNodeStore bns = new BigNodeStore(3450000000L);
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
                List<Long> nodeIds = w.getNodeIds();
                for (int i=0 ; i<nodeIds.size()-1 ; i++) {
                    boolean lastNode = (i==nodeIds.size()-2);
                    
                    SimpleNode from = nodesWithCounts.get(nodeIds.get(i));
                    SimpleNode to = nodesWithCounts.get(nodeIds.get(i+1));
                    
                    segmentLength += haversineMillimeters(from,to);
                    
                    if (firstNodeId == -1) {
                        firstNodeId = from.getId();
                        processNode(from);
                    }
                    
                    if (to.getWayCount() == 2 && !lastNode) {
                        // Just a waypoint - not at the start or end of a way.
                    } else {
                        if (w.isNavigableForwards())
                            processRoadSegment(firstNodeId, to.getId(), segmentLength);
                        if (w.isNavigableBackwards())
                            processRoadSegment(to.getId(), firstNodeId, segmentLength);
                        
                        processNode(to);
                        segmentLength=0;
                        firstNodeId=to.getId();
                    }
                    
                }
            }
        }
    }
    
    private void processRoadSegment(long from, long to, long segmentLengthMm) {
        try {
            directedRoadSegmentCount++;
            waysOutput.writeLong(wayId++);
            waysOutput.writeLong(from);
            waysOutput.writeLong(to);
            waysOutput.writeLong(segmentLengthMm);
            // Contraction details (spoiler: It's uncontracted)
            waysOutput.writeBoolean(false);
            waysOutput.writeLong(0);
            waysOutput.writeLong(0);
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
                nodesOutput.writeBoolean(false);
                nodesOutput.writeDouble(nodeInWay.getLat());
                nodesOutput.writeDouble(nodeInWay.getLon());
                
                nodeInWay.setWritten(true);
                nodesWithCounts.put(nodeInWay);
            }
            
        } catch (IOException e) {
            throw new RuntimeException(e);
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
    
    public static void main(String[] args) throws IOException {
        new ProcessPlanet().doIt();
    }

}
