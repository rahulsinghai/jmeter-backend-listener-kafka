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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestMetricsRow {

  private static BackendListenerContext context;

  @BeforeAll
  public static void setUp() {
    final Arguments arguments = new Arguments();
    arguments.addArgument("customArg1", Boolean.toString(false));
    arguments.addArgument("customArg2", "Test project");
    arguments.addArgument("customArg3", "1");
    context = new BackendListenerContext(arguments);
  }

  @AfterAll
  public static void tearDown() {
    context = null;
  }

  @Test
  public void testGetMetricInfo() throws UnknownHostException {
    SampleResult sampleResult = new SampleResult();
    sampleResult.sampleStart();
    try {
      Thread.sleep(110);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    sampleResult.setBytes(100L);
    sampleResult.setSampleLabel("Test Sample");
    sampleResult.setEncodingAndType("text/html");
    sampleResult.setSuccessful(false);

    AssertionResult assertResult = new AssertionResult("assertion1");
    assertResult.setResultForNull();
    sampleResult.addAssertionResult(assertResult);
    sampleResult.sampleEnd();

    String servicePrefixName = "kafka.";
    MetricsRow metricsRow =
        new MetricsRow(
            sampleResult, "info", "yyyy-MM-dd'T'HH:mm:ss.SSSZZ", 0, false, false, new HashSet<>());
    Map<String, Object> mapMetric = metricsRow.getRowAsMap(context, servicePrefixName);
    assertNotNull(mapMetric);
    assertNotNull(mapMetric.get("SampleLabel"));
    assertEquals(mapMetric.get("SampleLabel").toString(), "Test Sample");
  }

  @Test
  public void testGetMetricError() throws UnknownHostException {
    SampleResult sampleResult = new SampleResult();
    sampleResult.sampleStart();
    try {
      Thread.sleep(110);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    sampleResult.setBytes(100L);
    sampleResult.setSampleLabel("Test Sample");
    sampleResult.setEncodingAndType("text/html");
    sampleResult.setSuccessful(true);
    sampleResult.setResponseHeaders("X-kafka-backend:true\\nresponse-header:test");
    sampleResult.sampleEnd();

    String servicePrefixName = "kafka.";
    MetricsRow metricsRow =
        new MetricsRow(
            sampleResult, "error", "yyyy-MM-dd'T'HH:mm:ss.SSSZZ", 1, false, true, new HashSet<>());
    Map<String, Object> mapMetric = metricsRow.getRowAsMap(context, servicePrefixName);
    assertNotNull(mapMetric);
    assertNotNull(mapMetric.get("SampleLabel"));
    assertEquals(mapMetric.get("SampleLabel").toString(), "Test Sample");
  }
}
