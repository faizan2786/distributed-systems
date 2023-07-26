import org.apache.zookeeper.*;
import zk.classes.ZKConnection;

import java.io.IOException;

public class ServiceRegistry {

    private static final String ZNODE_ROOT = "/service_registry";
    private ZooKeeper zooKeeper;

    public static void main(String[] args) throws IOException, InterruptedException {
        ServiceRegistry app = new ServiceRegistry();
    }

    public ServiceRegistry() throws IOException, InterruptedException {
        ZKConnection zkConn = new ZKConnection();
        this.zooKeeper = zkConn.getConnection();
        waitForZKThread();
    }

    // synchronise Zookeeper event thread
    private void waitForZKThread() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }

}


