import org.apache.zookeeper.ZooKeeper;
import zk.classes.FaultTolerantCluster;

import javax.naming.InsufficientResourcesException;
import java.io.IOException;
import org.apache.zookeeper.KeeperException;

public class FaultTolerantClusterMain {

    private static final String ZNODE_ROOT = "/election";
    private ZooKeeper zkConnection;

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException, InsufficientResourcesException {

        FaultTolerantClusterMain app = new FaultTolerantClusterMain();
        app.initCluster();
        app.waitForEventThread();
    }

    // Initialise a zookeeper cluster
    private void initCluster() throws IOException, InterruptedException, KeeperException, InsufficientResourcesException {
        FaultTolerantCluster cluster = new FaultTolerantCluster(ZNODE_ROOT);
        this.zkConnection = cluster.getConnection();
        cluster.addNewZNode();
    }
    private void waitForEventThread() throws InterruptedException {
        synchronized (zkConnection) {
            zkConnection.wait();
        }
    }
}
