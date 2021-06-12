package com.dkzas.kafka.info.subcmd;


import com.dkzas.kafka.info.KzTool;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "partition_offset", description = "Get partition offsets from topic",
        mixinStandardHelpOptions = true, exitCodeListHeading = "Exit Codes:%n",
        exitCodeList = {CommandLine.ExitCode.OK + ": successful program execution",
                CommandLine.ExitCode.USAGE + ": incorrect parameters",
                CommandLine.ExitCode.SOFTWARE + ": if error"})
@Slf4j
public class PartitionOffsetSubcmd implements Callable<Integer> {

    @CommandLine.Option(names={"-b", "--bootstrap-server"}, required = true, description = "The Kafka server to connect to.")
    private String bootstrapServers;

    @CommandLine.Option(names={"-c", "--command-config"}, required = true, description = "Property file containing configs to be  passed to Admin Client. This is used only with --bootstrap-server option for describing and altering broker configs.The Kafka server to connect to.")
    private String commandConfig;

    @CommandLine.Option(names={"--topic-list"}, required = false, description = "The list of topics to be queried in the form \"topic1,topic2,topic3\". All topics will be queried if no topic list is specified ")
    private String topicList;

    public Integer call() throws Exception {
        return null;
    }
}
