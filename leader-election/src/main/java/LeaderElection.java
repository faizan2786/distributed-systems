import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher;

import java.io.IOException;

public class LeaderElection implements Watcher {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000; // in millisecond
    private ZooKeeper zookeeper; // zookeeper client object

    public static void main(String[] args) throws IOException, InterruptedException {
        LeaderElection election = new LeaderElection();
        election.connectToZookeeper();
        election.waitForEventThread();
        System.out.println();
        System.out.println("Disconnected from zookeeper, exiting application!");
    }

    public void connectToZookeeper() throws IOException {
        this.zookeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
    }

    // wait for zookeeper event threads to finish
    public void waitForEventThread() throws InterruptedException
    {
        synchronized (zookeeper) {
            this.zookeeper.wait(); // main threads waits for the event thread to finish
        }
    }

    // implement event handler method 'process' of Watcher Interface (to handle zookeeper events)
    // Note: This is a callback which will be executed on a separate (Zookeeper Event)
    //       thread when a Zookeeper event occurs
    @Override
    public void process(WatchedEvent event) {
        switch(event.getType()) {
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
