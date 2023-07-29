import org.apache.zookeeper.KeeperException;
import zk.classes.OnElectionCallBack;
import zk.classes.ServiceRegistry;

import java.net.InetAddress;
import java.net.UnknownHostException;

// an implementation for OnElectionCallBack interface
public class OnElectionAction implements OnElectionCallBack {

    private ServiceRegistry myRegistry;
    private int portNum;

    // needs:
    // registry object and
    // port number to register node's address
    OnElectionAction(ServiceRegistry registry, int portNum) {
        this.myRegistry = registry;
        this.portNum = portNum;
    }

    @Override
    public void onLeader() {
        try {
            myRegistry.unRegisterNode();
            myRegistry.updateAddresses();
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFollower() {
        try {
            String serverAddress = String.format("https://%s:%d", InetAddress.getLocalHost().getCanonicalHostName(), portNum);
            myRegistry.registerNode(serverAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
