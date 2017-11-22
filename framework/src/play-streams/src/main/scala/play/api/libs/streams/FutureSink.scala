/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.streams

import akka.stream.{ Attributes, Inlet, Shape, SinkShape }
import akka.stream.scaladsl.{ Keep, Sink, Source }
import akka.stream.stage.{ GraphStageLogic, GraphStageWithMaterializedValue, InHandler, OutHandler }

import scala.concurrent.{ Future, Promise }

private[streams] class FutureSink[E, A](future: Future[Sink[E, A]])
    extends GraphStageWithMaterializedValue[SinkShape[E], Future[A]] {

  val in: Inlet[E] = Inlet[E]("FutureSink.in")
  override lazy val shape: SinkShape[E] = SinkShape.of(in)
  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[A]) = {
    val promise: Promise[A] = Promise[A]()
    val logic = new FutureSinkLogic("Sink", shape, promise, future) {

      import play.api.libs.streams.Execution.Implicits.trampoline

      override def startGraph(): Unit = {
        val sourceOut = createSubOutlet(in)

        val callback = getAsyncCallback[Sink[E, A]]{ sink =>
          promise.trySuccess(Source.fromGraph(sourceOut.source).toMat(sink)(Keep.right).run()(subFusingMaterializer))
        }

        future.foreach(callback.invoke)
      }

      setHandler(in, new InHandler {
        override def onPush() = ()
      })
    }

    (logic, promise.future)
  }
}

private abstract class FutureSinkLogic[E, A, S <: Shape](
    name: String,
    shape: S,
    promise: Promise[A],
    future: Future[Sink[E, A]]
) extends GraphStageLogic(shape) {
  protected def startGraph(): Unit
  protected final def createSubOutlet[T](in: Inlet[T]): SubSourceOutlet[T] = {
    import play.api.libs.streams.Execution.Implicits.trampoline

    val sourceOut = new SubSourceOutlet[T]("FutureSink.subOut")
    var elements = 0

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
      override def onPush() = {
        elements += 1
        if (sourceOut.isAvailable) {
          sourceOut.push(grab(in))
        }
      }
      override def onUpstreamFinish() = {
        // we never had an element
        // just call the future sink with a empty source
        if (elements == 0) {
          future.foreach(sink => promise.trySuccess(Source.empty.toMat(sink)(Keep.right).run()(subFusingMaterializer)))
        }
        sourceOut.complete()
      }
      override def onUpstreamFailure(ex: Throwable) = {
        sourceOut.fail(ex)
        promise.tryFailure(ex)
      }
    })

    sourceOut
  }

  override def preStart(): Unit = startGraph()
}