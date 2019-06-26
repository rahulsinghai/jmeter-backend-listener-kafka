# jmeter-backend-listener-kafka

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/2574897d4d0646b4a2f2a34c0b86fc35)](https://app.codacy.com/app/rahulsinghai/jmeter-backend-listener-kafka?utm_source=github.com&utm_medium=referral&utm_content=rahulsinghai/jmeter-backend-listener-kafka&utm_campaign=Badge_Grade_Dashboard)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/2574897d4d0646b4a2f2a34c0b86fc35)](https://app.codacy.com/app/rahulsinghai/jmeter-backend-listener-kafka?utm_source=github.com&utm_medium=referral&utm_content=rahulsinghai/jmeter-backend-listener-kafka&utm_campaign=Badge_Grade_Dashboard)
[![Build Status](https://travis-ci.org/rahulsinghai/jmeter-backend-listener-kafka.svg?branch=master)](https://travis-ci.org/rahulsinghai/jmeter-backend-listener-kafka)

A JMeter plug-in that enables you to send test results to a Kafka server.

# Overview

### Description
JMeter Backend Listener Kafka is a JMeter plugin enabling you to send test results to a Kafka server.
It is meant as an alternative live-monitoring tool to the built-in "InfluxDB" backend listener of JMeter.
It is inspired from JMeter [ElasticSearch](https://github.com/delirius325/jmeter-elasticsearch-backend-listener) backend listener plug-in.

### Features

* Filters
  * Only send the samples you want, by using Filters! Simply type them as follows in the appropriate field: ``filter1;filter2;filter3`` or ``sampleLabel_must_contain_this``.
* Specific fields `field1;field2;field3`
  * Specify fields that you want to send to Kafka (possible fields below):
     * AllThreads
     * BodySize
     * Bytes
     * SentBytes
     * ConnectTime
     * ContentType
     * DataType
     * ErrorCount
     * GrpThreads
     * IdleTime
     * Latency
     * ResponseTime
     * SampleCount
     * SampleLabel
     * ThreadName
     * URL
     * ResponseCode
     * TestStartTime
     * SampleStartTime
     * SampleEndTime
     * Timestamp
     * InjectorHostname
* Verbose, semi-verbose, error only, and quiet mode:
  * __debug__ : Send request/response information of all samplers (headers, body, etc.)
  * __info__ : Sends all samplers to the Kafka server, but only sends the headers, body info for the failed samplers.
  * __quiet__ : Only sends the response time, bytes, and other metrics
  * __error__ : Only sends the failing samplers to the Kafka server (Along with their headers and body information).
* Use Logstash or any other tool to consume data from Kafka topic and then ingest it into a Database of your liking.

### Maven

```xml
<dependency>
  <groupId>io.github.rahulsinghai</groupId>
  <artifactId>jmeter.backendlistener.kafka</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Packaging and testing your newly added code

Execute below mvn command. Make sure JAVA_HOME is set properly

```bash
mvn package
```

Move the resulting JAR to your `JMETER_HOME/lib/ext`.

## Screenshots

### Configuration

![screnshot1](https://cdn-images-1.medium.com/max/2000/1*iVb7mIp2dPg7zE4Ph3PrGQ.png "Screenshot of configuration")

### Sample Grafana dashboard

![screnshot1](https://image.ibb.co/jW6LNx/Screen_Shot_2018_03_21_at_10_21_18_AM.png "Sample Grafana Dashboard")

### Sample Grafana dashboard

![screnshot1](https://image.ibb.co/jW6LNx/Screen_Shot_2018_03_21_at_10_21_18_AM.png "Sample Grafana Dashboard")

### For more info

For more information, here's a little [documentation](https://github.com/rahulsinghai/jmeter-backend-listener-kafka/wiki).

## Contributing

Feel free to contribute by branching and making pull requests, or simply by suggesting ideas through the "Issues" tab.

### Code Styling

Please find instructions [here](https://github.com/HPI-Information-Systems/Metanome/wiki/Installing-the-google-styleguide-settings-in-intellij-and-eclipse) on how to configure your IntelliJ or Eclipse to format the source code according to Google style.
Once configured in IntelliJ, format code as normal with `Ctrl + Alt + L`.

#### Further Changes

Adding the XML file alone and auto-formatting the whole document could replace imports with wildcard imports, which isn't always what we want.

- To stop this from happening, Go to `File` → `Settings` → `Editor` → `Code Style` → `Java` and select the `Imports` tab.
- Set `Class Count to use import with '*'` and `Names count to us static import with '*'` to a higher value; anything over `999` should be fine.

You can now reformat code throughout your project without imports being changed to Wildcard imports.
