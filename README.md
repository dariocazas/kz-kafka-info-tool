	# kz-kafka-info-tool
	
	[![Java CI with Maven](https://github.com/dariocazas/kz-kafka-info-tool/actions/workflows/maven.yml/badge.svg)](https://github.com/dariocazas/kz-kafka-info-tool/actions/workflows/maven.yml)
	
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
	
	### Log level
	
	You can change the log level adding `-Dorg.slf4j.simpleLogger.defaultLogLevel=debug` to config slf4j
	
	### Subcommand `partition_offset`
	
	This command retrieve as standard output one line per partition with this info:
	```csv
	<topic-name:string>,<partition-number:int>,<last-offset:long>
	```
	
	You can check the parameter list using this command:
	
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
	
	### Subcommand `partition_size`
	
	Actually, the command only run using the output of `kafka-log-dirs.sh` script as imput.
	
	The standard output only prints the CVS results, using standard error for log.
	
	Support serveral parameters to group the size as:
	
	```shell
	$ kafka-log-dirs.sh --bootstrap-server $BROKERS --command-config $COMMAND_CONFIG --describe |  java -cp target/kz-kafka-info-tool-1.0.0-shaded.jar com.dkzas.kafka.info.App partition_size --from-stdin  --print-header
	topic_name,partition_index,broker,size
	__consumer_offsets,0,1,0
	__consumer_offsets,0,2,0
	__consumer_offsets,0,3,0
	__consumer_offsets,1,2,0
	__consumer_offsets,1,3,0
	__consumer_offsets,1,4,0
	__consumer_offsets,2,1,0
	__consumer_offsets,2,2,0
	__consumer_offsets,2,4,0
	__consumer_offsets,3,1,0
	__consumer_offsets,3,3,0
	__consumer_offsets,3,4,0
	__consumer_offsets,4,2,34988
	__consumer_offsets,4,3,34988
	__consumer_offsets,4,4,34988
	__consumer_offsets,5,1,0
	__consumer_offsets,5,2,0
	__consumer_offsets,5,4,0
	(...)
	```
	
	```shell
	$ kafka-log-dirs.sh --bootstrap-server $BROKERS --command-config $COMMAND_CONFIG --describe |  java -cp target/kz-kafka-info-tool-1.0.0-shaded.jar com.dkzas.kafka.info.App partition_size --from-stdin --group-by-topic --print-header
	topic_name,size
	__consumer_offsets,31999566
	__transaction_state,3834
	(...)
	```
	
	```shell
	$ kafka-log-dirs.sh --bootstrap-server $BROKERS --command-config $COMMAND_CONFIG --describe |  java -cp target/kz-kafka-info-tool-1.0.0-shaded.jar com.dkzas.kafka.info.App partition_size --from-stdin --group-by-topic --group-by-broker --print-header
	 topic_name,broker_1_size,broker_2_size,broker_3_size,broker_4_size
	__consumer_offsets,10593737,127078,10669942,10638590
	__transaction_state,852,1065,852,1065
	(...)
	```
	
	## Usage with maven
	
	```shell
	mvn verify exec:java -Dexec.arguments="partition_offset,-h"
	```
	