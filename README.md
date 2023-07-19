# Distributed-Systems

This repository contains individual projects (built using [Maven](https://maven.apache.org/index.html)) for practising distributed systems programming in Java.\
**Note**: If you want a quick tutorial for Maven, please checkout my [maven-tutorial](https://github.com/faizan2786/maven-tutorial) repo.

The repo is created to demonstrate various *Distributed Systems concepts* in action using **Java**. It borrows examples and tutorials from the highly recommended **Udemy course** [Distributed Systems & Cloud Computing with Java](https://www.udemy.com/course/distributed-systems-cloud-computing-with-java/). It uses state-of-the art open source tools and frameworks such as Zookeeper, Kafka, Hadoop etc.that are widely used in the industry. 

## Some Distributed system terminology:

- **Fault-detection** -> Ability of a distributed system to detect when a node in a cluster has crashed or unreachable.
- **Fault-tolerance** -> Ability of a system to detect faults **and** automatically adjust the cluster without affecting its functionality.
- **Highly-available** -> A system is highly available if there is **always** at-least a node running and serving the goal of the system.\
  **Note** that, it is not possible to have a distributed system that is 100% available, but systems with **Three, Four or Five nines uptime** (i.e. 99.9%, 99.99% or 99.999% available) are common.
- **Horizontally Scalable** -> A system that can grow (or shrink - in terms of number of nodes in the cluster) relatively easily in proportion to the load on the system.

## Repo Structure
The repo contains several **modules** (i.e. folders), each for a different distributed system technology.\
Each module contains **multiple** **projects** to demonstrate different distributed system concepts using that tool.

The ***repo-level*** `pom.xml` file includes all the individual modules in the repo and the ***module-level*** `pom.xml` file includes all the individual projects in that module.
