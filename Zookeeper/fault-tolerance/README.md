# Fault-Tolerance and Leader Re-election

This project demonstrates how to create an **efficient, fault-tolerant and scalable** distributed cluster using Zookeeper.\
It uses a simple Leader Election algorithm ( described in the `leader-election` project ) to elect clusters' leader and 
uses Zookeeper events and Watchers (described in the `event-watcher` ) to monitor re-elect the leader in case of a failure.

## The Algorithm

### General Idea:
- To detect a leader failure, all the followers can subscribe to ZK's `exists()` event on the current leader. 
This way, they will get notified when the leader's znode is deleted (i.e. leader node dies). 
- Upon receiving the trigger, all nodes can query ZK server for its children and the client with minimum sequence id can be the leader.
- All other clients can subscribe to this new leader and the process repeats.

**The Herd effect**: There is one issue with above approach which that all clients are subscribed to a same znode's event. 
Hence, when the event occurs, ZK server will have to send notifications to all the clients, and it will also be bombarded with subsequent query for new leader from all the followers. 
This can easily **overload** the server and may also cause it to crash in a large system.

### Efficient Approach

- Instead of having all clients query ZK for leader node, we can have **each follower** node watch its **predecessor** znode (i.e. a znode with one sequence id smaller than its own)
using `exists()` event.
  - If a predecessor fails, its watcher client will be notified and the client can query ZK server for its current children and decide if it's a new leader or not.
  - If it is NOT the leader then it will watch its available predecessor.
  - Repeat the process on each trigger. 

- This mechanism will have the following **advantages**:
  - Since each node is watching its predecessor, system will be able to detect failure **not only for the leader** but also **for any node** in the cluster.
  - Since each node is ONLY watching a single node, only a single client will be notified upon its failure. Hence, it will solve the "Herd effect".

Above mechanism provides an efficient fault-tolerant system which is also highly dynamic (scalable) where any number of nodes can join or drop from the cluster. 
As long as there is at least one node alive in the cluster, our system will be available.

Hence, we can **summarise** our failure detection and leader reelection process (in **pseudocode**) as follows:

- The `LeaderElectionAndFailover()` method:
  - Until we are leader or we found a valid predecessor:
    - Get all children
    - Sort the children by their sequence id
    - If minimum seq id = our seq. id then "we are the leader", `return`.
    - Else search the seq. id of predecessor - node at index 1 less than us in the list from step-1.
    - Subscribe to `exists()` event on this predecessor znode.
    

- On `NodeDeleted` event: 
  - Call `LeaderElectionAndFailover()` method

  
