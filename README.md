Stock Exchange and Broker implemented with ActiveMQ as a Message Broker

## Prerequisites
- Maven
- Java 21
- ActiveMQ (installed, configured and running on port 8161)

## Usage (Build & Run)
### Clean and Install Dependencies (if needed)
`mvn clean install`

### Maven Compilation
`mvn package`

### Run JAR file for SimpleBroker
```java -jar broker/target/broker-1.0-SNAPSHOT.jar```

### Run JAR file for 1 Client 
```java -jar client/target/client-1.0-SNAPSHOT.jar```

### Run 3 Clients Simulation Script
- when three clients try to buy all the available stocks of AAPL
  concurrently:

- ```java -jar client/target/client-1.0-SNAPSHOT.jar auto Client1 AAPL 200 & java -jar client/target/client-1.0-SNAPSHOT.jar auto Client2 AAPL 200 & java -jar client/target/client-1.0-SNAPSHOT.jar auto Client3 AAPL 200 & wait```

## ActiveMQ (Message Broker) 

###  Optional: Containerized ActiveMQ Lifecycle using Podman
Prerequisites (Podman installed) https://podman.io/get-started
- `podman build -t activemq ActiveMQ/` (executed from the project root folder)
- `podman run -d --name activemq -p 61616:61616 -p 8161:8161 activemq`
- `podman restart activemq`
- `podman stop activemq`
- `podman start activemq` (for stopped container)

#### Clean-up
- `podman rm activemq`
- `podman rmi activemq:latest`

#### ActiveMQ WebConsole 
Available at http://localhost:8161/
- Default Username: `admin`
- Default Password: `admin`

#### Logs
- `podman logs -f activemq` (Follow logs in real-time)

###  Optional:  Using Make for more convenience by utilizing Podman commands

Use `make help` to see Available targets
