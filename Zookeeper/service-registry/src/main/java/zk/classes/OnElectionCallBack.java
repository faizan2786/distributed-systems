package zk.classes;

// interface to define callbacks on election events
public interface OnElectionCallBack {
    void onLeader();
    void onFollower();
}
