# distributed-systems


This repository contains individual projects (built using [Maven](https://maven.apache.org/index.html)) for practising distributed systems programming in Java.\
**Note**: If you want a quick tutorial for Maven, please checkout my [maven-tutorial](https://github.com/faizan2786/maven-tutorial) repo.

The repo is created to demonstrate various *Distributed Systems concepts* in action using **Java**.
It uses state-of-the art open source tools and frameworks such as Zookeeper, Kafka, Hadoop etc.that are widely used in the industry. 

Each project in the repo is described briefly below:

- **zookeeper-client-test** -> A quick "Hello World" program to illustrate the use of ZooKeeper client Java API. It includes connection/disconnection to zookeeper server and zookeeper event handling.

- **leader-election** -> Demonstrates **Leader Election** algorithm in  a distributed systems using Zookeeper client cluster (by the use of **ephemeral znodes**).
  - `LeaderElection.java` -> The main Java class. It establishes connection to ZK server and handles ZK events.\
The `createZNode()` method creates a new *znode* (a tree node in ZK server's in-memory data model).
Thus, our running application will become a new "node" in the existing **ZK client cluster**.\
**Note:** A *cluster* is simply a collection of multiple **running instances** of our application.\
The `electLeader()` method determines who among the running client instances is the current leader.
