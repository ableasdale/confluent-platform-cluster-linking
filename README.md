# confluent-platform-cluster-linking
Demonstrating Cluster Linking Between two clusters running Confluent Platform 

```bash
docker-compose exec zookeeper zookeeper-shell localhost:2181
```

```
ls /
get /controller
{"version":1,"brokerid":2,"timestamp":"1673382716473"}
ls /brokers/ids
get /brokers/ids/1
{"features":{},"listener_security_protocol_map":{"BROKER":"PLAINTEXT","PLAINTEXT_HOST":"PLAINTEXT"},"endpoints":["BROKER://broker1:9091","PLAINTEXT_HOST://localhost:29091"],"jmx_port":-1,"port":9091,"host":"broker1","version":5,"tags":{},"timestamp":"1673382717473"}
```

Try to connect to Zk on the other cluster

```bash
docker-compose exec zookeeper2 zookeeper-shell localhost:2182
```

```
ls /
[admin, brokers, cluster, config, consumers, controller, controller_epoch, feature, isr_change_notification, latest_producer_id_block, leadership_priority, log_dir_event_notification, zookeeper]
get /controller
{"version":1,"brokerid":3,"timestamp":"1673382712259"}
ls /brokers/ids
[1, 2, 3]
get /brokers/ids/1
{"features":{},"listener_security_protocol_map":{"BROKER":"PLAINTEXT","PLAINTEXT_HOST":"PLAINTEXT"},"endpoints":["BROKER://broker4:9094","PLAINTEXT_HOST://localhost:29094"],"jmx_port":-1,"port":9094,"host":"broker4","version":5,"tags":{},"timestamp":"1673382714368"}
```

### Get the IDs for both clusters

```bash
docker-compose exec broker1 kafka-cluster cluster-id --bootstrap-server broker1:9091
```

You should see something like:
```
Cluster ID: YTAd13fGSziks7O0NRs2QA
```

```bash
docker-compose exec broker4 kafka-cluster cluster-id --bootstrap-server broker4:9094
```

You should see:

```
Cluster ID: 3vcAUrrvSCqPDykFsSIhfg
```

### Attempt to link the clusters

Create a file called `link-config.properties`:

```
bootstrap.servers=broker1:9091,broker2:9092,broker3:9093
sasl.mechanism=PLAIN
ssl.endpoint.identification.algorithm=http
```

Copy the file to broker1:

```bash
docker cp ./link-config.properties broker1:/tmp
```

Create the Cluster Link:

```bash
docker-compose exec broker1 kafka-cluster-links --bootstrap-server broker4:9094 --create --link ab-link --config-file /tmp/link-config.properties --cluster-id YTAd13fGSziks7O0NRs2QA
```

``` 
Cluster link 'ab-link' creation successfully completed.
```

IT WORKS! :)

### show it

```bash
docker-compose exec broker1 kafka-cluster-links --list --bootstrap-server broker4:9094 
```

you will see:

```
Link name: 'ab-link', link ID: 'XFdTSdXuTN6jULmLNrAuzQ', remote cluster ID: 'YTAd13fGSziks7O0NRs2QA', local cluster ID: '3vcAUrrvSCqPDykFsSIhfg', remote cluster available: 'true'
```

```bash
docker-compose exec broker1 kafka-configs --bootstrap-server broker4:9094 \
                  --describe \
                  --cluster-link ab-link
```

you will see:

```
Dynamic configs for cluster-link ab-link are:
  sasl.oauthbearer.token.endpoint.url=null sensitive=false synonyms={}
```

### Create Source and Destination topics and mirror them

Let's start by creating a topic on the source:

```bash
docker exec -it broker1 /bin/bash -c 'kafka-topics --bootstrap-server broker1:9091 --topic demo-perf-topic --replication-factor 3 --partitions 1 --create --config min.insync.replicas=2'
```

You should see:

```
Created topic demo-perf-topic.
```

Mirror it:

```bash
docker-compose exec broker1 kafka-mirrors --create --mirror-topic demo-perf-topic \
--link ab-link \
--bootstrap-server broker4:9094
```

You should see:

```
Created topic demo-perf-topic.
```

### Produce to the first cluster; Consume from the mirror on the second cluster

Start a console producer:

```bash
docker-compose exec broker1 kafka-console-producer --bootstrap-server broker1:9091 --topic demo-perf-topic
```

Start a consumer on the second cluster

Create a properties file `consumer.properties`:

```

```bash
docker cp ./consumer.properties broker4:/tmp
```

```bash
docker-compose exec broker4 kafka-console-consumer --bootstrap-server broker4:9094 --consumer.config /tmp/consumer.properties --from-beginning --topic demo-perf-topic
```

Confirm that any messages produced to cluster 1 can be read from the mirror topic.


----

cruft below --

echo ruok | nc zookeeper 2181

docker-compose exec zookeeper-dc1 zookeeper-shell localhost:2181 get /cluster/id | grep version | grep id | jq -r .id

confluent kafka link create my-link --cluster <destination id> \
    --source-cluster-id <source id> \
    --source-bootstrap-server <source bootstrap server> \
    --source-api-key <key> --source-api-secret <secret>

docker-compose exec broker1 kafka-cluster-links --bootstrap-server broker4:9094 \
--create \
--link my-link \
--cluster-id YTAd13fGSziks7O0NRs2QA

```bash
kafka-cluster-links --bootstrap-server localhost:9093 \
                       --create \
                       --link example-link \
                       --config-file example-link.config
```

kafka-console-consumer \
  --bootstrap-server <cluster id>.us-west-2.aws.confluent.cloud:9092 \
  --consumer.config audit-cluster.properties \
  --topic confluent-audit-log-events \
  --from-beginning 