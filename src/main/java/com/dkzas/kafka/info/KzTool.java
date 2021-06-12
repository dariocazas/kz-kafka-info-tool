package com.dkzas.kafka.info;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "kztool",
        description = "Info tool for Kafka",
        mixinStandardHelpOptions = true,
//        version = 1.0-SNAPSHOT,
        subcommands = {}
)
@Slf4j
public class KzTool implements Callable<Integer> {

    @Getter
    public enum EXIT_CODES {
        SUCCESS(0),
        ERROR_ARGS(10),
        ERROR_KAFKA(20);

        private final int code;

        private EXIT_CODES(int code) {
            this.code = code;
        }
    };

    @Override
    public Integer call() {
        System.err.println("No arguments. Use -h for help");
        return EXIT_CODES.ERROR_ARGS.getCode();
    }
}
