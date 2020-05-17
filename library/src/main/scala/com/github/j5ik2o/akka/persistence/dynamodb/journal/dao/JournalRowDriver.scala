package com.github.j5ik2o.akka.persistence.dynamodb.journal.dao

import akka.NotUsed
import akka.stream.scaladsl.{ Flow, Source, SourceUtils }
import akka.stream.{ ActorAttributes, Attributes }
import com.github.j5ik2o.akka.persistence.dynamodb.config.PluginConfig
import com.github.j5ik2o.akka.persistence.dynamodb.journal.{ JournalRow, PersistenceId, SequenceNumber }

trait JournalRowDriver {

  protected val startTimeSource: Source[Long, NotUsed] =
    SourceUtils
      .lazySource(() => Source.single(System.nanoTime())).mapMaterializedValue(_ => NotUsed)

  protected val logLevels: Attributes = Attributes.logLevels(
    onElement = Attributes.LogLevels.Debug,
    onFailure = Attributes.LogLevels.Error,
    onFinish = Attributes.LogLevels.Debug
  )

  protected def applyV1Dispatcher[A, B](pluginConfig: PluginConfig, flow: Flow[A, B, NotUsed]): Flow[A, B, NotUsed] = {
    pluginConfig.clientConfig.v1ClientConfig.dispatcherName.fold(flow) { name =>
      flow.withAttributes(ActorAttributes.dispatcher(name))
    }
  }

  protected def applyV2Dispatcher[A, B](pluginConfig: PluginConfig, flow: Flow[A, B, NotUsed]): Flow[A, B, NotUsed] = {
    pluginConfig.clientConfig.v2ClientConfig.dispatcherName.fold(flow) { name =>
      flow.withAttributes(ActorAttributes.dispatcher(name))
    }
  }

}

trait JournalRowReadDriver extends JournalRowDriver {

  def getJournalRows(
      persistenceId: PersistenceId,
      toSequenceNr: SequenceNumber,
      deleted: Boolean
  ): Source[Seq[JournalRow], NotUsed]

  def getJournalRows(
      persistenceId: PersistenceId,
      fromSequenceNr: SequenceNumber,
      toSequenceNr: SequenceNumber,
      max: Long,
      deleted: Option[Boolean] = Some(false)
  ): Source[JournalRow, NotUsed]

  def highestSequenceNr(
      persistenceId: PersistenceId,
      fromSequenceNr: Option[SequenceNumber] = None,
      deleted: Option[Boolean] = None
  ): Source[Long, NotUsed]
}

trait JournalRowWriteDriver extends JournalRowReadDriver {

  def singlePutJournalRowFlow: Flow[JournalRow, Long, NotUsed]
  def multiPutJournalRowsFlow: Flow[Seq[JournalRow], Long, NotUsed]

  def updateMessage(journalRow: JournalRow): Source[Unit, NotUsed]

  def singleDeleteJournalRowFlow: Flow[PersistenceIdWithSeqNr, Long, NotUsed]
  def multiDeleteJournalRowsFlow: Flow[Seq[PersistenceIdWithSeqNr], Long, NotUsed]

}