/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.streams

import akka.stream.{ Attributes, Inlet, SinkShape }
import akka.stream.scaladsl.{ Keep, Sink, Source }
import akka.stream.stage.{ GraphStageLogic, GraphStageWithMaterializedValue, InHandler, OutHandler }

import scala.compat.java8.FutureConverters.promise
import scala.concurrent.{ Future, Promise }

private[streams] class FutureSink[E, A](future: Future[Sink[E, A]])
    extends GraphStageWithMaterializedValue[SinkShape[E], Future[A]] {

  val in: Inlet[E] = Inlet[E]("FutureSink.in")
  override lazy val shape: SinkShape[E] = SinkShape.of(in)
  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[A]) = {
    val logic = new GraphStageLogic(shape) {

      val promise: Promise[A] = Promise[A]()

      import play.api.libs.streams.Execution.Implicits.trampoline

      protected final def createSubOutlet[T](in: Inlet[T]): SubSourceOutlet[T] = {
        val sourceOut = new SubSourceOutlet[T]("FutureSink.subOut")

        sourceOut.setHandler(new OutHandler {
          override def onPull() = if (isAvailable(in)) {
            sourceOut.push(grab(in))
          } else {
            if (!hasBeenPulled(in)) {
              pull(in)
            }
          }
          override def onDownstreamFinish() = {
            cancel(in)
          }
        })

        setHandler(in, new InHandler {
          override def onPush() = if (sourceOut.isAvailable) {
            sourceOut.push(grab(in))
          }
          override def onUpstreamFinish() = {
            sourceOut.complete()
          }
          override def onUpstreamFailure(ex: Throwable) = {
            promise.tryFailure(ex)
            sourceOut.fail(ex)
          }
        })

        sourceOut
      }
      override def preStart(): Unit = {
        val sourceOut = createSubOutlet(in)

        val callback = getAsyncCallback[Sink[E, A]] { sink =>
          val subMaterializedValue = Source.fromGraph(sourceOut.source).toMat(sink)(Keep.right).run()(subFusingMaterializer)
          promise.trySuccess(subMaterializedValue)

        }

        future.foreach(callback.invoke)
      }

    }

    (logic, promise.future)
  }
}
