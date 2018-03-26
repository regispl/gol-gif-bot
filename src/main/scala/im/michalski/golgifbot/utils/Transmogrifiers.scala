package im.michalski.golgifbot.utils

import cats.{Applicative, Foldable, Monad, Monoid}
import cats.data.EitherT

import scala.language.higherKinds


object Transmogrifiers {

  implicit class EitherTOps[M[_], F[_], A, B](val self: M[EitherT[F, A, B]]) extends AnyVal {
    def magic(implicit
              MONOIDM: Monoid[M[B]],
              FOLDM: Foldable[M],
              APPLM: Applicative[M],
              APPLF: Applicative[F],
              MONADF: Monad[F]
             ): EitherT[F, A, M[B]] = {
      import cats.implicits._

      val zero = EitherT[F, A, M[B]](APPLF.pure(Right(MONOIDM.empty)))

      FOLDM.foldLeft(self, zero) {
        (acc, a) => acc.flatMap(b => a.map(c => b |+| APPLM.pure(c)))
      }
    }
  }
}
