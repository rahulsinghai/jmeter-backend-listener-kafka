/*
 * Copyright 2019 Rahul Singhai.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.rahulsinghai.jmeter.backendlistener.kafka;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import io.github.rahulsinghai.jmeter.backendlistener.model.MetricsRow;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link org.apache.jmeter.visualizers.backend.Backend Backend} which produces Kafka messages.
 *
 * @author rahulsinghai
 * @since 20190624
 */
public class KafkaBackendClient extends AbstractBackendListenerClient {

  private static final Logger logger = LoggerFactory.getLogger(KafkaBackendClient.class);

  private static final String BUILD_NUMBER = "BuildNumber";

  /** Parameter for setting the Kafka topic name. */
  private static final String KAFKA_TOPIC = "kafka.topic";

  private static final String KAFKA_FIELDS = "kafka.fields";
  private static final String KAFKA_TIMESTAMP = "kafka.timestamp";
  private static final String KAFKA_SAMPLE_FILTER = "kafka.sample.filter";
  private static final String KAFKA_TEST_MODE = "kafka.test.mode";
  private static final String KAFKA_PARSE_REQ_HEADERS = "kafka.parse.all.req.headers";
  private static final String KAFKA_PARSE_RES_HEADERS = "kafka.parse.all.res.headers";

  /** Parameter for setting the Kafka security protocol; "true" or "false". */
  private static final String KAFKA_SSL_ENABLED = "kafka.ssl.enabled";

  /** The password of the private key in the key store file. This is optional for client. */
  private static final String KAFKA_SSL_KEY_PASSWORD = "kafka.ssl.key.password";

  /**
   * The location of the key store file (include path information). This is optional for client and
   * can be used for two-way authentication for client.
   */
  private static final String KAFKA_SSL_KEYSTORE_LOCATION = "kafka.ssl.keystore.location";

  /**
   * The store password for the Kafka SSL key store file. This is optional for client and only
   * needed if kafka.ssl.keystore.location is configured.
   */
  private static final String KAFKA_SSL_KEYSTORE_PASSWORD = "kafka.ssl.keystore.password";

  /**
   * Parameter for setting the Kafka ssl truststore file location(include path information); for
   * example, "client.truststore.jks".
   */
  private static final String KAFKA_SSL_TRUSTSTORE_LOCATION = "kafka.ssl.truststore.location";

  /**
   * The password for the Kafka SSL trust store file. If a password is not set access to the
   * truststore is still available, but integrity checking is disabled.
   */
  private static final String KAFKA_SSL_TRUSTSTORE_PASSWORD = "kafka.ssl.truststore.password";

  /** The list of protocols enabled for SSL connections. */
  private static final String KAFKA_SSL_ENABLED_PROTOCOLS = "kafka.ssl.enabled.protocols";

  /** The file format of the key store file. This is optional for client. */
  private static final String KAFKA_SSL_KEYSTORE_TYPE = "kafka.ssl.keystore.type";

  /**
   * The SSL protocol used to generate the SSLContext. Default setting is TLS, which is fine for
   * most cases. Allowed values in recent JVMs are TLS, TLSv1.1 and TLSv1.2. SSL, SSLv2 and SSLv3
   * may be supported in older JVMs, but their usage is discouraged due to known security
   * vulnerabilities.
   */
  private static final String KAFKA_SSL_PROTOCOL = "kafka.ssl.protocol";

  /**
   * The name of the security provider used for SSL connections. Default value is the default
   * security provider of the JVM.
   */
  private static final String KAFKA_SSL_PROVIDER = "kafka.ssl.provider";

  /** The file format of the trust store file. */
  private static final String KAFKA_SSL_TRUSTSTORE_TYPE = "kafka.ssl.truststore.type";

  /**
   * The number of acknowledgments the producer requires the leader to have received before
   * considering a request complete.
   *
   * <ul>
   *   <li><code>acks=0</code>
   *   <li><code>acks=1</code>
   *   <li><code>acks=all</code>
   * </ul>
   */
  private static final String KAFKA_ACKS_CONFIG = "kafka.acks";

