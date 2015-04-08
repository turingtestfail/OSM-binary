
package uk.me.mjt.osmpbf.planetproc;

import crosby.binary.Osmformat;
import crosby.binary.file.BlockInputStream;
import crosby.binary.file.BlockReaderAdapter;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;


public class NodeReader implements Iterable<SimpleNode> {
    private final File f;
    
    public NodeReader(String fileToRead) {
        f = new File(fileToRead);
    }
    
    public Iterator<SimpleNode> iterator() {
        return new NodeFileIterator(f);
    }
    
    private class NodeFileIterator implements Iterator<SimpleNode> {
        
        private final NodeFileReader nfr;
        private Iterator<SimpleNode> currentIterator = null;
        private final Thread readerThread;

        public NodeFileIterator(File f) {
            nfr = new NodeFileReader(f);
            readerThread = new Thread(nfr, "Node Reader thread for " + f);
            readerThread.start();
        }
        
        public boolean hasNext() {
            if (currentIterator != null && currentIterator.hasNext()) {
                return true;
            } else {
                try {
                    while (!nfr.isDone()) {
                        Collection<SimpleNode> nodeSet = nfr.getSimpleNodeQueue().poll(10, TimeUnit.MILLISECONDS);
                        if (nodeSet != null) {
                            currentIterator = nodeSet.iterator();
                            if (currentIterator.hasNext()) {
                                return true;
                            }
                        }
                    }
                    return false;
                } catch (InterruptedException e) {
                    return false;
                }
            }
        }

        public SimpleNode next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            } else {
                return currentIterator.next();
            }
        }

        public void remove() {
            throw new UnsupportedOperationException("This is read-only, sorry.");
        }
        
        @Override
        protected void finalize() throws Throwable {
            nfr.setConsumerGone();
            readerThread.interrupt();
            super.finalize();
        }
        
    }
    
    private class NodeFileReader extends SimplePbfParser implements Runnable {
        
        private final File f;
        private final int queueCapacity = 64;
        private final ArrayBlockingQueue<Collection<SimpleNode>> simpleNodeQueue = new ArrayBlockingQueue(queueCapacity);
        private volatile boolean fileCompleted = false;
        private volatile boolean consumerGone = false;
        private long nodesReadSoFar = 0;
        private long startTime = -1;

        public NodeFileReader(File f) {
            this.f = f;
        }
        
        public void run() {
            try {
                System.out.println("Reading nodes from " + f);
                InputStream input = new BufferedInputStream(new FileInputStream(f));
                new BlockInputStream(input, this).process();
                System.out.println("...file read completed!");
            } catch (ConsumerGoneRuntimeException e) {
                System.out.println("...Stopped reading file, as consumer gone.");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void parseDense(Osmformat.DenseNodes nodes) {
            if (startTime == -1) startTime = System.currentTimeMillis();
            long lastId = 0;
            long lastLat = 0;
            long lastLon = 0;
            
            ArrayList<SimpleNode> out = new ArrayList(nodes.getIdCount());
            for (int i = 0; i < nodes.getIdCount(); i++) {
                lastId += nodes.getId(i);
                lastLat += nodes.getLat(i);
                lastLon += nodes.getLon(i);
                int latMillionths = (int)Math.round(1000000*parseLat(lastLat));
                int lonMillionths = (int)Math.round(1000000*parseLon(lastLon));
                SimpleNode sn = new SimpleNode(lastId, latMillionths, lonMillionths);
                out.add(sn);
                
                nodesReadSoFar++;
                if (nodesReadSoFar % 5000000L == 0 || nodesReadSoFar <= 10) {
                    long procTime = System.currentTimeMillis()-startTime;
                    double nodesPerSecond = (1000.0 * nodesReadSoFar) / procTime;
                    System.out.println("Read " + nodesReadSoFar + " nodes in " + procTime + "ms, " + nodesPerSecond + " per second");
                }
            }
            offerNodes(out);
        }

        @Override
        protected void parseNodes(List<Osmformat.Node> nodes) {
            if (startTime == -1) startTime = System.currentTimeMillis();
            ArrayList<SimpleNode> out = new ArrayList(nodes.size());
            for (Osmformat.Node n : nodes) {
                int latMillionths = (int)Math.round(1000000*parseLat(n.getLat()));
                int lonMillionths = (int)Math.round(1000000*parseLon(n.getLon()));
                SimpleNode sn = new SimpleNode(n.getId(), latMillionths, lonMillionths);
                out.add(sn);
                
                nodesReadSoFar++;
                if (nodesReadSoFar % 5000000L == 0 || nodesReadSoFar <= 10) {
                    long procTime = System.currentTimeMillis()-startTime;
                    double nodesPerSecond = (1000.0 * nodesReadSoFar) / procTime;
                    System.out.println("Read " + nodesReadSoFar + " nodes in " + procTime + "ms, " + nodesPerSecond + " per second");
                }
            }
            offerNodes(out);
        }
        
        private void offerNodes(Collection<SimpleNode> out) {
            try {
                boolean putSuccessful = false;
                while (!putSuccessful) {
                    putSuccessful = simpleNodeQueue.offer(out, 1, TimeUnit.MINUTES);
                    if (!putSuccessful) {
                        System.gc(); // In case queue's stalled because our consumer is out of scope and needs GC to finalise it.
                    }
                }
                if (consumerGone) throw new ConsumerGoneRuntimeException();
                simpleNodeQueue.put(out);
            } catch (InterruptedException e) {
                if (consumerGone) {
                    throw new ConsumerGoneRuntimeException();
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
        
        public ArrayBlockingQueue<Collection<SimpleNode>> getSimpleNodeQueue() {
            return simpleNodeQueue;
        }
        
        @Override
        public void complete() {
            System.out.println("Read completed.");
            long procTime = System.currentTimeMillis()-startTime;
            double nodesPerSecond = (1000.0 * nodesReadSoFar) / procTime;
            System.out.println("Read " + nodesReadSoFar + " nodes in " + procTime + "ms, " + nodesPerSecond + " per second");
            fileCompleted = true;
        }
        
        public boolean isDone() {
            return (fileCompleted && simpleNodeQueue.isEmpty());
        }

        public void setConsumerGone() {
            this.consumerGone = true;
        }
        
    }
    
    private class ConsumerGoneRuntimeException extends RuntimeException {}

}
