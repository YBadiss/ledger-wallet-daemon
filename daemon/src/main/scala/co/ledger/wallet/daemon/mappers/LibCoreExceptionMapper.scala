package co.ledger.wallet.daemon.mappers

import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import co.ledger.core.implicits.NotEnoughFundsException
import co.ledger.wallet.daemon.controllers.responses.ResponseSerializer
import com.twitter.finagle.http.{Request, Response}
import javax.inject.{Inject, Singleton}

/**
  * Describe your class here.
  *
  * User: Chenyu LU
  * Date: 02-11-2018
  * Time: 11:48
  *
  */
@Singleton
class LibCoreExceptionMapper @Inject()(response: ResponseBuilder)
  extends ExceptionMapper[NotEnoughFundsException] {
  override def toResponse(request: Request, throwable: NotEnoughFundsException): Response = {
    ResponseSerializer.serializeBadRequest(
      Map("response" -> "Not enough funds"), response)
  }
}

