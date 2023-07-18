import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class LeaderFailOver implements Watcher { // define a Zookeeper Watcher(event-handler) class
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000; // in milliseconds

    private static final String ZNODE_ROOT = "/election";
    private ZooKeeper zookeeper; // zookeeper client object
    private String currentZnodeName;  // current znode's name

    // NOTE - Don't forget to create the /election ZNode using zkCli.sh before executing this method
    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        LeaderFailOver election = new LeaderFailOver();
        election.connectToZookeeper();
        election.createZNode();
        election.electLeaderWithFailOver();
        election.waitForEventThread();
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

    // This method creates a new znode (i.e. a ZK client) with a new sequence id assigned by the ZK server.
    private void createZNode() throws InterruptedException, KeeperException {
        String znodePrefix = ZNODE_ROOT + "/c_"; // full path prefix for creating new modes ("c_" = child node)
        String znodeFullPath = zookeeper.create(znodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                                            CreateMode.EPHEMERAL_SEQUENTIAL);   // creates a new temporary (ephemeral) znode
                                                                                // using given prefix and appending it with a new sequence id

        System.out.println("znode full name: " + znodeFullPath);
        this.currentZnodeName = znodeFullPath.replace(ZNODE_ROOT + "/", ""); // extract only the name (without its path)
    }

    /**
    Failure detection:
        - Every non-leader client will repeatedly watch its predecessor i.e. a znode with one sequence id smaller,
        using ZK's exists() event trigger.
        - If the watched znode dies, ZK will notify the current client, and it will subscribe to watch the znode before
        - If there is no more znode with the smaller seq. id means that the current client is the smallest and
        hence, it will become the leader.
        - In such mechanism, any number of znode can join or disconnect from the client cluster.
        - As long as there is only even one client active, the cluster will stay alive.
        - Hence, we can say that such cluster is "horizontally (dynamically) scalable" and "fault-tolerant"
     */
    private void electLeaderWithFailOver() throws InterruptedException, KeeperException {
        Stat stats = null;
        String predecessorName = "";

        // keep repeating the loop until we are the leader or we find a valid znode to watch
        while (stats == null) {
            List<String> children = zookeeper.getChildren(ZNODE_ROOT, false);
            Collections.sort(children); // sort the nodes by their sequence (names)
            // now if the smallest child is same as the current znode then it is the leader
            String smallestNode = children.get(0);
            if (smallestNode.equals(currentZnodeName)) {
                System.out.println("I am the leader!");
                return;
            } else {
                System.out.println("I am NOT the leader.\nThe leader is: " + smallestNode);

                // find the previous (predecessor) znode
                int predecessorIndex = Collections.binarySearch(children, currentZnodeName) - 1;
                predecessorName = children.get(predecessorIndex);
                stats = zookeeper.exists(ZNODE_ROOT + "/" + predecessorName, this); // watch the predecessor
            }
        }

        System.out.println("Watching node: " + predecessorName);
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
            case NodeDeleted:
                try{
                    electLeaderWithFailOver();
                }catch (KeeperException ex) {
                }catch (InterruptedException ex) {
                }
                break;
        }
    }
}
