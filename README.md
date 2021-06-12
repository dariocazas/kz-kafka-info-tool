This tool provides basic kafka info

## Build

```shell
mvn verify
```

The output is:
- `target/kz-kafka-info-tool-<version>.jar` with the compiled code
- `target/kz-kafka-info-tool-<version>-shaded.jar` with compiled and dependencies in one jar

## Usage without maven

This tool use console as output, using std with CSV result, and err with log.
Check documentation and error codes with `-h` parameter.

```shell
❯ java -cp target/kz-kafka-info-tool-1.0.0-shaded.jar com.dkzas.kafka.info.App -h
Usage: kztool [-hV] [COMMAND]
Info tool for Kafka
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  partition_offset  Get partition offsets from topic
```

```sh
❯ java -cp target/kz-kafka-info-tool-1.0.0-shaded.jar com.dkzas.kafka.info.App partition_offset -h
Usage: kztool partition_offset [-hV] -b=BROKERS -c=FILE [--timeout=<timeoutMs>]
                               [--topic-list=TOPICS]
Get partition offsets from topic
  -b, --bootstrap-server=BROKERS
                            The Kafka server to connect to.
  -c, --command-config=FILE Property file containing configs to be  passed to
                              Admin Client. This is used only with
                              --bootstrap-server option for describing and
                              altering broker configs.The Kafka server to
                              connect to.
  -h, --help                Show this help message and exit.
      --timeout=<timeoutMs> Timeout in milliseconds in operations with Kafka
      --topic-list=TOPICS   The list of topics to be queried in the form
                              "topic1,topic2,topic3". All topics will be
                              queried if no topic list is specified
  -V, --version             Print version information and exit.
Exit Codes:
  0    successful program execution
  2    incorrect parameters
  1    if error
```

The parameters `--bootstrap-servers` and `--command-config` are the same at used by official Kafka scripts like 
`kafka-topics.sh` or `kafka-topics.sh`.

You can change the log level adding `-Dorg.slf4j.simpleLogger.defaultLogLevel=debug` to config slf4j

## Usage with maven

```shell
mvn verify exec:java -Dexec.arguments="partition_offset,-h"
```
