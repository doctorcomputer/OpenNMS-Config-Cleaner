OpenNMS-Config-Cleaner
======================

This commandline tool checks your requisitions, foreign-sources, poller-configuration and collectd-configuration for problems.

how to use it?
start the jar like this:
java -jar -DConfigFolder=/your/opennms/config/folder -DOutPutFile=/the/result/file/Result.xls configcleaner.jar 

how to build it?
clone the repository
run:
mvn clean package
get the configcleaner-*-jar-with-dependencies.jar from the target foler and rename it to configcleaner.jar
use the configcleaner.jar as listed at "how to use"
