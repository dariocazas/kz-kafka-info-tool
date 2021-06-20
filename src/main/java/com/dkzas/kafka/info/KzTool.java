package com.dkzas.kafka.info;

import com.dkzas.kafka.info.subcmd.PartitionOffsetSubcmd;
import com.dkzas.kafka.info.subcmd.PartitionSizeParserSubcmd;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "kztool",
        description = "Info tool for Kafka",
        mixinStandardHelpOptions = true,
//        version = 1.0-SNAPSHOT,
        subcommands = {PartitionOffsetSubcmd.class, PartitionSizeParserSubcmd.class}
)
@Slf4j
public class KzTool implements Callable<Integer> {
    @Override
    public Integer call() {
        System.err.println("No arguments. Use -h for help");
        return CommandLine.ExitCode.USAGE;
    }
}
