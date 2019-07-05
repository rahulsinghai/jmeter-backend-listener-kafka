package io.github.rahulsinghai.jmeter.backendlistener.kafka;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

public class TestKafkaMetricPublisher {

  @Test
  public void testMetricList() {
    KafkaMetricPublisher pub = new KafkaMetricPublisher(null, null);
    assertEquals(pub.getListSize(), 0);
    pub.addToList("metric1");
    assertEquals(pub.getListSize(), 1);
    pub.clearList();
    assertEquals(pub.getListSize(), 0);
  }
}
