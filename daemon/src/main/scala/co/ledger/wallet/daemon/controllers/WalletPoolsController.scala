package co.ledger.wallet.daemon.controllers

import javax.inject.Inject

import co.ledger.wallet.daemon.async.MDCPropagatingExecutionContext
import co.ledger.wallet.daemon.controllers.requests.{CommonMethodValidations, RichRequest}
import co.ledger.wallet.daemon.controllers.responses.ResponseSerializer
import co.ledger.wallet.daemon.services.AuthenticationService.AuthentifiedUserContext._
import co.ledger.wallet.daemon.services.PoolsService
import co.ledger.wallet.daemon.services.PoolsService.PoolConfiguration
import com.fasterxml.jackson.annotation.JsonProperty
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.request.RouteParam
import com.twitter.finatra.validation.{MethodValidation, NotEmpty, ValidationResult}

import scala.concurrent.ExecutionContext

class WalletPoolsController @Inject()(poolsService: PoolsService) extends Controller {
  implicit val ec: ExecutionContext = MDCPropagatingExecutionContext.Implicits.global

  import WalletPoolsController._

  /**
    * End point queries for wallet pools view.
    *
    */
  get("/pools") { request: Request =>
    info(s"GET wallet pools $request, Parameters(user: ${request.user.get.id})")
    poolsService.pools(request.user.get)
  }

  /**
    * End point queries for wallet pool view by the specified name of this pool. The
    * name of a pool is unique per user.
    *
    */
  get("/pools/:pool_name") { request: PoolRouteRequest =>
    info(s"GET wallet pool $request")
    poolsService.pool(request.user, request.pool_name).map {
      case Some(view) => ResponseSerializer.serializeOk(view, response)
      case None => ResponseSerializer.serializeNotFound(
        Map("response" -> "Wallet pool doesn't exist", "pool_name" -> request.pool_name), response)
    }.recover {
      case e: Throwable => ResponseSerializer.serializeInternalError(response, e)
    }
  }

  /**
    * End point to trigger the synchronization process of existing wallet pools of user.
    *
    */
  post("/pools/operations/synchronize") { request: Request =>
    info(s"SYNC wallet pools $request, Parameters(user: ${request.user.get.id})")
    val t0 = System.currentTimeMillis()
    poolsService.syncOperations().map { result =>
      val t1 = System.currentTimeMillis()
      info(s"Synchronization finished, elapsed time: ${t1 - t0} milliseconds")
      result
    }
  }

  /**
    * End point to trigger the synchronization process of one existing wallet pool
    *
    */
  post("/pools/:pool_name/operations/synchronize") { request: PoolRouteRequest =>
    poolsService.syncOperations(request.user, request.pool_name)
  }

  /**
    * End point to create a new wallet pool.
    *
    */
  post("/pools") { request: CreationRequest =>
    info(s"CREATE wallet pool $request")
    // TODO: Deserialize the configuration from the body of the request
    poolsService.createPool(request.user, request.pool_name, PoolConfiguration())
  }

  /**
    * End point to delete a wallet pool instance. The operation will delete the wallet
    * pool record from daemon database and remove the reference to core library. After the
    * operation, user will not be able to access the underlying wallets or accounts.
    *
    */
  delete("/pools/:pool_name") { request: PoolRouteRequest =>
    info(s"DELETE wallet pool $request")
    poolsService.removePool(request.user, request.pool_name)
  }

}

object WalletPoolsController {

  case class CreationRequest(
                              @NotEmpty @JsonProperty pool_name: String,
                              request: Request
                            ) extends RichRequest(request) {
    @MethodValidation
    def validatePoolName: ValidationResult = CommonMethodValidations.validateName("pool_name", pool_name)

    override def toString: String = s"$request, Parameters(user: ${user.id}, pool_name: $pool_name)"
  }

  case class PoolRouteRequest(
                               @RouteParam pool_name: String,
                               request: Request) extends RichRequest(request) {
    @MethodValidation
    def validatePoolName: ValidationResult = CommonMethodValidations.validateName("pool_name", pool_name)

    override def toString: String = s"$request, Parameters(user: ${user.id}, pool_name: $pool_name)"
  }

}