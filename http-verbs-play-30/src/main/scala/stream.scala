/*
 * Copyright 2023 HM Revenue & Customs
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

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

// These helper aliases are package private - but given the packaging in this library (play package should really be inside http package)
// we have to implement for the two disjoint packages that require streaming.
package uk.gov.hmrc.play.http {
  package stream {
    private[http] trait ExecutorServiceDelegate extends org.apache.pekko.dispatch.ExecutorServiceDelegate
  }
  package object stream {
    private[http] type ActorSystem  = org.apache.pekko.actor.ActorSystem

    private[http] object ActorSystem {
      def apply(name: String): ActorSystem =
        org.apache.pekko.actor.ActorSystem.apply(name)
    }

    private[http] def scheduleAfter[T](duration: FiniteDuration, using: org.apache.pekko.actor.Scheduler)(value: => Future[T])(implicit ec: ExecutionContext): Future[T] =
      org.apache.pekko.pattern.after(duration, using)(value)(ec)
  }
}

package uk.gov.hmrc.http {
  package object stream {
    private[http] type ActorSystem  = org.apache.pekko.actor.ActorSystem
    private[http] type ByteString   = org.apache.pekko.util.ByteString
    private[http] type Materializer = org.apache.pekko.stream.Materializer
    private[http] type Source[A, B] = org.apache.pekko.stream.scaladsl.Source[A, B]

    private[http] object ActorSystem {
      def apply(name: String): ActorSystem =
        org.apache.pekko.actor.ActorSystem.apply(name)
    }

    private[http] object Source {
      def single[A](a: A): Source[A, _] =
        org.apache.pekko.stream.scaladsl.Source.single(a)
    }

    private[http] object ByteString {
      def apply(s: String): ByteString =
        org.apache.pekko.util.ByteString.apply(s)
    }

    private[http] def scheduleAfter[T](duration: FiniteDuration, using: org.apache.pekko.actor.Scheduler)(value: => Future[T])(implicit ec: ExecutionContext): Future[T] =
      org.apache.pekko.pattern.after(duration, using)(value)(ec)
  }
}
