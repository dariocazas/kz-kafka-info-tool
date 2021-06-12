package com.dkzas.kafka.info;

import picocli.CommandLine;

public class App {

    public static void main(String... args) {
        CommandLine cl = new CommandLine(new KzTool());
        cl.setCaseInsensitiveEnumValuesAllowed(true);
        System.exit(cl.execute(args));
    }
}
