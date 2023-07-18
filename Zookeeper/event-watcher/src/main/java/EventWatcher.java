import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import java.io.IOException;
import java.util.List;

public class EventWatcher implements Watcher { // implement a Zookeeper Watcher(event-handler) class
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000; // in milliseconds

    private static final String TARGET_ZNODE = "/target_root";  // path of the znode to watch
    private ZooKeeper zookeeper; // zookeeper client object

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        EventWatcher failOver = new EventWatcher();
        failOver.connectToZookeeper();
        failOver.watchZnode();
        failOver.waitForEventThread();
        System.out.println();
        System.out.println("Disconnected from zookeeper, exiting application!");
    }

    private void connectToZookeeper() throws IOException {
        this.zookeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);   // pass the current class as the watcher
                                                                                            // to watch (serve callbacks) for ZK events
    }

    // wait for zookeeper event threads to finish
    private void waitForEventThread() throws InterruptedException
    {
        synchronized (zookeeper) {
            this.zookeeper.wait(); // main threads waits for the event thread to finish
        }
    }

    // method to create watchers (i.e. callbacks) for znode events...

    // ZooKeeper client/server model is based on event driven architecture.
    // Meaning, the clients subscribe to zookeeper events (via callbacks i.e. watchers) with the server and
    // the server notifies the clients when an event happens.

    // There are mainly three events to observe a znode:
    // exists() -> notifies if a znode is created or deleted
    // getData() -> notifies about change of data in a znode
    // getChildren() -> notifies about any change in a znode's children
    // Note: All these events are ONE TIME only events.
    //       Means, we will need to subscribe to the events again after they are triggered to get another notification.
    private void watchZnode() throws KeeperException, InterruptedException
    {
        Stat stats = this.zookeeper.exists(TARGET_ZNODE, this);
        if (stats == null) { // return if the target znode doesn't exist
            return;
        }

        byte[] data = zookeeper.getData(TARGET_ZNODE, this, stats);
        List<String> children = zookeeper.getChildren(TARGET_ZNODE, this);

        System.out.println("Data: " + new String(data));
        System.out.println("Children: " + children);
        System.out.println();
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
                break;
            case NodeCreated:
                System.out.println(TARGET_ZNODE + " created!");
                break;
            case NodeDeleted:
                System.out.println(TARGET_ZNODE + " deleted!");
                break;
            case NodeDataChanged:
                System.out.println(TARGET_ZNODE + " data changed!");
                break;
            case NodeChildrenChanged:
                System.out.println(TARGET_ZNODE + " children changed!");
                break;
        }
        try {
            watchZnode();  // subscribe the watchers after each ZK event
        } catch (KeeperException ex) {
        } catch (InterruptedException ex) {
        }
    }
}