  /**
   * A list of host/port pairs to use for establishing the initial connection to the Kafka cluster.
   * The client will make use of all servers irrespective of which servers are specified here for
   * bootstrapping—this list only impacts the initial hosts used to discover the full set of
   * servers. This list should be in the form host1:port1,host2:port2,.... Since these servers are
   * just used for the initial connection to discover the full cluster membership (which may change
   * dynamically), this list need not contain the full set of servers (you may want more than one,
   * though, in case a server is down).
   */
  private static final String KAFKA_BOOTSTRAP_SERVERS_CONFIG = "kafka.bootstrap.servers";

  /**
   * Optional compression type for all data generated by the producer. The default is none (i.e. no
   * compression). Valid values are <code>none</code>, <code>gzip</code>, <code>snappy</code>,
   * <code>lz4</code>, or <code>zstd</code>. Compression is of full batches of data, so the efficacy
   * of batching will also impact the compression ratio (more batching means better compression).
   */
  private static final String KAFKA_COMPRESSION_TYPE_CONFIG = "kafka.compression.type";

  /**
   * The producer will attempt to batch records together into fewer requests whenever multiple
   * records are being sent to the same partition. This helps performance on both the client and the
   * server. This configuration controls the default batch size in bytes.
   *
   * <p>No attempt will be made to batch records larger than this size.
   *
   * <p>Requests sent to brokers will contain multiple batches, one for each partition with data
   * available to be sent.
   *
   * <p>A small batch size will make batching less common and may reduce throughput (a batch size of
   * zero will disable batching entirely). A very large batch size may use memory a bit more
   * wastefully as we will always allocate a buffer of the specified batch size in anticipation of
   * additional records.
   */
  private static final String KAFKA_BATCH_SIZE_CONFIG = "kafka.batch.size";

  /**
   * An id string to pass to the server when making requests. The purpose of this is to be able to
   * track the source of requests beyond just ip/port by allowing a logical application name to be
   * included in server-side request logging.
   */
  private static final String KAFKA_CLIENT_ID_CONFIG = "kafka.client.id";

  /** Close idle connections after the number of milliseconds specified by this config. */
  private static final String KAFKA_CONNECTIONS_MAX_IDLE_MS_CONFIG =
      "kafka.connections.max.idle.ms";

  private static final Map<String, String> DEFAULT_ARGS = new LinkedHashMap<>();

