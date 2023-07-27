import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class RegularFeedProducer {

    /**
     * Setup steps
     *
     * docker-compose exec broker1 kafka-cluster cluster-id --bootstrap-server broker1:9091
     * docker-compose exec broker4 kafka-cluster cluster-id --bootstrap-server broker4:9094
     * docker cp ./link-config.properties broker1:/tmp
     * docker-compose exec broker1 kafka-cluster-links --bootstrap-server broker4:9094 --create --link ab-link --config-file /tmp/link-config.properties --cluster-id <id for first broker>
     * docker-compose exec broker1 kafka-cluster-links --list --bootstrap-server broker4:9094
     * docker exec -it broker1 /bin/bash -c 'kafka-topics --bootstrap-server broker1:9091 --topic demo-perf-topic --replication-factor 3 --partitions 6 --create --config min.insync.replicas=2'
     * docker-compose exec broker1 kafka-mirrors --create --mirror-topic demo-perf-topic --link ab-link --bootstrap-server broker4:9094
     *
     * Then run the application `gradle run`
     *
     * (then take out the second cluster)
     */

    private static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main(String[] args) throws InterruptedException {

        // docker-compose exec broker1 kafka-console-producer --bootstrap-server broker1:9091 --topic demo-perf-topic
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:29091");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        final String topic = "demo-perf-topic";
        Producer<String, String> producer = new KafkaProducer<>(props);

        // Send a message every 10 seconds indefinitely ...

        while (true) {
            Thread.sleep(10000);
            String key = "test-"+ ThreadLocalRandom.current().nextInt(0, 12 + 1);;
            String unixTimestamp = String.valueOf(Instant.now().getEpochSecond());

            producer.send(
                    new ProducerRecord<>(topic, key, unixTimestamp),
                    (event, ex) -> {
                        if (ex != null)
                            LOG.error(ex.getMessage()+" exception caught",ex);
                        else
                            LOG.info(String.format("Produced event to topic %s: key = %s | value = %s", topic, key, unixTimestamp));
                    });
            producer.flush();
        }

    }
}

