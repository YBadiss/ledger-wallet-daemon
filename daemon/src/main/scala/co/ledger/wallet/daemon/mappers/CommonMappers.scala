package co.ledger.wallet.daemon.mappers

import co.ledger.wallet.daemon.controllers.responses.ResponseSerializer
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import javax.inject.{Inject, Singleton}

/**
  * Describe your class here.
  *
  * User: Chenyu LU
  * Date: 02-11-2018
  * Time: 11:18
  *
  */
@Singleton
class ThrowableMapper @Inject()(response: ResponseBuilder)
  extends ExceptionMapper[Throwable] {
  override def toResponse(request: Request, throwable: Throwable): Response = {
    ResponseSerializer.serializeInternalError(response, throwable)
  }
}

@Singleton
class IllegalArgumentExceptionMapper @Inject()(response: ResponseBuilder)
  extends ExceptionMapper[IllegalArgumentException] {
  override def toResponse(request: Request, throwable: IllegalArgumentException): Response = {
    ResponseSerializer.serializeBadRequest(Map("response" -> throwable.getMessage), response)
  }
}
