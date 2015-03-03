package uk.gov.hmrc.play.test

object Concurrent {
  import scala.concurrent.{Await, Future}
  import scala.concurrent.duration._

  val defaultTimeout = 5 seconds

  implicit def extractAwait[A](future: Future[A]) = await[A](future)
  implicit def liftFuture[A](v: A) = Future.successful(v)

  def await[A](future: Future[A]) = Await.result(future, defaultTimeout)
}
