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

### Attempt to link the clusters

Get the IDs for both clusters

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

```bash
kafka-cluster-links --bootstrap-server localhost:9093 \
                       --create \
                       --link example-link \
                       --config-file example-link.config
```











cruft below --
echo ruok | nc zookeeper 2181

docker-compose exec zookeeper-dc1 zookeeper-shell localhost:2181 get /cluster/id | grep version | grep id | jq -r .id
