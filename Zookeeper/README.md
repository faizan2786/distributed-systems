# Apache ZooKeeper
Apache [ZooKeeper](https://zookeeper.apache.org) is an open-source **distributed service** for coordination, synchronisation and configuration management of distributed applications. It facilitates the synchronisation process among various nodes working together in a distributed cluster.

## Zookeeper data model

Zookeeper achieves this highly efficient synchronisation mechanism by the use efficient **in-memory data model**. It uses a tree-like data structure (i.e like a file system) to store its clients' information and meta-data.

Each client connecting to Zookeeper may create a *node* in this in-memory data model representation. This node is know as a **[ZNode](https://zookeeper.apache.org/doc/current/zookeeperOver.html)**. Unlike a file system, Zookeeper each file (i.e. a node) can have data and may also have children nodes. 

Zookeeper uses concept of  **ephemeral** znodes to make client  
synchronisation easier. These znodes exists as long as the client that created the znode is active in the cluster. When the client's session ends the znode is deleted. 

Clients can subscribe to specific **events** such as creation or deletion of znodes, modification to the data in znodes etc. to get notified by the ZK server.

## Module overview

Each project in the repo is described briefly below:

- `zookeeper-connect` -> A quick "Hello World" program to illustrate the use of ZooKeeper client Java API.
  - It defines `ZKConnection` **helper class** to handle connection/disconnection to ZooKeeper server. Other projects in this module may simply use this class to establish the connection to ZooKeeper.


- `leader-election` -> Demonstrates **Leader Election** algorithm in  a distributed systems using a Zookeeper client cluster (by the use of **ephemeral znodes**).


- `event-wtcher` -> This project illustrates how to use Zookeeper **Watchers** and **event triggers** to watch and get notified about changes in a specific znode's state.


- `fault-tolerance` -> Demonstrates how to build a **simple fault-tolerant and scalable** distributed cluster using ZK.

- `service-registry` -> This module implements a fully-functional **service registry application** using ZooKeeper. A **service registry** is a service that keeps record of addresses of all available worker nodes in the cluster.\
  It also demonstrates the application in action with a _fault-tolerant leader-follower system_ similar to the one in `fault-tolerance` project;.
