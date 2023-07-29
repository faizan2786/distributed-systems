package zk.classes;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceRegistry implements Watcher {
    private static final String ZNODE_ROOT = "/service_registry";
    private static final Logger logger = Logger.getLogger(ServiceRegistry.class);
    private ZooKeeper zooKeeper;

    private String currentZnode; // name of the latest znode registered with the registry
                                      // (useful to keep internal record of a created znode name)
    private List<String> allServiceAddresses = null;

    // Service registry needs:
    // Zookeeper connection object and
    // Class loader of the client's class to locate the log4j.properties files for clients.
    public ServiceRegistry(ZooKeeper zooKeeper, ClassLoader classLoader) {

        // initialise logger object
        PropertyConfigurator.configure(classLoader.getResource("registry.log4j.properties"));

        this.zooKeeper = zooKeeper;
        try {
            setupRootZNode();
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
            if (this.currentZnode != null) {
                System.out.println("Already registered to service registry");
                return;
            }
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
                System.out.println("node unregistered from the registry!");
                this.currentZnode = null;
            }
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }
    public synchronized List<String> getAllServiceAddresses() {
        if (allServiceAddresses == null) {
            try {
                updateAddresses();
            } catch ( InterruptedException | KeeperException e) {
                e.printStackTrace();
            }
        }
        return allServiceAddresses;
    }

    // update (and cache) list of addresses for all active registered services
    // Note: while doing so, this method also registers for any change in root's children (i.e. calling getChildren() on root)
    public synchronized void updateAddresses() throws InterruptedException, KeeperException {

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
        System.out.println("service addresses are: " + allServiceAddresses);
        logger.info("");
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

