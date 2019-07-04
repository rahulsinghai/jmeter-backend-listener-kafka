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
  "index_patterns": "jmeter_metrics-*",
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
      "properties": {
        "@timestamp": {
          "type": "date",
          "format": "dateOptionalTime"
        },
        "@version": {
          "type": "keyword",
          "ignore_above": 256
        },
        "AllThreads": {
          "type": "integer"
        },
        "AssertionResults": {
          "properties": {
            "failure": {
              "type": "boolean"
            },
            "failureMessage": {
              "type": "text",
              "index": false
            },
            "name": {
              "type": "text",
              "index": false
            }
          }
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
          "type": "date",
          "format": "dateOptionalTime"
        },
        "ElapsedTimeComparison": {
          "type": "date",
          "format": "dateOptionalTime"
        },
        "ErrorCount": {
          "type": "integer"
        },
        "FailureMessage": {
          "type": "text",
          "index": false
        },
        "GrpThreads": {
          "type": "integer"
        },
        "IdleTime": {
          "type": "long"
        },
        "InjectorHostname": {
          "type": "text",
          "index": false
        },
        "Latency": {
          "type": "long"
        },
        "RequestBody": {
          "type": "text",
          "index": false
        },
        "RequestHeaders": {
          "type": "text",
          "index": false
        },
        "ResponseBody": {
          "type": "text",
          "index": false
        },
        "ResponseCode": {
          "type": "keyword",
          "ignore_above": 256
        },
        "ResponseHeaders": {
          "type": "text",
          "index": false
        },
        "ResponseMessage": {
          "type": "text",
          "index": false
        },
        "ResponseTime": {
          "type": "long"
        },
        "SampleCount": {
          "type": "integer"
        },
        "SampleEndTime": {
          "type": "date",
          "format": "dateOptionalTime"
        },
        "SampleLabel": {
          "type": "keyword",
          "ignore_above": 256
        },
        "SampleStartTime": {
          "type": "date",
          "format": "dateOptionalTime"
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
              "type": "text",
              "index": false
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
          "type": "date",
          "format": "dateOptionalTime"
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

GET _cat/indices/jmeter_metrics-*?v&s=index
GET jmeter_metrics/_search
{
  "query": {
    "match_all": {}
  }
}
```
