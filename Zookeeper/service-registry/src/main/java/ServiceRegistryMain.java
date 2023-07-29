import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import zk.classes.FaultTolerantCluster;
import zk.classes.OnElectionCallBack;
import zk.classes.ServiceRegistry;
import zk.classes.ZKConnection;

import javax.naming.InsufficientResourcesException;
import java.io.IOException;

public class ServiceRegistryMain {
    private static final int DEFAULT_PORT = 8081;

    private static final String CLUSTER_NAME = "registry_cluster";

    private ZooKeeper zooKeeper;


    // a port number can be passes as an optional argument
    public static void main(String[] args) throws InterruptedException, IOException, KeeperException {

        // get the port number (if provided)
        int portNum = args.length == 1 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        ServiceRegistryMain app = new ServiceRegistryMain();
        ZooKeeper zooKeeper = app.connectToZookeeper();

        // create a service registry object
        ServiceRegistry registry = new ServiceRegistry(zooKeeper, ServiceRegistryMain.class.getClassLoader());

        // instantiate an OnElectionAction object
        OnElectionAction electionAction = new OnElectionAction(registry, portNum);

        // create a cluster with leader election
        FaultTolerantCluster cluster = new FaultTolerantCluster(CLUSTER_NAME, zooKeeper, electionAction);

        // register a node
        cluster.addNewNode();

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
