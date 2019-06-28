# Elasticsearch index template

Below are instructions to create an Elasticsearch template for the index that will be used to store JMeter metrics:

```text
HEAD _template/jmeter_metrics_template
GET /_template/jmeter_metrics_template
GET _template/*jmeter_metrics*
DELETE /_template/jmeter_metrics_template

PUT _template/jmeter_metrics_template
{
  "order": 1,
  "template": "jmeter_metrics-*",
  "settings": {
    "index": {
      "codec": "best_compression",
      "mapping": {
        "total_fields": {
          "limit": "256"
        }
      },
      "refresh_interval": "1s",
      "number_of_replicas": "2",
      "number_of_shards": "1"
    }
  },
  "mappings": {
    "logs": {
      "dynamic_templates": [
        {
          "strings_as_keywords": {
            "match_mapping_type": "string",
            "mapping": {
              "type": "keyword"
            }
          }
        }
      ],
      "_all": {
        "enabled": false
      },
      "properties": {
        "@timestamp": {
          "type": "date"
        },
        "@version": {
          "type": "keyword",
          "ignore_above": 256
        },
        "AllThreads": {
          "type": "integer"
        },
        "AssertionResults": {
          "type": "text"
        },
        "BodySize": {
          "type": "long"
        },
        "BuildNumber": {
          "type": "integer"
        },
        "Bytes": {
          "type": "long"
        },
        "ConnectTime": {
          "type": "long"
        },
        "ContentType": {
          "type": "text",
          "fields": {
            "keyword": {
              "type": "keyword",
              "ignore_above": 256
            }
          }
        },
        "DataType": {
          "type": "keyword",
          "ignore_above": 256
        },
        "ElapsedTime": {
          "type": "date"
        },
        "ElapsedTimeComparison": {
          "type": "date"
        },
        "ErrorCount": {
          "type": "integer"
        },
        "FailureMessage": {
          "type": "text"
        },
        "GrpThreads": {
          "type": "integer"
        },
        "IdleTime": {
          "type": "long"
        },
        "InjectorHostname": {
          "type": "keyword",
          "ignore_above": 256
        },
        "Latency": {
          "type": "long"
        },
        "RequestBody": {
          "type": "text"
        },
        "RequestHeaders": {
          "type": "text"
        },
        "ResponseBody": {
          "type": "text"
        },
        "ResponseCode": {
          "type": "keyword",
          "ignore_above": 256
        },
        "ResponseHeaders": {
          "type": "text"
        },
        "ResponseMessage": {
          "type": "text"
        },
        "ResponseTime": {
          "type": "long"
        },
        "SampleCount": {
          "type": "integer"
        },
        "SampleEndTime": {
          "type": "date"
        },
        "SampleLabel": {
          "type": "keyword",
          "ignore_above": 256
        },
        "SampleStartTime": {
          "type": "date"
        },
        "SentBytes": {
          "type": "long"
        },
        "Success": {
          "type": "boolean"
        },
        "TestElement": {
          "properties": {
            "name": {
              "type": "keyword",
              "ignore_above": 256
            }
          }
        },
        "TestStartTime": {
          "type": "long"
        },
        "ThreadName": {
          "type": "keyword",
          "ignore_above": 256
        },
        "Timestamp": {
          "type": "date"
        },
        "URL": {
          "type": "keyword",
          "ignore_above": 256
        }
      }
    }
  },
  "aliases": {
    "jmeter_metrics": {}
  }
}

DELETE jmeter_metrics-2019-06-28
PUT jmeter_metrics-2019-06-28
GET jmeter_metrics-2019-06-28/_mapping
GET jmeter_metrics-2019-06-28/_aliases
GET jmeter_metrics-2019-06-28/_settings
```
