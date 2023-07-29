package zk.classes;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import javax.naming.InsufficientResourcesException;
import java.util.Collections;
import java.util.List;

// A utility class to manage a fault-tolerant cluster that
// also supports callbacks for service registry
public class FaultTolerantCluster implements Watcher { // define a Zookeeper Watcher(event-handler) class
    private String rootZNodePath;
    private ZooKeeper zookeeper; // zookeeper client object
    private String currentZnodeName;  // current znode's name

    private OnElectionCallBack electionCallback;

    // Initialise a zookeeper cluster with given Znode root
    public FaultTolerantCluster(String clusterRootName, ZooKeeper zooKeeper, OnElectionCallBack electionCallback) throws InterruptedException, KeeperException {
        this.zookeeper = zooKeeper;
        this.electionCallback = electionCallback;
        setRootZNode(clusterRootName);
    }

    // set root-level znode for the cluster
    private void setRootZNode(String rootName) throws InterruptedException, KeeperException {

        final String rootPath = rootName.startsWith("/") ? rootName : "/" + rootName; // set the root path

        // create a parchment root node if it doesn't exist already
        Stat stat = zookeeper.exists(rootPath, this);
        if (stat == null) {
            zookeeper.create(rootPath, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            System.out.println("Root ZNode created!");
        }
        System.out.println("Root path: " + rootPath);
        rootZNodePath = rootPath;
    }

    public String getRootZNodePath() {
        return rootZNodePath;
    }

    // This method creates a new znode under the root with a new sequence id assigned by the ZK server.
    // It also queries for the leader or a watcher node in the existing cluster after creating one
    // returns - newly created znode's name
    public String addNewNode() throws InterruptedException, KeeperException {

        String znodePrefix = rootZNodePath + "/c_"; // full path prefix for creating new modes ("c_" = child node)
        String znodeFullPath = zookeeper.create(znodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                                            CreateMode.EPHEMERAL_SEQUENTIAL);   // creates a new temporary (ephemeral) znode
                                                                                // using given prefix and appending it with a new sequence id

        System.out.println("znode's full path: " + znodeFullPath);
        this.currentZnodeName = znodeFullPath.replace(rootZNodePath + "/", ""); // extract only the name (without its path)

        electLeaderWithFailOver();
        return currentZnodeName;
    }

    private void electLeaderWithFailOver() throws InterruptedException, KeeperException {
        Stat stats = null;
        String predecessorName = "";

        // keep repeating the loop until we are the leader, or we find a valid znode to watch
        while (stats == null) {
            List<String> children = zookeeper.getChildren(rootZNodePath, false);
            Collections.sort(children); // sort the nodes by their sequence (names)
            // now if the smallest child is same as the current znode then it is the leader
            String smallestNode = children.get(0);
            if (smallestNode.equals(currentZnodeName)) {
                System.out.println("I am the leader!");
                electionCallback.onLeader();
                return;
            } else {
                System.out.println("I am NOT the leader.\nThe leader is: " + smallestNode);

                // find the previous (predecessor) znode
                int predecessorIndex = Collections.binarySearch(children, currentZnodeName) - 1;
                predecessorName = children.get(predecessorIndex);
                stats = zookeeper.exists(rootZNodePath + "/" + predecessorName, this); // watch the predecessor
            }
        }
        electionCallback.onFollower();
        System.out.println("Watching node: " + predecessorName);
        System.out.println();
    }

    // implement event handler method 'process' of Watcher Interface (to handle zookeeper events)
    // Note: This is a callback which will be executed on a separate (Zookeeper Event)
    //       thread when a Zookeeper event occurs
    @Override
    public void process(WatchedEvent event) {
        switch(event.getType()) {
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
