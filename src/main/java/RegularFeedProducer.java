import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class RegularFeedProducer {

    private static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main(String[] args) throws InterruptedException {

        // docker-compose exec broker1 kafka-console-producer --bootstrap-server broker1:9091 --topic demo-perf-topic
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:29091");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        final String topic = "data-topic";
        Producer<String, String> producer = new KafkaProducer<>(props);
        String key = "test";

        // Send a message every 10 seconds indefinitely ...

        while (true) {
            Thread.sleep(10000);
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

