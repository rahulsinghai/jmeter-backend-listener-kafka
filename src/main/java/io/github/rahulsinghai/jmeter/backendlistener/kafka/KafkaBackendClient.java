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
  private static final String KAFKA_USE_SSL = "kafka.use.ssl";

  private static final String KAFKA_SSL_KEY_PASSWORD = "kafka.ssl.key.password";

  /**
   * Parameter for setting the Kafka ssl keystore (include path information); for example,
   * "server.keystore.jks".
   */
  private static final String KAFKA_SSL_KEYSTORE_LOCATION = "kafka.ssl.keystore.location";

  /** Parameter for setting the Kafka ssl keystore password. */
  private static final String KAFKA_SSL_KEYSTORE_PASSWORD = "kafka.ssl.keystore.password";

  /**
   * Parameter for setting the Kafka ssl truststore (include path information); for example,
   * "client.truststore.jks".
   */
  private static final String KAFKA_SSL_TRUSTSTORE_LOCATION = "kafka.ssl.truststore.location";

  /** Parameter for setting the Kafka ssl truststore password. */
  private static final String KAFKA_SSL_TRUSTSTORE_PASSWORD = "kafka.ssl.truststore.password";

  private static final String KAFKA_SSL_ENABLED_PROTOCOLS = "kafka.ssl.enabled.protocols";
  private static final String KAFKA_SSL_KEYSTORE_TYPE = "kafka.ssl.keystore.type";
  private static final String KAFKA_SSL_PROTOCOL = "kafka.ssl.protocol";
  private static final String KAFKA_SSL_PROVIDER = "kafka.ssl.provider";
  private static final String KAFKA_SSL_TRUSTSTORE_TYPE = "kafka.ssl.truststore.type";

  /** Parameter for setting encryption. It is optional. */
  private static final String KAFKA_COMPRESSION_TYPE = "kafka.compression.type";

  private static final Map<String, String> DEFAULT_ARGS = new LinkedHashMap<>();

  static {
    // Serializer class for key that implements the
    // <code>org.apache.kafka.common.serialization.Serializer</code> interface.
    DEFAULT_ARGS.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());

    // Serializer class for value that implements the
    // <code>org.apache.kafka.common.serialization.Serializer</code> interface.
    DEFAULT_ARGS.put(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

    // The number of acknowledgments the producer requires the leader to have received before
    // considering a request complete.
    // This controls the  durability of records that are sent.
    // The following settings are allowed:
    //
    // <ul> <li><code>acks=0</code>
    // If set to zero then the producer will not wait for any acknowledgment from the server at all.
    // The record will be immediately added to the socket buffer and considered sent.
    // No guarantee can be made that the server has received the record in this case, and the
    // <code>retries</code> configuration will not take effect (as the client won't generally know
    // of any failures).
    // The offset given back for each record will always be set to -1.
    //
    // <li><code>acks=1</code>
    // This will mean the leader will write the record to its local log but will respond without
    // awaiting full acknowledgement from all followers.
    // In this case should the leader fail immediately after acknowledging the record but before the
    // followers have replicated it then the record will be lost.
    //
    // <li><code>acks=all</code>
    // This means the leader will wait for the full set of in-sync replicas to acknowledge the
    // record.
    // This guarantees that the record will not be lost as long as at least one in-sync replica
    // remains alive.
    // This is the strongest available guarantee. This is equivalent to the acks=-1 setting.
    DEFAULT_ARGS.put(ProducerConfig.ACKS_CONFIG, "1");

    // A list of host/port pairs to use for establishing the initial connection to the Kafka
    // cluster.
    // The client will make use of all servers irrespective of which servers are specified here for
    // bootstrapping—this list only impacts the initial hosts used to discover the full set of
    // servers.
    // This list should be in the form host1:port1,host2:port2,....
    // Since these servers are just used for the initial connection to discover the full cluster
    // membership (which may change dynamically), this list need not contain the full set of servers
    // (you may want more than one, though, in case a server is down).
    DEFAULT_ARGS.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, null);

    // The total bytes of memory the producer can use to buffer records waiting to be sent to the
    // server.
    // If records are sent faster than they can be delivered to the server the producer will block
    // for <code>max.block.ms</code> after which it will throw an exception.
    // <p>This setting should correspond roughly to the total memory the producer will use, but is
    // not a hard bound since not all memory the producer uses is used for buffering.
    // Some additional memory will be used for compression (if compression is enabled) as well as
    // for maintaining in-flight requests.
    DEFAULT_ARGS.put(ProducerConfig.BUFFER_MEMORY_CONFIG, Long.toString(33554432));

    // The compression type for all data generated by the producer.
    // The default is none (i.e. no compression).
    // Valid  values are <code>none</code>, <code>gzip</code>, <code>snappy</code>,
    // <code>lz4</code>, or <code>zstd</code>.
    // Compression is of full batches of data, so the efficacy of batching will also impact the
    // compression ratio (more batching means better compression).
    DEFAULT_ARGS.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, null);

    // Setting a value greater than zero will cause the client to resend any record whose send fails
    // with a potentially transient error.
    // Note that this retry is no different than if the client resent the record upon receiving the
    // error.
    // Allowing retries without setting <code>max.in.flight.requests.per.connection</code> to 1 will
    // potentially change the ordering of records because if two batches are sent to a single
    // partition,
    // and the first fails and is retried but the second succeeds, then the records in the second
    // batch may appear first.
    // Note additionally that produce requests will be failed before the number of retries has been
    // exhausted if the timeout configured by <code>delivery.timeout.ms</code> expires first before
    // successful acknowledgement.
    // Users should generally prefer to leave this config unset and instead use
    // <code>delivery.timeout.ms</code> to control retry behavior.
    DEFAULT_ARGS.put(ProducerConfig.RETRIES_CONFIG, Integer.toString(2147483647));

    // The password of the private key in the key store file. This is optional for client.
    DEFAULT_ARGS.put(KAFKA_SSL_KEY_PASSWORD, null);

    // The location of the key store file. This is optional for client and can be used for two-way
    // authentication for client.
    DEFAULT_ARGS.put(KAFKA_SSL_KEYSTORE_LOCATION, null);

    // The store password for the key store file. This is optional for client and only needed if
    // ssl.keystore.location is configured.
    DEFAULT_ARGS.put(KAFKA_SSL_KEYSTORE_PASSWORD, null);

    // The location of the trust store file.
    DEFAULT_ARGS.put(KAFKA_SSL_TRUSTSTORE_LOCATION, null);

    // The password for the trust store file. If a password is not set access to the truststore is
    // still available, but integrity checking is disabled.
    DEFAULT_ARGS.put(KAFKA_SSL_TRUSTSTORE_PASSWORD, null);

    // The list of protocols enabled for SSL connections.
    DEFAULT_ARGS.put(KAFKA_SSL_ENABLED_PROTOCOLS, "TLSv1.2,TLSv1.1,TLSv1");

    // The file format of the key store file. This is optional for client.
    DEFAULT_ARGS.put(KAFKA_SSL_KEYSTORE_TYPE, "JKS");

    // The SSL protocol used to generate the SSLContext.
    // Default setting is TLS, which is fine for most cases.
    // Allowed values in recent JVMs are TLS, TLSv1.1 and TLSv1.2.
    // SSL, SSLv2 and SSLv3 may be supported in older JVMs, but their usage is discouraged due to
    // known security vulnerabilities.
    DEFAULT_ARGS.put(KAFKA_SSL_PROTOCOL, "TLS");

    // The name of the security provider used for SSL connections. Default value is the default
    // security provider of the JVM.
    DEFAULT_ARGS.put(KAFKA_SSL_PROVIDER, null);

    // The file format of the trust store file.
    DEFAULT_ARGS.put(KAFKA_SSL_TRUSTSTORE_TYPE, "JKS");

    // The producer will attempt to batch records together into fewer requests whenever multiple
    // records are being sent to the same partition.
    // This helps performance on both the client and the server.
    // This configuration controls the default batch size in bytes.
    // <p>No attempt will be made to batch records larger than this size.
    // <p>Requests sent to brokers will contain multiple batches, one for each partition with data
    // available to be sent.
    // <p>A small batch size will make batching less common and may reduce throughput (a batch size
    // of zero will disable batching entirely).
    // A very large batch size may use memory a bit more wastefully as we will always allocate a
    // buffer of the specified batch size in anticipation of additional records.
    DEFAULT_ARGS.put(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(16384));

    // An id string to pass to the server when making requests. The purpose of this is to be able to
    // track the source of requests beyond just ip/port by allowing a logical application name to be
    // included in server-side request logging.
    DEFAULT_ARGS.put(ProducerConfig.CLIENT_ID_CONFIG, "JMeterKafkaBackendListener");

    // Controls how the client uses DNS lookups.
    // If set to use_all_dns_ips then, when the lookup returns multiple IP addresses for a hostname,
    // they will all be attempted to connect to before failing the connection. Applies to both
    // bootstrap and advertised servers.
    // If the value is resolve_canonical_bootstrap_servers_only each entry will be resolved and
    // expanded into a list of canonical names.
    DEFAULT_ARGS.put(ProducerConfig.CLIENT_DNS_LOOKUP_CONFIG, "default");

    // Close idle connections after the number of milliseconds specified by this config.
    DEFAULT_ARGS.put(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, Long.toString(540000L));

    // An upper bound on the time to report success or failure after a call to <code>send()</code>
    // returns.
    // This limits the total time that a record will be delayed prior to sending, the time to await
    // acknowledgement from the broker (if expected),
    // and the time allowed for retriable send failures. The producer may report failure to send a
    // record earlier than this config if either an unrecoverable error is encountered,
    // the retries have been exhausted, or the record is added to a batch which reached an earlier
    // delivery expiration deadline.
    // The value of this config should be greater than or equal to the sum of
    // <code>request.timeout.ms</code> and <code>linger.ms</code>.
    DEFAULT_ARGS.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, Integer.toString(120000));

    // The producer groups together any records that arrive in between request transmissions into a
    // single batched request.
    // Normally this occurs only under load when records arrive faster than they can be sent out.
    // However in some circumstances the client may want to reduce the number of requests even under
    // moderate load.
    // This setting accomplishes this by adding a small amount of artificial delay&mdash;that is,
    // rather than immediately sending out a record the producer will wait for up to the given delay
    // to allow other records to be sent so that the sends can be batched together.
    // This can be thought of as analogous to Nagle's algorithm in TCP.
    // This setting gives the upper bound on the delay for batching: once we get
    // <code>batch.size</code> worth of records for a partition it will be sent immediately
    // regardless of this setting,
    // however if we have fewer than this many bytes accumulated for this partition we will 'linger'
    // for the specified time waiting for more records to show up.
    // This setting defaults to 0 (i.e. no delay).
    // Setting <code>linger.ms=5</code>, for example, would have the effect of reducing the number
    // of requests sent but would add up to 5ms of latency to records sent in the absence of load.
    DEFAULT_ARGS.put(ProducerConfig.LINGER_MS_CONFIG, "0");

    DEFAULT_ARGS.put(KAFKA_TOPIC, null);
    DEFAULT_ARGS.put(KAFKA_TIMESTAMP, "yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
    DEFAULT_ARGS.put(KAFKA_SAMPLE_FILTER, null);
    DEFAULT_ARGS.put(KAFKA_FIELDS, null);
    DEFAULT_ARGS.put(KAFKA_TEST_MODE, "info");
    DEFAULT_ARGS.put(KAFKA_PARSE_REQ_HEADERS, "false");
    DEFAULT_ARGS.put(KAFKA_PARSE_RES_HEADERS, "false");
    DEFAULT_ARGS.put(KAFKA_USE_SSL, "false");
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
        context.getParameter(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
    props.put(
        ProducerConfig.CLIENT_ID_CONFIG, context.getParameter(ProducerConfig.CLIENT_ID_CONFIG));
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.ACKS_CONFIG, context.getParameter(ProducerConfig.ACKS_CONFIG));
    props.put(
        ProducerConfig.BUFFER_MEMORY_CONFIG,
        Long.parseLong(context.getParameter(ProducerConfig.BUFFER_MEMORY_CONFIG)));

    String compressionType = context.getParameter(KAFKA_COMPRESSION_TYPE);
    if (!Strings.isNullOrEmpty(compressionType)) {
      props.put(
          ProducerConfig.COMPRESSION_TYPE_CONFIG,
          context.getParameter(ProducerConfig.COMPRESSION_TYPE_CONFIG));
    }

    props.put(
        ProducerConfig.RETRIES_CONFIG,
        Integer.parseInt(context.getParameter(ProducerConfig.RETRIES_CONFIG)));

    // check if kafka security protocol is SSL or PLAINTEXT (default)
    if (context.getParameter(KAFKA_USE_SSL).equals("true")) {
      logger.info("Setting up SSL properties...");
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
        Integer.parseInt(context.getParameter(ProducerConfig.BATCH_SIZE_CONFIG)));
    props.put(
        ProducerConfig.CLIENT_DNS_LOOKUP_CONFIG,
        context.getParameter(ProducerConfig.CLIENT_DNS_LOOKUP_CONFIG));
    props.put(
        ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG,
        Long.parseLong(context.getParameter(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG)));
    props.put(
        ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,
        Integer.parseInt(context.getParameter(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG)));
    props.put(
        ProducerConfig.LINGER_MS_CONFIG, context.getParameter(ProducerConfig.LINGER_MS_CONFIG));

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
      JSONMetric metric =
          new JSONMetric(
              sr,
              context.getParameter(KAFKA_TEST_MODE),
              context.getParameter(KAFKA_TIMESTAMP),
              this.buildNumber,
              context.getBooleanParameter(KAFKA_PARSE_REQ_HEADERS, false),
              context.getBooleanParameter(KAFKA_PARSE_RES_HEADERS, false),
              fields);

      if (validateSample(context, sr)) {
        try {
          this.publisher.addToList(new Gson().toJson(metric.getMetric(context)));
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
