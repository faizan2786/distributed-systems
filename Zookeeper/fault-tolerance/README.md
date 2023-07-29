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

- The `electLeaderWithFailOver()` method:
  - Until we are leader, or we found a valid predecessor:
    - Get all children
    - Sort the children by their sequence id
    - If minimum seq id = our seq. id then "we are the leader", `return`.
    - Else search the seq. id of predecessor - node at index 1 less than us in the list from step-1.
    - Subscribe to `exists()` event on this predecessor znode.
    

- On `NodeDeleted` event: 
  - Call `electLeaderWithFailOver()` method

## How to test:
- Go to the current project directory and run `mvn package` command. 
  - This will create `fault-tolerance-1.0-SNAPSHOT-jar-with-dependencies.jar` file with all the dependencies included in the file.
- In a terminal, start a Zookeeper server using `zkServer.sh start` command.
- Now, run `zkCli.sh` command to start the ZK client script.
- Create the root znode using `create /election` command in the terminal (if it isn't created already).
- In a separate terminal, run the `.jar` file from step-1 using the command `java -jar target/fault-tolerance-1.0-SNAPSHOT-jar-with-dependencies.jar`
  - Upon running above command, our main program will create a znode within the root namespace with a prefix `c_` and the sequence id = `0000000X`  (i.e. its name will be like `c_00000000X`).
  - Since this would be the first znode in the root, the main program will be able to confirm that it is indeed a node with minimum sequence id and will establish itself as a leader.
  - You will see the message `"I am the leader!"` printed on the console.
- Now, you can spin up another client by running the jar file in a separate terminal. 
  - However, this time new znode created by the client program will have a sequence id higher than the last one.
  - Hence, this client program will print the new znode name that it has just created and will print the message `""I am NOT the leader. The leader is ...`.
  - It will also find its predecessor and will subscribe to its `exists()` event. This will display the message: `"Watching Node: c_00...."` on the console.
- You can repeat above step a few more times to start multiple clients.
- Now **test the fault-tolerance feature by killing the leader client** terminal.
  - Within 3 secs (which is a set timeout period),  you will see the second client will be the leader and will display "I am the leader" message.
  - Since only one client was terminated, rest of the client terminals will be **unaffected**
- Now you can **kill one of the non-leader** terminal.
  - Again, within 3 secs you will see that the terminal that was watching the above killed client will update its predecessor and display the message `"Watching Node: c_00...."` with **new predecessor name**!

Hence, you can verify that our system is self-healing and very flexible.

  
