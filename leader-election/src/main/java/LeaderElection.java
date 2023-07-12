import org.apache.zookeeper.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class LeaderElection implements Watcher {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000; // in millisecond

    private static final String ZNODE_ROOT = "/election";
    private ZooKeeper zookeeper; // zookeeper client object
    private String currentZnodeName;  // current znode's name

    // NOTE - Don't forget to create the /election ZNode using zkCli.sh before executing this method
    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        LeaderElection election = new LeaderElection();
        election.connectToZookeeper();
        election.createZNode();
        election.electLeader();
        election.waitForEventThread();
        System.out.println();
        System.out.println("Disconnected from zookeeper, exiting application!");
    }

    private void connectToZookeeper() throws IOException {
        this.zookeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
    }

    // wait for zookeeper event threads to finish
    private void waitForEventThread() throws InterruptedException
    {
        synchronized (zookeeper) {
            this.zookeeper.wait(); // main threads waits for the event thread to finish
        }
    }

    // This method creates a new znode with a new sequence id assigned by the ZK server.
    private void createZNode() throws InterruptedException, KeeperException {
        String znodePrefix = ZNODE_ROOT + "/c_"; // full path prefix for creating new modes ("c_" = child node)
        String znodeFullPath = zookeeper.create(znodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                                            CreateMode.EPHEMERAL_SEQUENTIAL); // creates a new temporary (ephemeral) znode using given prefix appending with a new sequence id

        System.out.println("znode full name: " + znodeFullPath);
        this.currentZnodeName = znodeFullPath.replace(ZNODE_ROOT + "/", ""); // extract only the name (without the full path)
    }

    // The newly created znode automatically nominates itself as a candidate.
    // If there is NOT any existing znode available with a smaller sequence,
    // then the created znode will be the leader.
    private void electLeader() throws InterruptedException, KeeperException {
        List<String> children = zookeeper.getChildren(ZNODE_ROOT, false);
        Collections.sort(children); // sort the nodes by their sequence (names)

        // now if the smallest child is same as the current znode then it is the leader
        String smallestNode = children.get(0);

        if (smallestNode.equals(currentZnodeName)) {
            System.out.println("I am the leader!");
        }
        else {
            System.out.println("I am NOT the leader.\nThe leader is: " + smallestNode);
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
