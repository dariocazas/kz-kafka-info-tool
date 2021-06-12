package com.dkzas.kafka.info.subcmd;


import com.dkzas.kafka.info.KzTool;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "partition_offset", description = "Get partition offsets from topic",
        mixinStandardHelpOptions = true, exitCodeListHeading = "Exit Codes:%n",
        exitCodeList = {CommandLine.ExitCode.OK + ": successful program execution",
                CommandLine.ExitCode.USAGE + ": incorrect parameters",
                CommandLine.ExitCode.SOFTWARE + ": if error"})
@Slf4j
@Data
public class PartitionOffsetSubcmd implements Callable<Integer> {

    @CommandLine.Option(names = {"-b", "--bootstrap-server"}, paramLabel = "BROKERS", required = true,
            description = "The Kafka server to connect to.")
    private String bootstrapServers;

    @CommandLine.Option(names = {"-c", "--command-config"}, paramLabel= "FILE", required = true,
            description = "Property file containing configs to be  passed to Admin Client. This is used only with --bootstrap-server option for describing and altering broker configs.The Kafka server to connect to.")
    private File commandConfig;

    @CommandLine.Option(names = {"--topic-list"}, paramLabel = "TOPICS", required = false,
            description = "The list of topics to be queried in the form \"topic1,topic2,topic3\". All topics will be queried if no topic list is specified ")
    private String topicList;

    @CommandLine.Option(names = {"--timeout"}, required = false, defaultValue = "10000",
            description = "Timeout in milliseconds in operations with Kafka")
    private int timeoutMs;

    public Integer call() throws Exception {
        try (KafkaConsumer<byte[], byte[]> consumer = initConsumerClient(kafkaProperties())) {
            var topicPartitions = listPartitions(consumer);
            consumer.assign(topicPartitions);
            for(TopicPartition tp : topicPartitions) {
                long offset = consumer.position(tp, getTimeoutDuration());
                System.out.println(String.join(",", tp.topic(), Integer.toString(tp.partition()), Long.toString(offset)));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return CommandLine.ExitCode.SOFTWARE;
        }
        return CommandLine.ExitCode.OK;
    }

    private Properties kafkaProperties() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream(commandConfig)) {
            properties.load(fileInputStream);
        }
        properties.remove("group.id");
        properties.put("bootstrap.servers", bootstrapServers);
        properties.computeIfAbsent("client.id", x -> "com.dkzas.kafka.info.KzTool");
        properties.computeIfAbsent("key.serializer", x -> "org.apache.kafka.common.serialization.ByteArraySerializer");
        properties.computeIfAbsent("value.serializer", x -> "org.apache.kafka.common.serialization.ByteArraySerializer");
        properties.computeIfAbsent("key.deserializer", x -> "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        properties.computeIfAbsent("value.deserializer", x -> "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        return properties;
    }

    private KafkaConsumer<byte[], byte[]> initConsumerClient(Properties props) {
        return new KafkaConsumer<>(props);
    }

    private Duration getTimeoutDuration() {
        return Duration.ofMillis(timeoutMs);
    }

    private List<TopicPartition> listPartitions(KafkaConsumer<?, ?> consumer) {
        Map<String, List<PartitionInfo>> topics = consumer.listTopics(getTimeoutDuration());
        if (topicList != null && !topicList.isEmpty()) {
            topics = topics.entrySet().stream()
                    .filter(x -> topicList.contains(x.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return topics.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(partition -> new TopicPartition(entry.getKey(), partition.partition())))
                .sorted(this::compare)
                .collect(Collectors.toList());
    }

    private int compare(TopicPartition a, TopicPartition b) {
        int result = a.topic().compareTo(b.topic());
        if (result == 0) {
            result = Integer.compare(a.partition(), b.partition());
        }
        return result;
    }
}
