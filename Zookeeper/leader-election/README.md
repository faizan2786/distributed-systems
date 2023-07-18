# Leader Election

This project demonstrate a simple "Leader Election" mechanism using zookeeper and concept of **sequential ephemeral znodes**.

## The Algorithm:
As mentioned perviously, Zookeeper provides a notion of ephemeral node, which is a temporary node in the Zookeeper data model that exists as long as the session that created the node is active. Now, if we choose to create a **sequential** node then Zookeeper will append a new monotonically increasing _sequence id_ to each newly created node. We can use this ability to achieve leader election in a zookeeper client cluster.\
**Note:** A *cluster* is simply a collection of multiple **running instances** of a client program connected to Zookeeper. It can be on a single machine or on separate machines.\
To keep the algorithm **simple**, we can choose a znode with the **least** (**minimum**) **sequence** **id** number to be our leader. Hence, the client responsible for creating such znode will be the **leader** of a cluster and rest of the clients will be its followers.

Hence, we can **summarise** our leader election psuedo code as follows:
- connect to zookeeper
- create a znode
- check for leader:
  - Get all children of the root znode.
  - sort the children by their sequence id.
  - If the current znode's number is same as the number of first child (after sorting) then current client is the **leader**.
  - If it is not a node with minimum number, then the client is **not** the leader.
- close the connection

## Project overview:
- `LeaderElection.java` -> The main Java class. It establishes connection to ZK server and handles ZK events.
   - The `createZNode()` method creates a new *znode* (a tree node in ZK server's in-memory data model).
 
  - The `electLeader()` method determines who among the running client instances is the current leader.

## How to test:
- Go to the current project directory and run `mvn package` command. 
  - This will create `leader-election-1.0-SNAPSHOT-jar-with-dependencies.jar` file with all the dependencies included in the file.
- In a terminal, start a Zookeeper server using `zkServer.sh start` command.
- Now, run `zkCli.sh` command to start the ZK client script.
- Create the root znode using `create /election` command in the terminal.
- In a separate terminal, run the `.jar` file from step-1 using the command `java -jar target/leader-election-1.0-SNAPSHOT-jar-with-dependencies.jar`
  - Upon running above command, our main program will create a znode within the root namespace with a prefix `c_` and the sequence id = `0000000`  (i.e. its name will be like `c_00000000`).
  - Since this would be the first znode in the root, the main program will be able to confirm that it is indeed a node with minimum sequence id and will establish itself as a leader.
  - You will see the message `"I am the leader!"` printed on the console.
- Now, you can bring up as many other terminals as you want and repeat above step. 
  - However, this time each new znode created by the client program will have a sequence id higher than the last one.
  - Hence, each subsequent client program will print the new znode name that it has created and will print the message `""I am NOT the leader. The leader is ...`.
- To stop the zookeeper server (and all its clients) type command `zkServer.sh stop` in a terminal.
  
