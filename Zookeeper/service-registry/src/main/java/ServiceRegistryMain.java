import org.apache.zookeeper.ZooKeeper;
import zk.classes.ServiceRegistry;
import zk.classes.ZKConnection;

import java.io.IOException;

public class ServiceRegistryMain {
    private ZooKeeper zooKeeper;

    public static void main(String[] args) throws InterruptedException, IOException {

        ServiceRegistryMain app = new ServiceRegistryMain();
        ZooKeeper zooKeeper = app.connectToZookeeper();

        // create service registry instance
        ServiceRegistry registry = new ServiceRegistry(zooKeeper, ServiceRegistryMain.class.getClassLoader());
        registry.registerNode("127.0.0.1");
        registry.registerNode("127.0.0.2");
        registry.registerNode("127.0.0.3");
        System.out.println("Node addresses: " + registry.getAllServiceAddresses());
        System.out.println();

        registry.unRegisterNode();  // unregister last registered znode
        registry.registerNode("127.0.0.4");
        System.out.println("Node addresses: " + registry.getAllServiceAddresses());

        // Instantiate cluster with leader election

        app.waitForZKThread();
    }

    private ZooKeeper connectToZookeeper() throws IOException {
        // make zookeeper connection
        ZKConnection zkConn = new ZKConnection();
        this.zooKeeper = zkConn.getConnection();
        return zooKeeper;
    }

    // synchronise Zookeeper event thread
    private void waitForZKThread() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }
}