  static {
    DEFAULT_ARGS.put(KAFKA_ACKS_CONFIG, "1");
    DEFAULT_ARGS.put(KAFKA_BOOTSTRAP_SERVERS_CONFIG, null);
    DEFAULT_ARGS.put(KAFKA_TOPIC, null);
    DEFAULT_ARGS.put(KAFKA_SAMPLE_FILTER, null);
    DEFAULT_ARGS.put(KAFKA_FIELDS, null);
    DEFAULT_ARGS.put(KAFKA_TEST_MODE, "info");
    DEFAULT_ARGS.put(KAFKA_PARSE_REQ_HEADERS, "false");
    DEFAULT_ARGS.put(KAFKA_PARSE_RES_HEADERS, "false");
    DEFAULT_ARGS.put(KAFKA_TIMESTAMP, "yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
    DEFAULT_ARGS.put(KAFKA_COMPRESSION_TYPE_CONFIG, null);
    DEFAULT_ARGS.put(KAFKA_SSL_ENABLED, "false");
    DEFAULT_ARGS.put(KAFKA_SSL_KEY_PASSWORD, null);
    DEFAULT_ARGS.put(KAFKA_SSL_KEYSTORE_LOCATION, null);
    DEFAULT_ARGS.put(KAFKA_SSL_KEYSTORE_PASSWORD, null);
    DEFAULT_ARGS.put(KAFKA_SSL_TRUSTSTORE_LOCATION, null);
    DEFAULT_ARGS.put(KAFKA_SSL_TRUSTSTORE_PASSWORD, null);
    DEFAULT_ARGS.put(KAFKA_SSL_ENABLED_PROTOCOLS, "TLSv1.2,TLSv1.1,TLSv1");
    DEFAULT_ARGS.put(KAFKA_SSL_KEYSTORE_TYPE, "JKS");
    DEFAULT_ARGS.put(KAFKA_SSL_PROTOCOL, "TLS");
    DEFAULT_ARGS.put(KAFKA_SSL_PROVIDER, null);
    DEFAULT_ARGS.put(KAFKA_SSL_TRUSTSTORE_TYPE, "JKS");
    DEFAULT_ARGS.put(KAFKA_BATCH_SIZE_CONFIG, Integer.toString(16384));
    DEFAULT_ARGS.put(KAFKA_CLIENT_ID_CONFIG, "JMeterKafkaBackendListener");
    DEFAULT_ARGS.put(KAFKA_CONNECTIONS_MAX_IDLE_MS_CONFIG, Long.toString(180000L));
  }

  private KafkaMetricPublisher publisher;
  private Set<String> modes;
  private Set<String> filters;
  private Set<String> fields;
  private int buildNumber;

  @Override
  public Arguments getDefaultParameters() {
    Arguments arguments = new Arguments();
    DEFAULT_ARGS.forEach(arguments::addArgument);
    return arguments;
  }

  @Override
  public void setupTest(BackendListenerContext context) throws Exception {
    this.filters = new HashSet<>();
    this.fields = new HashSet<>();
    this.modes = new HashSet<>(Arrays.asList("info", "debug", "error", "quiet"));
    this.buildNumber =
        (JMeterUtils.getProperty(KafkaBackendClient.BUILD_NUMBER) != null
                && !JMeterUtils.getProperty(KafkaBackendClient.BUILD_NUMBER).trim().equals(""))
            ? Integer.parseInt(JMeterUtils.getProperty(KafkaBackendClient.BUILD_NUMBER))
            : 0;

    Properties props = new Properties();
    props.put(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
        context.getParameter(KAFKA_BOOTSTRAP_SERVERS_CONFIG));
    props.put(ProducerConfig.CLIENT_ID_CONFIG, context.getParameter(KAFKA_CLIENT_ID_CONFIG));
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.ACKS_CONFIG, context.getParameter(KAFKA_ACKS_CONFIG));

    String compressionType = context.getParameter(KAFKA_COMPRESSION_TYPE_CONFIG);
    if (!Strings.isNullOrEmpty(compressionType)) {
      props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
    }

    // check if kafka security protocol is SSL or PLAINTEXT (default)
    if (context.getParameter(KAFKA_SSL_ENABLED).equals("true")) {
      logger.debug("Setting up SSL properties...");
      props.put(KAFKA_SSL_KEY_PASSWORD, context.getParameter(KAFKA_SSL_KEY_PASSWORD));
      props.put(KAFKA_SSL_KEYSTORE_LOCATION, context.getParameter(KAFKA_SSL_KEYSTORE_LOCATION));
      props.put(KAFKA_SSL_KEYSTORE_PASSWORD, context.getParameter(KAFKA_SSL_KEYSTORE_PASSWORD));
      props.put(KAFKA_SSL_TRUSTSTORE_LOCATION, context.getParameter(KAFKA_SSL_TRUSTSTORE_LOCATION));
      props.put(KAFKA_SSL_TRUSTSTORE_PASSWORD, context.getParameter(KAFKA_SSL_TRUSTSTORE_PASSWORD));
      props.put(KAFKA_SSL_ENABLED_PROTOCOLS, context.getParameter(KAFKA_SSL_ENABLED_PROTOCOLS));
      props.put(KAFKA_SSL_KEYSTORE_TYPE, context.getParameter(KAFKA_SSL_KEYSTORE_TYPE));
      props.put(KAFKA_SSL_PROTOCOL, context.getParameter(KAFKA_SSL_PROTOCOL));
      props.put(KAFKA_SSL_PROVIDER, context.getParameter(KAFKA_SSL_PROVIDER));
      props.put(KAFKA_SSL_TRUSTSTORE_TYPE, context.getParameter(KAFKA_SSL_TRUSTSTORE_TYPE));
    }
    props.put(
        ProducerConfig.BATCH_SIZE_CONFIG,
        Integer.parseInt(context.getParameter(KAFKA_BATCH_SIZE_CONFIG)));
    props.put(
        ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG,
        Long.parseLong(context.getParameter(KAFKA_CONNECTIONS_MAX_IDLE_MS_CONFIG)));

    convertParameterToSet(context, KAFKA_SAMPLE_FILTER, this.filters);
    convertParameterToSet(context, KAFKA_FIELDS, this.fields);

    KafkaProducer<Long, String> producer = new KafkaProducer<>(props);
    this.publisher = new KafkaMetricPublisher(producer, context.getParameter(KAFKA_TOPIC));

    checkTestMode(context.getParameter(KAFKA_TEST_MODE));
    super.setupTest(context);
  }

