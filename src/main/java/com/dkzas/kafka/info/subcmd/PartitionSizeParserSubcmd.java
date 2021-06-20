package com.dkzas.kafka.info.subcmd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

@CommandLine.Command(name = "partition_size", description = "Get partition size, with options to group",
        mixinStandardHelpOptions = true, exitCodeListHeading = "Exit Codes:%n",
        exitCodeList = {CommandLine.ExitCode.OK + ": successful program execution",
                CommandLine.ExitCode.USAGE + ": incorrect parameters",
                CommandLine.ExitCode.SOFTWARE + ": if error happends"})
@Slf4j
@Data
public class PartitionSizeParserSubcmd implements Callable<Integer> {

    private static final String OUTPUT_SEPARATOR = ",";

    @CommandLine.Option(names = {"--from-stdin"}, required = true,
            description = "Receive the output of kafka-log-dirs.sh from standard input.")
    private boolean fromStdIn;

    @CommandLine.Option(names = {"--group-by-topic"},
            description = "The size is accumulated per topic. This option can work with other group options")
    private boolean groupPerTopic;

    @CommandLine.Option(names = {"--group-by-broker"},
            description = "The size is accumulated per broker. This option can work with other group options")
    private boolean groupPerBroker;

    @CommandLine.Option(names = {"--print-header"},
            description = "First line at header info")
    private boolean printHeader;

    public Integer call() throws Exception {
        long startTime = System.currentTimeMillis();
        String jsonString = readInputToString(System.in);
        long startProcessingTime = System.currentTimeMillis();
        log.debug("Read json of {} size", jsonString.length());
        log.trace("{}", jsonString);
        List<PartitionSize> partitionSizes = parseJson(jsonString);
        log.info("Read {} partition info", partitionSizes.size());

        List<? extends Grouped> groupedData = group(partitionSizes, groupPerTopic, groupPerBroker);
        log.info("Data grouped in {} results", groupedData.size());
        if (printHeader && !groupedData.isEmpty()) {
            System.out.println(groupedData.get(0).header());
        }
        for (Grouped g : groupedData) {
            System.out.println(g.groupToString());
        }

        log.info("Read in {}ms, processed json in {}ms", System.currentTimeMillis() - startTime, System.currentTimeMillis() - startProcessingTime);
        return CommandLine.ExitCode.OK;
    }

    private String readInputToString(InputStream inputStream) throws IOException {
        try (InputStreamReader isr = new InputStreamReader(inputStream);
             BufferedReader reader = new BufferedReader(isr)) {
            return reader.lines().filter(x -> x.startsWith("{")).findFirst().get();
        }
    }

