package zk.classes;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import java.io.IOException;

/**
 * Helper class to maintain establish connection to the Zookeeper
 */
public class ZKConnection implements Watcher {

    private static final String ZK_ADDRESS = "localhost:2181";
    private static final int TIME_OUT = 3000; // server time out in milli secs
    private ZooKeeper zookeeper;

    public ZooKeeper getConnection() throws IOException {
        if (zookeeper == null) {
            zookeeper = new ZooKeeper(ZK_ADDRESS, TIME_OUT, this);
        }
        return zookeeper;
    }

    // process executes on a separate event thread
    @Override
    public void process(WatchedEvent event) {
        switch (event.getType()) {
            case None:
                if (event.getState() == Event.KeeperState.SyncConnected) {// event of successful connection to zookeeper server
                    System.out.println("-------------------------------------");
                    System.out.println("Successfully connected to Zookeeper!");
                    System.out.println("-------------------------------------");
                }
                else {
                    synchronized (zookeeper) {
                        System.out.println("-------------------------------------");
                        System.out.println("Disconnection from Zookeeper event!");
                        System.out.println("-------------------------------------");
                        zookeeper.notifyAll(); // notify all the waiting threads to wake up (i.e. main thread in this case)
                    }
                }
        }
    }
}