  /** Method that converts a semicolon separated list contained in a parameter into a string set */
  private void convertParameterToSet(
      BackendListenerContext context, String parameter, Set<String> set) {
    String[] array =
        (context.getParameter(parameter).contains(";"))
            ? context.getParameter(parameter).split(";")
            : new String[] {context.getParameter(parameter)};
    if (array.length > 0 && !array[0].trim().equals("")) {
      for (String entry : array) {
        set.add(entry.toLowerCase().trim());
        if (logger.isDebugEnabled()) {
          logger.debug("Parsed from " + parameter + ": " + entry.toLowerCase().trim());
        }
      }
    }
  }

  @Override
  public void handleSampleResults(List<SampleResult> results, BackendListenerContext context) {
    for (SampleResult sr : results) {
      MetricsRow row =
          new MetricsRow(
              sr,
              context.getParameter(KAFKA_TEST_MODE),
              context.getParameter(KAFKA_TIMESTAMP),
              this.buildNumber,
              context.getBooleanParameter(KAFKA_PARSE_REQ_HEADERS, false),
              context.getBooleanParameter(KAFKA_PARSE_RES_HEADERS, false),
              fields);

      if (validateSample(context, sr)) {
        try {
          // Prefix to skip from adding service specific parameters to the metrics row
          String servicePrefixName = "kafka.";
          this.publisher.addToList(new Gson().toJson(row.getRowAsMap(context, servicePrefixName)));
        } catch (Exception e) {
          logger.error(
              "The Kafka Backend Listener was unable to add sampler to the list of samplers to send... More info in JMeter's console.");
          e.printStackTrace();
        }
      }
    }

    try {
      this.publisher.publishMetrics();
    } catch (Exception e) {
      logger.error("Error occurred while publishing to Kafka topic.", e);
    } finally {
      this.publisher.clearList();
    }
  }

  @Override
  public void teardownTest(BackendListenerContext context) throws Exception {
    if (this.publisher.getListSize() > 0) {
      this.publisher.publishMetrics();
    }
    this.publisher.closeProducer();
    super.teardownTest(context);
  }

  /**
   * This method checks if the test mode is valid
   *
   * @param mode The test mode as String
   */
  private void checkTestMode(String mode) {
    if (!this.modes.contains(mode)) {
      logger.warn(
          "The parameter \"kafka.test.mode\" isn't set properly. Three modes are allowed: debug ,info, and quiet.");
      logger.warn(
          " -- \"debug\": sends request and response details to Kafka. Info only sends the details if the response has an error.");
      logger.warn(" -- \"info\": should be used in production");
      logger.warn(" -- \"error\": should be used if you.");
      logger.warn(" -- \"quiet\": should be used if you don't care to have the details.");
    }
  }

  /**
   * This method will validate the current sample to see if it is part of the filters or not.
   *
   * @param context The Backend Listener's context
   * @param sr The current SampleResult
   * @return true or false depending on whether or not the sample is valid
   */
  private boolean validateSample(BackendListenerContext context, SampleResult sr) {
    boolean valid = true;
    String sampleLabel = sr.getSampleLabel().toLowerCase().trim();

    if (this.filters.size() > 0) {
      for (String filter : filters) {
        Pattern pattern = Pattern.compile(filter);
        Matcher matcher = pattern.matcher(sampleLabel);

        if (sampleLabel.contains(filter) || matcher.find()) {
          valid = true;
          break;
        } else {
          valid = false;
        }
      }
    }

    // if sample is successful but test mode is "error" only
    if (sr.isSuccessful()
        && context.getParameter(KAFKA_TEST_MODE).trim().equalsIgnoreCase("error")
        && valid) {
      valid = false;
    }

    return valid;
  }
}