    private List<PartitionSize> parseJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonRoot = mapper.readTree(json);
        return parseJson(jsonRoot);
    }

    private List<PartitionSize> parseJson(JsonNode jsonRoot) {
        List<PartitionSize> partitionSizes = new ArrayList<>();
        if (!jsonRoot.has("brokers")) return partitionSizes;
        ArrayNode brokers = (ArrayNode) jsonRoot.get("brokers");
        for (int i = 0; i < brokers.size(); i++) {
            JsonNode broker = brokers.get(i);
            String brokerId = broker.get("broker").asText();
            log.trace("Broker: {}", brokerId);
            if (!broker.has("logDirs")) continue;
            ArrayNode logDirs = (ArrayNode) broker.get("logDirs");
            log.debug("Broker: {} has {} logDirs", brokerId, logDirs.size());
            for (int j = 0; j < logDirs.size(); j++) {
                log.trace("Broker: {}, logDirs: {}", brokerId, j);
                JsonNode logDir = logDirs.get(j);
                if (!logDir.has("partitions")) continue;
                ArrayNode partitions = (ArrayNode) logDir.get("partitions");
                log.debug("Broker: {}, logDirs: {} has {} partitions", brokerId, j, partitions.size());
                for (int k = 0; k < partitions.size(); k++) {
                    log.trace("Broker: {}, logDirs: {}, partition: {}", brokerId, j, k);
                    JsonNode partition = partitions.get(k);
                    String partitionName = partition.get("partition").asText();
                    Long partitionSize = partition.get("size").asLong();
                    partitionSizes.add(from(brokerId, partitionName, partitionSize));
                }
            }
        }
        return partitionSizes;
    }

    private PartitionSize from(String brokerId, String partitionName, Long partitionSize) {
        int split = partitionName.lastIndexOf('-');
        String topic = partitionName.substring(0, split);
        int partitionId = Integer.parseInt(partitionName.substring(split + 1, partitionName.length()));
        return PartitionSize.builder().topic(topic).partition(partitionId).broker(brokerId).size(partitionSize).build();
    }

    private List<? extends Grouped> group(List<PartitionSize> partitionSizes, boolean byTopic, boolean byBroker) {
        if (byTopic && byBroker)
            return groupByTopicAndBroker(partitionSizes);
        if (byBroker) {
            throw new UnsupportedOperationException("byBroker");
        }
        if (byTopic) return groupByTopic(partitionSizes);
        Collections.sort(partitionSizes);
        return partitionSizes;
    }

    private List<TopicBrokerSize> groupByTopicAndBroker(List<PartitionSize> partitionSizes) {
        List<String> brokers = partitionSizes.stream().map(PartitionSize::getBroker).distinct().collect(Collectors.toList());

        List<TopicBrokerSize> group = partitionSizes.stream()
                .map(x -> {
                    TopicBrokerSize ts = TopicBrokerSize.builder().topic(x.getTopic()).build();
                    ts.getBrokers().put(x.getBroker(), x.getSize());
                    return ts;
                }).collect(Collectors.toMap(TopicBrokerSize::getTopic, Function.identity(), (left, right) -> {
                    for (String key : right.getBrokers().keySet()) {
                        if (left.getBrokers().containsKey(key)) {
                            left.getBrokers().put(key, left.getBrokers().get(key) + right.getBrokers().get(key));
                        } else {
                            left.getBrokers().put(key, right.getBrokers().get(key));
                        }
                    }
                    return left;
                }))
                .values()
                .stream()
                .sorted()
                .collect(Collectors.toList());
        for (TopicBrokerSize ts : group) {
            for (String broker : brokers) {
                ts.brokers.computeIfAbsent(broker, x -> 0L);
            }
        }
        return group;
    }

    private List<TopicSize> groupByTopic(List<PartitionSize> partitionSizes) {
        return partitionSizes.stream()
                .map(x -> TopicSize.builder().topic(x.getTopic()).size(x.getSize()).build())
                .collect(Collectors.toMap(TopicSize::getTopic, Function.identity(), (left, right) -> {
                    left.size += right.getSize();
                    return left;
                }))
                .values()
                .stream()
                .sorted()
                .collect(Collectors.toList());
    }

    private interface Grouped extends Comparable<Grouped> {
        String groupToString();
        String header();
    }

    @Data
    @Builder
    private static class PartitionSize implements Grouped {
        String topic;
        int partition = -1;
        String broker;
        Long size;

        @Override
        public String groupToString() {
            StringBuilder sb = new StringBuilder();
            sb.append(topic).append(OUTPUT_SEPARATOR);
            sb.append(partition).append(OUTPUT_SEPARATOR);
            sb.append(broker).append(OUTPUT_SEPARATOR);
            sb.append(size);
            return sb.toString();
        }

        @Override
        public String header() {
            return String.join(OUTPUT_SEPARATOR, "topic_name", "partition_index", "broker", "size");
        }

        @Override
        public int compareTo(Grouped item) {
            PartitionSize o = (PartitionSize) item;
            int compare = topic.compareTo(o.topic);
            if (compare != 0) return compare;
            compare = Integer.compare(partition, o.partition);
            if (compare != 0) return compare;
            return broker.compareTo(o.broker);
        }
    }

    @Data
    @Builder
    private static class TopicSize implements Grouped {
        String topic;
        Long size;

        @Override
        public String groupToString() {
            StringBuilder sb = new StringBuilder();
            sb.append(topic);
            sb.append(OUTPUT_SEPARATOR);
            sb.append(size);
            return sb.toString();
        }

        @Override
        public String header() {
            return String.join(OUTPUT_SEPARATOR, "topic_name", "size");
        }

        @Override
        public int compareTo(Grouped item) {
            TopicSize o = (TopicSize) item;
            return topic.compareTo(o.topic);
        }
    }

    @Data
    @Builder
    private static class TopicBrokerSize implements Grouped {
        String topic;
        @Builder.Default
        Map<String, Long> brokers = new HashMap<>();

        @Override
        public String groupToString() {
            StringBuilder sb = new StringBuilder();
            sb.append(topic).append(OUTPUT_SEPARATOR);
            brokers.keySet().stream().sorted()
                    .forEach(broker -> sb.append(brokers.get(broker)).append(OUTPUT_SEPARATOR));
            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        }

        @Override
        public String header() {
            return "topic_name" + OUTPUT_SEPARATOR + String.join(OUTPUT_SEPARATOR, brokers.keySet().stream().sorted().map(x -> "broker_" + x + "_size").collect(Collectors.toList()));
        }


        @Override
        public int compareTo(Grouped item) {
            TopicBrokerSize o = (TopicBrokerSize) item;
            return topic.compareTo(o.topic);
        }
    }

}
