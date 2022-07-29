FROM apache/spark:v3.2.2
ADD jars/* /opt/spark/jars
ADD target/scala-2.13/jobs.jar /opt/fluentlabs/