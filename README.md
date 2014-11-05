## Developer Cloud Service - PF-ASAR tutorial



### Getting Started 

To run this application, you will need a Developer Cloud Sandbox that can be requested from Terradue's [Portal](http://www.terradue.com/partners), provided user registration approval. 

A Developer Cloud Sandbox provides Earth Science data access services, and assistance tools for a user to implement, test and validate his application.
It runs in two different lifecycle modes: Sandbox mode and Cluster mode. 
Used in Sandbox mode (single virtual machine), it supports cluster simulation and user assistance functions in building the distributed application.
Used in Cluster mode (collections of virtual machines), it supports the deployment and execution of the application with the power of distributed computing processing over large datasets (leveraging the Hadoop Streaming MapReduce technology). 

### Installation 

Log on the developer sandbox and run these commands in a shell:

* Install **asarRT** and **csh**

```bash
sudo yum install -y asarRT csh
```

* Install this application

```bash
cd
git clone git@github.com:Terradue/dcs-pf-asar.git
cd dcs-pf-asar
mvn install -U -up
```

### Submitting the workflow

Run this command in a shell:

```bash
ciop-run
```

Or invoke the Web Processing Service via the Sandbox dashboard.

### Community and Documentation

To learn more and find information go to 

* [Developer Cloud Sandbox](http://docs.terradue.com/developer) service 

### Authors (alphabetically)

* Barchetta Francesco
* Brito Fabrice

### License

Copyright 2014 Terradue Srl

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
=======
