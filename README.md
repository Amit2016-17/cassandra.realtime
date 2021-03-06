# Cassandra Realtime
Sample projects for using Cassandra with a realtime, event-based infrastructure.

## Cassandra Realtime Demo: Writing Apache Kafka™ Events into Apache Cassandra™
[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/Anant/cassandra.realtime)

Pro tip: To view README in preview mode from Gitpod, right click on the file and select `Open With > Preview`:
![Open README Preview](https://raw.githubusercontent.com/Anant/cassandra.realtime/master/screenshots/open-readme-preview.png )

# Setup Cassandra API
First we will setup a REST API for Cassandra. For this demo, the Flask version of Cassandra.api will work better.

## Open Cassandra API in Gitpod
https://gitpod.io/#https://github.com/Anant/cassandra.api

## Follow instructions in the Cassandra.api [README](https://github.com/Anant/cassandra.api/blob/master/README.md):
- You can use either NodeJS or Flask for the API, it is up to you. 
- We recommend naming your db table `leaves` in order to keep it simple when following this demo, but you can use a different tablename, as long as you change the tablename throughout the rest of the demo to use the same table.

## Make port 8000 for your REST API public 
Make sure your REST API's port 8000 is exposed, so that we can send requests to it later:
![Make port 8000 public](https://raw.githubusercontent.com/Anant/cassandra.realtime/master/screenshots/open-port-8000.png)

Note that if you don't use this gitpod workspace frequently enough, it will timeout and spin down. If this happens, you can just reopen the workspace and restart the server (using `npm start` for NodeJS or `python3 app.py` for Python).

## Get url for future reference
```
gp url 8000
```
![Get url for future reference](https://raw.githubusercontent.com/Anant/cassandra.realtime/master/screenshots/get-url-for-rest-api.png)

We will refer to this later, when we tell our Kafka Consumer where to send events.

# Open Cassandra Realtime in Gitpod

  [![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/Anant/cassandra.realtime)

## Setup Kafka
Make sure Kafka services are up by running `confluent local start`. Note that you don't need to start kafka connect yet (and indeed, it won't work until we set it up later on in this demo), but the others should be up. 

You can check with the confluent cli:
```
confluent local status
# if some are not up yet (running again doesn't hurt anything, so you can just run this either way):
confluent local start
```
![confluent local start](https://raw.githubusercontent.com/Anant/cassandra.realtime/master/screenshots/confluent-local-services-start.png)
- Note that the specific command you use in the Confluent CLI depends on the version of CLI you are using. Newer versions of the CLI will require you to use `confluent local services start`. In gitpod, we downloaded v.1.6.0 for you, so you can use the shorter syntax: `confluent local <cmd>`.

## Create a topic
If you are in gitpod, we set `$CONFLUENT_HOME` for you. It points to where your confluent binary directory is (`/home/gitpod/lib/confluent-5.5.1`). If you are not running this in gitpod, you will have to set `$CONFLUENT_HOME` yourself.
  ```
  $CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic record-cassandra-leaves-avro
  ```
![create a topic](https://raw.githubusercontent.com/Anant/cassandra.realtime/master/screenshots/create-kafka-topic.png)

Check that the topic exists
  ```
  $CONFLUENT_HOME/bin/kafka-topics --list --zookeeper localhost:2181
  ```
  
  ![check topic](https://raw.githubusercontent.com/Anant/cassandra.realtime/master/screenshots/check-kafka-topic.png)


#### Create the Kafka schema for topic's messages value

Make sure your python environment has `requests` and other modules installed. You can install it using our requirements.txt file:
```
cd $PROJECT_HOME
pip install -r python/requirements.txt
```

If you are in gitpod, we set `$PROJECT_HOME` for you. It is an absolute path to where this directory is inside this repo (`/workspace/cassandra.realtime`). If you are not running this in gitpod, you will have to set `$PROJECT_HOME` yourself.

![install requirements](https://raw.githubusercontent.com/Anant/cassandra.realtime/master/screenshots/install-requirements.txt.png)

Now create the schema
```
python ./kafka/create-schema.py http://localhost:8081 record-cassandra-leaves ./kafka/leaves-record-schema.avsc
```

![create schema](https://raw.githubusercontent.com/Anant/cassandra.realtime/master/screenshots/create-schema.png)

### Check that the schema exists
```
curl http://127.0.0.1:8081/subjects
# should return: ["record-cassandra-leaves-value"]
```
Alternatively you can check AKHQ. Run this to start AKHQ:

  ```
  java -Dmicronaut.config.files=$PROJECT_HOME/kafka/akhq/gitpod-akhq-config.yml -jar ${BINARY_DIR}/akhq.jar
  ```
You can see the AKHQ GUI at `http://127.0.0.1:8080/`. If you are using gitpod, we exposed 8080 for you by default. You can double check by clicking down here:

![view ports](https://raw.githubusercontent.com/Anant/cassandra.realtime/master/screenshots/open-ports-popup.png)

  * Pro tip: Use this single-line command to open a preview for port 8080 in gitpod:  
  
  ```
  gp preview $(gp url 8080)
  ```
  To see the AKHQ Schema registry view specifically:
  ```
  gp preview $(gp url 8080)/ui/docker-kafka-server/schema
  ```
  
  It should look something like this:

  ![schema registry](https://raw.githubusercontent.com/Anant/cassandra.realtime/master/screenshots/akhq-schema-registry.png)


# Import the data into Kafka
We are now ready to start sending messages to Kafka:

```
cd $PROJECT_HOME/python
pip install -r requirements.txt
python3 data_importer.py --config-file-path configs/gitpod-config.ini
```

![produce to Kafka](https://raw.githubusercontent.com/Anant/cassandra.realtime/master/screenshots/produce-to-kafka-stdout.png)


## Confirm that the message arrived in Kafka Topics

You can check the topic that has the schema using `kafka-avro-console-consumer`:
(WARNING: can potentially have lots of output)
```
$CONFLUENT_HOME/bin/kafka-avro-console-consumer --topic record-cassandra-leaves-avro --bootstrap-server localhost:9092 --from-beginning --property schema.registry.url=http://localhost:8081
```
# Consume from Kafka, write to Cassandra

## Execute the scala job to pick up messages from Kafka, deserialize and write them to Cassandra

First, edit the gitpod-project.properties file with the url of your running cassandra.api instance. 
- You will need to change the `api.host` key. It will look something like `api.host=https://8000-c0f5dade-a15f-4d23-b52b-468e334d6abb.ws-us02.gitpod.io`. Again you can find it by running the following command in the gitpod instance running cassandra.api: `gp url 8000`.
- Go ahead and change the `cassandra.keyspace` as well to whatever your keyspace is in Astra.
- Note that if you don't do this, the consumer will still run, but will just fail to write to Cassandra, since its current setting isn't stopping on errors.
```
cd $PROJECT_HOME/kafka-to-cassandra-worker/src/main/resources/
cp gitpod-project.properties.example gitpod-project.properties
vim gitpod-project.properties
#...
```

Now package and run the project:

```
cd $PROJECT_HOME
mvn -f ./kafka-to-cassandra-worker/pom.xml clean package
```

This will install dependencies and package your jar. If you make changes to your `gitpod-project.properties` file, make sure to run `mvn clean package again`, using `-f` flag to point to the pom.xml file. 

There should now be two jars in ./kafka-to-cassandra-worker/target, one with-dependencies, one without. We'll use the one with dependencies:
```
cd $PROJECT_HOME
mvn -f ./kafka-to-cassandra-worker/pom.xml exec:java -Dexec.mainClass="org.anant.KafkaAvroConsumer" -Dexec.args="kafka-to-cassandra-worker/target/classes/gitpod-project.properties"
```

- Note that if your Cassandra.api gitpod workspace timed out, you might need to reopen it and restart the REST API server.
- Offset is at `latest`, so you won't see anything unless you have messages actively coming in.
- Send more messages whenever you want to by re-running the python script from the python dir:
    ```
    cd $PROJECT_HOME/python
    python data_importer.py --config-file-path configs/gitpod-config.ini
    ```


You can confirm we are consuming the correct topic using AKHQ, at `/ui/docker-kafka-server/topic`. 
  
  ```
  gp preview $(gp url 8080)/ui/docker-kafka-server/topic
  ```
(If AKHQ was already on that page, make sure to refresh the view). You should see our consumer group (`send-to-cassandra-api-consumer`) listed as a consumer on topic `record-cassandra-leaves-avro`:

![Topics in AKHQ](https://raw.githubusercontent.com/Anant/cassandra.realtime/master/screenshots/akhq-topics.png)

# Sending messages to Kafka using Kafka REST Proxy

Check your topics 
```
curl http://localhost:8082/topics/
curl http://localhost:8082/topics/record-cassandra-leaves-avro
```

Send using data importer's rest proxy mode:
```
cd $PROJECT_HOME/python
python3 data_importer.py --config-file-path configs/gitpod-rest-proxy-config.ini
```

There should now be new messages for you to consume in your Kafka topic.

![Rest Proxy](https://raw.githubusercontent.com/Anant/cassandra.realtime/master/screenshots/rest-proxy.png)


# Process messages using Kafka Streams and writing to Cassandra using Processor API
You can use the Kafka processor API if you want to send messages to Cassandra using the REST API we are using.

```
cd $PROJECT_HOME
mvn  -f ./kafka-to-cassandra-worker/pom.xml exec:java -Dexec.mainClass="org.anant.KafkaStreamsAvroConsumer" -Dexec.args="kafka-to-cassandra-worker/target/classes/gitpod-project.properties"
```

Make sure to keep sending messages in another terminal or nothing will happen. You can use the same command as before:
```
cd $PROJECT_HOME/python
python3 data_importer.py --config-file-path configs/gitpod-rest-proxy-config.ini
```

# Writing to Cassandra using Kafka Connect
We used the Processor API to show what it would look like to write to Cassandra using Kafka Streams and a REST API, but it is generally recommended to use Kafka Connect. We will be using the [Datastax connector](https://www.confluent.io/hub/datastax/kafka-connect-cassandra-sink), but there is also a Confluence Cassandra connector as well as other third party connectors available if you are interested. 

## Setup Kafka Connect
The Datastax Kafka connector also has instructions and a download link from [the Datastax website](https://docs.datastax.com/en/kafka/doc/kafka/install/kafkaInstall.html) as well as [Confluent Hub](https://www.confluent.io/hub/datastax/kafka-connect-cassandra-sink).

### Create a connector properties file
We provide a `connect-standalone.properties.example` that is setup to run `kafka-connect-cassandra-sink-1.4.0.jar`. However, you will need to change:

  1) the name of the astra credentials zip file (cloud.secureConnectBundle). The path should be fine.
  2) Topic settings, particularly keyspace and tablename, unless tablename is already leaves, then only change keyspace (topic.record-cassandra-leaves-avro.<my_ks>.leaves.mapping)
  3) Astra db's password and username (auth.password)

Fields that require changing are marked by `### TODO make sure to change!` in the example file.

```
cd $PROJECT_HOME/kafka/connect
cp connect-standalone.properties.gitpod-example connect-standalone.properties
vim connect-standalone.properties
# ...
```

The worker properties file we provide (found at $PROJECT_HOME/kafka/connect/worker-properties/gitpod-avro-worker.properties) should work fine without modification in gitpod. However, if you are not using gitpod, you will need to change `/workspace/cassandra.realtime` in the plugin path if you are not using gitpod, to whatever your $PROJECT_HOME is. 

### Setup Connect with Astra
- If you have not already, make sure that your Datastax astra secure connect bundle is downloaded 
- Place the secure creds bundle into astra.credentials

  ```
  mv ./path/to/astra.credentials/secure-connect-<database-name-in-astra>.zip $PROJECT_HOME/kafka/connect/astra.credentials/
  ```
  In gitpod you can just drag and drop it into `$PROJECT_HOME/kafka/connect/astra.credentials/`.

## Start Kafka Connect

Start Kafka connect using your `connect-standalone.properties` file. First you will have to stop the service that the confluent cli started. Start it using:

```
confluent local stop connect
$CONFLUENT_HOME/bin/connect-standalone $PROJECT_HOME/kafka/connect/worker-properties/gitpod-avro-worker.properties $PROJECT_HOME/kafka/connect/connect-standalone.properties
```

![kafka connect logs](https://raw.githubusercontent.com/Anant/cassandra.realtime/master/screenshots/kafka-connect-logs-gp.png)


Don't forget to send some more messages in a separate terminal:
```
cd $PROJECT_HOME/python
python3 data_importer.py --config-file-path configs/gitpod-config.ini
```

If you're not sure if it's working or not, before sending messages to Kafka using the data_importer.py, in the astra console you can delete records previously created using:

```
TRUNCATE <your_ks>.leaves;
```

Then send messages, and run a count
```
SELECT COUNT(*) FROM <your_ks>.leaves;
```
![astra count](https://raw.githubusercontent.com/Anant/cassandra.realtime/master/screenshots/astra-count-all-leaves.png)

