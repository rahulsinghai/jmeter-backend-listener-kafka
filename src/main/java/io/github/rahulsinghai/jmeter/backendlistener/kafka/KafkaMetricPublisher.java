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

import java.util.LinkedList;
import java.util.List;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper around Kafka Producer to publish messages.
 *
 * @author rahulsinghai
 * @since 20190624
 */
class KafkaMetricPublisher {

  private static final Logger logger = LoggerFactory.getLogger(KafkaMetricPublisher.class);

  private KafkaProducer<Long, String> producer;
  private String topic;
  private List<String> metricList;

  KafkaMetricPublisher(KafkaProducer<Long, String> producer, String topic) {
    this.producer = producer;
    this.topic = topic;
    this.metricList = new LinkedList<>();
  }

  /**
   * This method returns the current size of the JSON documents list
   *
   * @return integer representing the size of the JSON documents list
   */
  public int getListSize() {
    return this.metricList.size();
  }

  /** This method closes the producer */
  public void closeProducer() {
    this.producer.flush();
    this.producer.close();
  }

  /** This method clears the JSON documents list */
  public void clearList() {
    this.metricList.clear();
  }

  /**
   * This method adds a metric to the list (metricList).
   *
   * @param metric String parameter representing a JSON document for Kafka
   */
  public void addToList(String metric) {
    this.metricList.add(metric);
  }

  /** This method publishes the documents present in the list (metricList). */
  public void publishMetrics() {

    long time = System.currentTimeMillis();
    for (int i = 0; i < this.metricList.size(); i++) {
      final ProducerRecord<Long, String> record =
          new ProducerRecord<>(this.topic, i + time, metricList.get(i));
      producer.send(
          record,
          (metadata, exception) -> {
            long elapsedTime = System.currentTimeMillis() - time;
            if (metadata != null) {
              if (logger.isDebugEnabled()) {
                logger.debug(
                    "Record sent with (key=%s value=%s) "
                        + "meta(partition=%d, offset=%d) time=%d\n",
                    record.key(),
                    record.value(),
                    metadata.partition(),
                    metadata.offset(),
                    elapsedTime);
              }
            } else {
              if (logger.isErrorEnabled()) {
                logger.error("Exception: " + exception);
                logger.error(
                    "Kafka Backend Listener was unable to publish to the Kafka topic {}.",
                    this.topic);
              }
            }
          });
    }
  }
}
