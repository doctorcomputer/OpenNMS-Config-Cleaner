OpenNMS-Config-Cleaner
======================

This tool helps to analyze and validate configuration for the following OpenNMS components:
- Pollerd
- Collectd
- Provisiond
The output is represented as a matrix in a spreadsheet format like XLS or ODS. The configuration is just validated and give you the following information:
- All services configured for service assurance, datacollection and service detection
- Show configuration mismatches in poller-configuration, e.g. missing monitors entry or orphaned monitor definitions.
- Show which services are used for data collection
- Show which services have have a provisiond service detector
- Show which services are manually assigned throug provisioning

Given information help to understand the configuration for possible issues your requisitions, foreign-sources, poller-configuration and collectd-configuration.

Requirements:
=============
- Java development environment
- Maven for dependency management
- optional, but recommended: Git

Build
=====
1. Clone this repository from github
2. Build a runnable jar with all dependencies with `mvn clean package`
3. You find the runnable jar in the target folder

Usage
=====
start the jar like this:
`java -jar -DConfigFolder=/your/opennms/config/folder -DOutPutFile=/the/result/file/Result.xls configcleaner.jar`
