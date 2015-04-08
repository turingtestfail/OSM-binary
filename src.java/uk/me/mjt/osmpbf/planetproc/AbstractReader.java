
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


public abstract class AbstractReader<E> implements Iterable<E> {
    private final File f;
    
    public AbstractReader(String fileToRead) {
        f = new File(fileToRead);
    }
    
    public abstract Iterator<E> iterator();
    
    abstract class ReaderIterator implements Iterator<E> {
        
        private final PbfReader nfr;
        private Iterator<E> currentIterator = null;
        private final Thread readerThread;

        public ReaderIterator() {
            nfr = makeReader(f);
            readerThread = new Thread(nfr, "Reader thread for " + f);
            readerThread.start();
        }
        
        abstract PbfReader makeReader(File f);
        
        public boolean hasNext() {
            if (currentIterator != null && currentIterator.hasNext()) {
                return true;
            } else {
                try {
                    while (!nfr.isDone()) {
                        Collection<E> nodeSet = nfr.getSimpleNodeQueue().poll(10, TimeUnit.MILLISECONDS);
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

        public E next() {
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
    
    protected abstract class PbfReader extends SimplePbfParser implements Runnable {
        
        private final File f;
        private final int queueCapacity = 64;
        private final ArrayBlockingQueue<Collection<E>> queue = new ArrayBlockingQueue(queueCapacity);
        private volatile boolean fileCompleted = false;
        private volatile boolean consumerGone = false;
        
        public PbfReader(File f) {
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

        protected void offer(Collection<E> out) {
            try {
                boolean putSuccessful = false;
                while (!putSuccessful) {
                    putSuccessful = queue.offer(out, 10, TimeUnit.SECONDS);
                    if (!putSuccessful) {
                        System.gc(); // In case queue's stalled because our consumer is out of scope and needs GC to finalise it.
                    }
                }
                if (consumerGone) throw new ConsumerGoneRuntimeException();
            } catch (InterruptedException e) {
                if (consumerGone) {
                    throw new ConsumerGoneRuntimeException();
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
        
        public ArrayBlockingQueue<Collection<E>> getSimpleNodeQueue() {
            return queue;
        }
        
        @Override
        public void complete() {
            System.out.println("Read completed.");
            fileCompleted = true;
        }
        
        public boolean isDone() {
            return (fileCompleted && queue.isEmpty());
        }

        public void setConsumerGone() {
            this.consumerGone = true;
        }
        
    }
    
    protected static class ConsumerGoneRuntimeException extends RuntimeException {}

}
