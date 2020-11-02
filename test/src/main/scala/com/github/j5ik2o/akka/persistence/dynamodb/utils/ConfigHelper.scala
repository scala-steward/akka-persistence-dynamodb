package com.github.j5ik2o.akka.persistence.dynamodb.utils

import com.typesafe.config.{ Config, ConfigFactory }

object ConfigHelper {

  def config(
      defaultResource: Option[String],
      legacyConfigFormat: Boolean,
      legacyJournalMode: Boolean,
      dynamoDBPort: Int,
      clientVersion: String,
      clientType: String,
      journalRowDriverWrapperClassName: Option[String] = None
  ): Config = {
    val configString = s"""
       |akka.persistence.journal.plugin = "j5ik2o.dynamo-db-journal"
       |akka.persistence.snapshot-store.plugin = "j5ik2o.dynamo-db-snapshot"
       |j5ik2o.dynamo-db-journal {
       |  legacy-config-format = $legacyConfigFormat
       |  shard-count = 1024
       |  queue-enable = true
       |  queue-overflow-strategy = backpressure 
       |  queue-buffer-size = 1024
       |  queue-parallelism = 1
       |  write-parallelism = 1
       |  query-batch-size = 1024
       |  dynamo-db-client {
       |    region = "ap-northeast-1"
       |    access-key-id = "x"
       |    secret-access-key = "x" 
       |    endpoint = "http://127.0.0.1:${dynamoDBPort}/"
       |    client-version = "${clientVersion.toLowerCase}"
       |    client-type = "${clientType.toLowerCase()}"
       |  }
       |  ${if (journalRowDriverWrapperClassName.nonEmpty) {
                            s"""journal-row-driver-wrapper-class-name = "${journalRowDriverWrapperClassName.get}" """
                          } else ""}
       |  columns-def {
       |    sort-key-column-name = ${if (legacyJournalMode) "sequence-nr" else "skey"}
       |  }
       |}
       |
       |j5ik2o.dynamo-db-snapshot {
       |  dynamo-db-client {
       |    region = "ap-northeast-1"
       |    access-key-id = "x"
       |    secret-access-key = "x" 
       |    endpoint = "http://127.0.0.1:${dynamoDBPort}/"
       |  }
       |}
       |
       |j5ik2o.dynamo-db-read-journal {
       |  query-batch-size = 1
       |  dynamo-db-client {
       |    region = "ap-northeast-1"
       |    endpoint = "http://127.0.0.1:${dynamoDBPort}/"
       |  }
       |  columns-def {
       |    sort-key-column-name = ${if (legacyJournalMode) "sequence-nr" else "skey"}
       |  }
       |}
       """.stripMargin
    val config = ConfigFactory
      .parseString(
        configString
      ).withFallback(
        defaultResource.fold(ConfigFactory.load())(ConfigFactory.load)
      )
    // println(ConfigRenderUtils.renderConfigToString(config))
    config
  }
}
