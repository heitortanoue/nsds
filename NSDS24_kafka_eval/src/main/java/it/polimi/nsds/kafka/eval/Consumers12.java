package it.polimi.nsds.kafka.eval;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

// Group number: 12
// Group members:
//  - Heitor Tanoue de Mello
//  - Enzo Serrano Conti
//  - Pietro Pizzoccheri

// Number of partitions for inputTopic (min, max): 1, N
// Number of partitions for outputTopic1 (min, max): 1, 1 (since key is always "sum")
// Number of partitions for outputTopic2 (min, max): 1, N

// Number of instances of Consumer1 (min, max):
// - Minimum: 1
// - Maximum: as many as you want, given that they dont share the same group
// the limitation on Consumer1 is that the instances cannot share the same partition,
// otherwise the exactly one semantics would be broken (since a producer could generate 10 values and die,
// and then the consumers from same group wouldn't' get 10 values so wouldn't process any window)

// Number of instances of Consumer2 (min, max):
// - Minimum: 1
// - Maximum: as many as you want, but if you have N instances in the same group for a topic of N-1 partitions, then this 1 excess instance will remain idle
// (considering no liveness nor delivery-semantics constraint on the original problem this should be fine)
//
// Definition of instances for Consumer1 and Consumer2
//
// Consumer1: every new instance of Consumer1 should be run and passed a different group ID
// Consumer2: new instances can be run with the same groups or different groups
// this should be run after TopicsManager, for topics to be properly set up;

public class Consumers12 {
    public static void main(String[] args) {
        String serverAddr = "localhost:9092";
        int consumerId = Integer.valueOf(args[0]);
        String groupId = args[1];
        if (consumerId == 1) {
            Consumer1 consumer = new Consumer1(serverAddr, groupId);
            consumer.execute();
        } else if (consumerId == 2) {
            Consumer2 consumer = new Consumer2(serverAddr, groupId);
            consumer.execute();
        }
    }

    private static class Consumer1 {
        private final String serverAddr;
        private final String consumerGroupId;

        private static final String inputTopic = "inputTopic";
        private static final String outputTopic = "outputTopic1";

        public Consumer1(String serverAddr, String consumerGroupId) {
            this.serverAddr = serverAddr;
            this.consumerGroupId = consumerGroupId;
        }

        public void execute() {
            // Consumer
            final Properties consumerProps = new Properties();
            consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
            consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);

            consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());

            consumerProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
            consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(false));

            KafkaConsumer<String, Integer> consumer = new KafkaConsumer<>(consumerProps);
            consumer.subscribe(Collections.singletonList(inputTopic));

            // Producer
            final Properties producerProps = new Properties();
            producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
            producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
            producerProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "consumer1_id");
            producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, String.valueOf(true));

            final KafkaProducer<String, Integer> producer = new KafkaProducer<>(producerProps);
            producer.initTransactions();

            // initilize variables count and sum Integer class to 0
            int sum = 0;
            int counter = 0;
            final String key = "sum";
            while (true) {
                final ConsumerRecords<String, Integer> records = consumer.poll(Duration.of(5, ChronoUnit.MINUTES));
                producer.beginTransaction();

                for (final ConsumerRecord<String, Integer> record : records) {
                    Integer value = record.value();
                    System.out.println(
                            "Value: " + value
                    );

                    sum += value;
                    counter++;

                    if (counter == 10) {
                        producer.send(new ProducerRecord<>(outputTopic, key, sum));
                        System.out.println("<" + outputTopic + " " + key + " " + sum + ">");

                        counter = 0;
                        sum = 0;
                    }
                }

                final Map<TopicPartition, OffsetAndMetadata> map = new HashMap<>();
                for (final TopicPartition partition : records.partitions()) {
                    final List<ConsumerRecord<String, Integer>> partitionRecords = records.records(partition);
                    final long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
                    map.put(partition, new OffsetAndMetadata(lastOffset + 1));
                }

                producer.sendOffsetsToTransaction(map, consumer.groupMetadata());
                producer.commitTransaction();
            }
        }
    }

    private static class Consumer2 {
        private final String serverAddr;
        private final String consumerGroupId;

        private static final String inputTopic = "inputTopic";
        private static final String outputTopic = "outputTopic2";

        public Consumer2(String serverAddr, String consumerGroupId) {
            this.serverAddr = serverAddr;
            this.consumerGroupId = consumerGroupId;
        }

        public void execute() {
            // Consumer
            final Properties consumerProps = new Properties();
            consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
            consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
            consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
            consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(true));

            KafkaConsumer<String, Integer> consumer = new KafkaConsumer<>(consumerProps);
            consumer.subscribe(Collections.singletonList(inputTopic));

            // Producer
            final Properties producerProps = new Properties();
            producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
            producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());

            final KafkaProducer<String, Integer> producer = new KafkaProducer<>(producerProps);

            Map<String, Integer> counters = new HashMap<>();
            Map<String, Integer> sums = new HashMap<>();

            while (true) {
                final ConsumerRecords<String, Integer> records = consumer.poll(Duration.of(5, ChronoUnit.MINUTES));
                for (final ConsumerRecord<String, Integer> record : records) {
                    int value = record.value();
                    String key = record.key();
//                    System.out.println(
//                            "Key: " + key +
//                            "\tValue: " + value
//                    );

                    counters.compute(key, (k, v) -> v == null ? 1 : v+1);
                    sums.compute(key, (k, v) -> v == null ? value : v+value);

                    //System.out.println("Counter: " + counters.get(key) + "\tSum: " + sums.get(key));

                    if (counters.get(key) == 10) {
                        producer.send(new ProducerRecord<>(outputTopic, key, sums.get(key)));
                        System.out.println("<" + outputTopic + " " + key + " " + sums.get(key) + ">");

                        counters.put(key, 0);
                        sums.put(key, 0);
                    }
                }
            }
        }
    }
}
