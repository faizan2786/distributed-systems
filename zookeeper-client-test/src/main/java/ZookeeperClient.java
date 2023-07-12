import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher;
import java.io.IOException;

public class ZookeeperClient implements Watcher {

    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private ZooKeeper zooKeeper;

    public static void main(String[] args) throws IOException, InterruptedException
    {
        ZookeeperClient zkClient = new ZookeeperClient();
        zkClient.connectToZookeeper();
        zkClient.waitForEventThread();

        // connection closed
        System.out.println("Disconnected from Zookeeper server, exiting application!");
    }

    private void connectToZookeeper() throws IOException
    {
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
    }

    // wait for the event thread
    private void waitForEventThread() throws InterruptedException
    {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }

    // process executes on a separate event thread
    @Override
    public void process(WatchedEvent event) {
        switch(event.getType())
        {
            case None:
                if (event.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("-----------------------");
                    System.out.println("Successfully connected to Zookeeper");
                    System.out.println("-----------------------");
                }
                else {
                    synchronized (zooKeeper) {
                        System.out.println("-----------------------");
                        System.out.println("Disconnection event from Zookeeper");
                        System.out.println("-----------------------");
                        zooKeeper.notifyAll(); // notify all waiting threads (i.e. main thread)
                    }
                }
        }
    }
}
