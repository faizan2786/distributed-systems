import org.apache.zookeeper.ZooKeeper;
import java.io.IOException;
import zk.classes.ZKConnection;

public class ZookeeperClient {

    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private ZooKeeper zooKeeper;

    public static void main(String[] args) throws InterruptedException, IOException {
        ZookeeperClient zkClient = new ZookeeperClient();
        zkClient.connectToZookeeper();
        zkClient.syncWithEventThread();

        // connection closed
        System.out.println("Disconnected from Zookeeper server, exiting application!");
    }

    private void connectToZookeeper() throws IOException
    {
        ZKConnection conn = new ZKConnection();
        zooKeeper = conn.getConnection();
    }

    // synchronise main thread with zk event thread
    private void syncWithEventThread() throws InterruptedException
    {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }
}
