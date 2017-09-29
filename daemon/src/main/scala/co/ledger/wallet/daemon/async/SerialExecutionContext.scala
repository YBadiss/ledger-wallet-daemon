package co.ledger.wallet.daemon.async

import java.util.concurrent.Executors

import scala.concurrent.{ExecutionContext, Future}

object SerialExecutionContext {
  object Implicits{
    implicit lazy val global = SerialExecutionContextWrapper(ExecutionContext.Implicits.global)
    implicit lazy val single = SerialExecutionContextWrapper(ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor()))
  }
}

class SerialExecutionContextWrapper(implicit val ec: ExecutionContext) extends ExecutionContext with MDCPropagatingExecutionContext {
  private var _lastTask = Future.successful[Unit]()

  override def execute(runnable: Runnable): Unit = synchronized {
    _lastTask = _lastTask.map(_ => runnable.run())
  }

  override def reportFailure(cause: Throwable): Unit = ec.reportFailure(cause)

}

object SerialExecutionContextWrapper {
  def apply(implicit wrapped: ExecutionContext): SerialExecutionContextWrapper = {
    new SerialExecutionContextWrapper()
  }
}