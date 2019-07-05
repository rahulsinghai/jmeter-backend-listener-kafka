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

package io.github.rahulsinghai.jmeter.backendlistener.model;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricsRow {

  private static final Logger logger = LoggerFactory.getLogger(MetricsRow.class);
  private SampleResult sampleResult;
  private String kafkaTestMode;
  private String kafkaTimestamp;
  private int ciBuildNumber;
  private HashMap<String, Object> metricsMap;
  private Set<String> fields;
  private boolean allReqHeaders;
  private boolean allResHeaders;

  public MetricsRow(
      SampleResult sr,
      String testMode,
      String timeStamp,
      int buildNumber,
      boolean parseReqHeaders,
      boolean parseResHeaders,
      Set<String> fields) {
    this.sampleResult = sr;
    this.kafkaTestMode = testMode.trim();
    this.kafkaTimestamp = timeStamp.trim();
    this.ciBuildNumber = buildNumber;
    this.metricsMap = new HashMap<>();
    this.allReqHeaders = parseReqHeaders;
    this.allResHeaders = parseResHeaders;
    this.fields = fields;
  }

  /**
   * This method returns the current row as a Map(String, Object) for the provided sampleResult
   *
   * @param context BackendListenerContext
   * @param servicePrefixName Prefix string denoting the service name. This will allow to skip
   *     adding all service specific parameters to the metrics row.
   * @return A Map(String, Object) comprising all the metrics as key value objects
   * @throws UnknownHostException If unable to determine injector host name.
   */
  public Map<String, Object> getRowAsMap(BackendListenerContext context, String servicePrefixName)
      throws UnknownHostException {
    SimpleDateFormat sdf = new SimpleDateFormat(this.kafkaTimestamp);

    // add all the default SampleResult parameters
    addFilteredMetricToMetricsMap("AllThreads", this.sampleResult.getAllThreads());
    addFilteredMetricToMetricsMap("BodySize", this.sampleResult.getBodySizeAsLong());
    addFilteredMetricToMetricsMap("Bytes", this.sampleResult.getBytesAsLong());
    addFilteredMetricToMetricsMap("SentBytes", this.sampleResult.getSentBytes());
    addFilteredMetricToMetricsMap("ConnectTime", this.sampleResult.getConnectTime());
    addFilteredMetricToMetricsMap("ContentType", this.sampleResult.getContentType());
    addFilteredMetricToMetricsMap("DataType", this.sampleResult.getDataType());
    addFilteredMetricToMetricsMap("ErrorCount", this.sampleResult.getErrorCount());
    addFilteredMetricToMetricsMap("GrpThreads", this.sampleResult.getGroupThreads());
    addFilteredMetricToMetricsMap("IdleTime", this.sampleResult.getIdleTime());
    addFilteredMetricToMetricsMap("Latency", this.sampleResult.getLatency());
    addFilteredMetricToMetricsMap("ResponseTime", this.sampleResult.getTime());
    addFilteredMetricToMetricsMap("SampleCount", this.sampleResult.getSampleCount());
    addFilteredMetricToMetricsMap("SampleLabel", this.sampleResult.getSampleLabel());
    addFilteredMetricToMetricsMap("ThreadName", this.sampleResult.getThreadName());
    addFilteredMetricToMetricsMap("URL", this.sampleResult.getURL());
    addFilteredMetricToMetricsMap("ResponseCode", this.sampleResult.getResponseCode());
    addFilteredMetricToMetricsMap("TestStartTime", JMeterContextService.getTestStartTime());
    addFilteredMetricToMetricsMap(
        "SampleStartTime", sdf.format(new Date(this.sampleResult.getStartTime())));
    addFilteredMetricToMetricsMap(
        "SampleEndTime", sdf.format(new Date(this.sampleResult.getEndTime())));
    addFilteredMetricToMetricsMap(
        "Timestamp", sdf.format(new Date(this.sampleResult.getTimeStamp())));
    addFilteredMetricToMetricsMap("InjectorHostname", InetAddress.getLocalHost().getHostName());

    // Add the details according to the mode that is set
    switch (this.kafkaTestMode) {
      case "debug":
      case "error":
        addDetails();
        break;
      case "info":
        if (!this.sampleResult.isSuccessful()) {
          addDetails();
        }
        break;
      default:
        break;
    }

    addAssertions();
    addElapsedTime(sdf);
    addCustomFields(context, servicePrefixName);
    parseHeadersAsJsonProps(this.allReqHeaders, this.allResHeaders);

    return this.metricsMap;
  }

  /** This method adds all the assertions for the current sampleResult */
  private void addAssertions() {
    AssertionResult[] assertionResults = this.sampleResult.getAssertionResults();
    if (assertionResults != null) {
      @SuppressWarnings("unchecked")
      HashMap<String, Object>[] assertionArray = new HashMap[assertionResults.length];
      int i = 0;
      StringBuilder failureMessageStringBuilder = new StringBuilder();
      boolean isFailure = false;
      for (AssertionResult assertionResult : assertionResults) {
        HashMap<String, Object> assertionMap = new HashMap<>();
        boolean failure = assertionResult.isFailure() || assertionResult.isError();
        isFailure = isFailure || assertionResult.isFailure() || assertionResult.isError();
        assertionMap.put("failure", failure);
        assertionMap.put("failureMessage", assertionResult.getFailureMessage());
        failureMessageStringBuilder.append(assertionResult.getFailureMessage());
        failureMessageStringBuilder.append("\n");
        assertionMap.put("name", assertionResult.getName());
        assertionArray[i] = assertionMap;
        i++;
      }
      addFilteredMetricToMetricsMap("AssertionResults", assertionArray);
      addFilteredMetricToMetricsMap("FailureMessage", failureMessageStringBuilder.toString());
      addFilteredMetricToMetricsMap("Success", !isFailure);
    }
  }

  /**
   * This method adds the ElapsedTime as a key:value pair in the metricsMap object. Also, depending
   * on whether or not the tests were launched from a CI tool (i.e Jenkins), it will add a
   * hard-coded version of the ElapsedTime for results comparison purposes
   *
   * @param sdf SimpleDateFormat
   */
  private void addElapsedTime(SimpleDateFormat sdf) {
    Date elapsedTime;

    if (this.ciBuildNumber != 0) {
      elapsedTime = getElapsedTime(true);
      addFilteredMetricToMetricsMap("BuildNumber", this.ciBuildNumber);

      if (elapsedTime != null) {
        addFilteredMetricToMetricsMap("ElapsedTimeComparison", sdf.format(elapsedTime));
      }
    }

    elapsedTime = getElapsedTime(false);
    if (elapsedTime != null) {
      addFilteredMetricToMetricsMap("ElapsedTime", sdf.format(elapsedTime));
    }
  }

  /**
   * Methods that add all custom fields added by the user in the Backend Listener's GUI panel
   *
   * @param context BackendListenerContext
   */
  private void addCustomFields(BackendListenerContext context, String servicePrefixName) {
    Iterator<String> pluginParameters = context.getParameterNamesIterator();
    while (pluginParameters.hasNext()) {
      String parameterName = pluginParameters.next();

      if (!parameterName.startsWith(servicePrefixName)
          && !context.getParameter(parameterName).trim().equals("")) {
        String parameter = context.getParameter(parameterName).trim();

        try {
          addFilteredMetricToMetricsMap(parameterName, Long.parseLong(parameter));
        } catch (Exception e) {
          if (logger.isDebugEnabled()) {
            logger.debug("Cannot convert custom field to number");
          }
          addFilteredMetricToMetricsMap(parameterName, context.getParameter(parameterName).trim());
        }
      }
    }
  }

  /** Method that adds the request and response's body/headers */
  private void addDetails() {
    addFilteredMetricToMetricsMap("RequestHeaders", this.sampleResult.getRequestHeaders());
    addFilteredMetricToMetricsMap("RequestBody", this.sampleResult.getSamplerData());
    addFilteredMetricToMetricsMap("ResponseHeaders", this.sampleResult.getResponseHeaders());
    addFilteredMetricToMetricsMap("ResponseBody", this.sampleResult.getResponseDataAsString());
    addFilteredMetricToMetricsMap("ResponseMessage", this.sampleResult.getResponseMessage());
  }

  /**
   * This method will parse the headers and look for custom variables passed through as header. It
   * can also separate all headers into different Kafka document properties by passing "true". This
   * is a work-around the native behaviour of JMeter where variables are not accessible within the
   * backend listener.
   *
   * @param allReqHeaders boolean to determine if the user wants to separate ALL request headers
   *     into different JSON properties.
   * @param allResHeaders boolean to determine if the user wants to separate ALL response headers
   *     into different JSON properties.
   *     <p>NOTE: This will be fixed as soon as a patch comes in for JMeter to change the behaviour.
   */
  private void parseHeadersAsJsonProps(boolean allReqHeaders, boolean allResHeaders) {
    LinkedList<String[]> headersArrayList = new LinkedList<>();

    if (allReqHeaders) {
      headersArrayList.add(this.sampleResult.getRequestHeaders().split("\n"));
    }

    if (allResHeaders) {
      headersArrayList.add(this.sampleResult.getResponseHeaders().split("\n"));
    }

    for (String[] lines : headersArrayList) {
      for (String line : lines) {
        String[] header = line.split(":", 2);

        // if not all req headers and header contains special X-tag
        if (header.length > 1) {
          if (!this.allReqHeaders && header[0].startsWith("X-kafka-backend")) {
            this.metricsMap.put(header[0].replaceAll("kafka-", "").trim(), header[1].trim());
          } else {
            this.metricsMap.put(header[0].replaceAll("kafka-", "").trim(), header[1].trim());
          }
        }
      }
    }
  }

  /**
   * Adds a given key-value pair to metricsMap if the key is contained in the field filter or in
   * case of empty field filter
   */
  private void addFilteredMetricToMetricsMap(String key, Object value) {
    if (this.fields.size() == 0 || this.fields.contains(key.toLowerCase())) {
      this.metricsMap.put(key, value);
    }
  }

  /**
   * This method is meant to return the elapsed time in a human readable format. The purpose of this
   * is mostly for build comparison in Kibana. By doing this, the user is able to set the X-axis of
   * his graph to this date and split the series by build numbers. It allows him to overlap test
   * results and see if there is regression or not.
   *
   * @param forBuildComparison boolean to determine if there is CI (continuous integration) or not
   * @return The elapsed time in YYYY-MM-dd HH:mm:ss format
   */
  private Date getElapsedTime(boolean forBuildComparison) {
    String sElapsed;
    // Calculate the elapsed time (Starting from midnight on a random day - enables us to compare of
    // two loads over their duration)
    long start = JMeterContextService.getTestStartTime();
    long end = System.currentTimeMillis();
    long elapsed = (end - start);
    long minutes = (elapsed / 1000) / 60;
    long seconds = (elapsed / 1000) % 60;

    Calendar cal = Calendar.getInstance();
    cal.set(
        Calendar.HOUR_OF_DAY,
        0); // If there is more than an hour of data, the number of minutes/seconds will increment
    // this
    cal.set(Calendar.MINUTE, (int) minutes);
    cal.set(Calendar.SECOND, (int) seconds);

    if (forBuildComparison) {
      sElapsed =
          String.format(
              "2019-07-01 %02d:%02d:%02d",
              cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
    } else {
      sElapsed =
          String.format(
              "%s %02d:%02d:%02d",
              DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now()),
              cal.get(Calendar.HOUR_OF_DAY),
              cal.get(Calendar.MINUTE),
              cal.get(Calendar.SECOND));
    }

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    try {
      return formatter.parse(sElapsed);
    } catch (ParseException e) {
      logger.error("Unexpected error occurred computing elapsed date", e);
      return null;
    }
  }
}
