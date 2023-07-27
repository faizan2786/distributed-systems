import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Level;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.jetbrains.annotations.NotNull;
import zk.classes.ZKConnection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceRegistry implements Watcher {
    private static final String ZNODE_ROOT = "/service_registry";
    private static final Logger logger = Logger.getLogger(ServiceRegistry.class);
    private ZooKeeper zooKeeper;

    private String currentZnode = ""; // name of the latest znode registered with the registry
                                      // (useful to keep internal record of all znode names in a multithreaded environment)
    private List<String> allServiceAddresses = null;

    public static void main(String[] args) throws InterruptedException {
        ServiceRegistry app = new ServiceRegistry();
        Thread.sleep(100);
        app.registerNode("127.0.0.1");
        Thread.sleep(100);
        app.registerNode("127.0.0.2");
        Thread.sleep(100);
        app.registerNode("127.0.0.3");
        Thread.sleep(100);
        System.out.println("Node addresses: " + app.getAllServiceAddresses());
        System.out.println();

        app.unRegisterNode();  // unregister last registered znode
        Thread.sleep(100);
        app.registerNode("127.0.0.4");
        Thread.sleep(100);
        System.out.println("Node addresses: " + app.getAllServiceAddresses());

        app.waitForZKThread();
    }

    public ServiceRegistry() {
        // initialise logger object
        PropertyConfigurator.configure(this.getClass().getResource("log4j.Registry.properties"));
        try {

            ZKConnection zkConn = new ZKConnection();
            this.zooKeeper = zkConn.getConnection();
            setupRootZNode();
            updateAddresses();

        } catch (IOException  e) {
            e.printStackTrace();
        } catch (InterruptedException | KeeperException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupRootZNode() throws InterruptedException, KeeperException {
        if (zooKeeper.exists(ZNODE_ROOT, false) == null) {
            zooKeeper.create(ZNODE_ROOT, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            logger.info("Root znode created for the service registry!");
        }
    }

    // method to register a worker node (i.e a server)'s host name (i.e. IP address)
    // with the service registry
    public void registerNode(@NotNull String hostName) {
        try {
            this.currentZnode = zooKeeper.create(ZNODE_ROOT + "/n_", hostName.getBytes(),
                                                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            logger.info("New znode created: " + currentZnode);
            System.out.println("Node registered successfully!");
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    // method to unregister a worker node from the service registry
    // (useful for "leaders" to unregister themselves from the list of worker nodes)
    public void unRegisterNode()
    {
        try {
            if (currentZnode != null && zooKeeper.exists(currentZnode, false) != null) {
                zooKeeper.delete(currentZnode, -1);
                logger.info("Znode " + currentZnode + " has been removed from the registry!");
                this.currentZnode = null;
            }
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }
    public synchronized List<String> getAllServiceAddresses() {
        return allServiceAddresses;
    }

    // update (and cache) list of addresses for all active registered services
    private synchronized void updateAddresses() throws InterruptedException, KeeperException {

        List<String> children = zooKeeper.getChildren(ZNODE_ROOT, this);

        // get address from each znode and store it in a list
        ArrayList<String> addresses = new ArrayList<>(children.size());
        String workerFullPath;
        for (String node: children) {
            workerFullPath = ZNODE_ROOT + "/" + node;

            // to handle race condition where a znode is deleted between getChildren() method and getData() method calls,
            // first check if it exists.
            Stat stats = zooKeeper.exists(workerFullPath, false);
            if (stats == null) {
                continue;
            }
            byte[] data  = zooKeeper.getData(workerFullPath, false, stats);
            String address = new String(data);
            addresses.add(address);
        }
        allServiceAddresses = Collections.unmodifiableList(addresses);
        logger.info("Service address book updated!");
    }

    // synchronise Zookeeper event thread
    private void waitForZKThread() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()) {
            case NodeChildrenChanged:
                try {
                    updateAddresses(); // call updateAddresses() method on any changes to the worker (children) nodes
                } catch (InterruptedException | KeeperException e) {
                    e.printStackTrace();
                }
        }
    }
}

