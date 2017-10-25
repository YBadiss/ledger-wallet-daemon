package co.ledger.wallet.daemon.controllers

import java.util.UUID
import javax.inject.Inject

import co.ledger.wallet.daemon.async.MDCPropagatingExecutionContext
import co.ledger.wallet.daemon.controllers.requests.{CommonMethodValidations, RichRequest}
import co.ledger.wallet.daemon.controllers.responses.ResponseSerializer
import co.ledger.wallet.daemon.exceptions._
import co.ledger.wallet.daemon.filters.AccountCreationContext._
import co.ledger.wallet.daemon.filters.AccountCreationFilter
import co.ledger.wallet.daemon.services.AuthenticationService.AuthentifiedUserContext._
import co.ledger.wallet.daemon.services.{AccountsService, OperationQueryParams}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.request.{QueryParam, RouteParam}
import com.twitter.finatra.validation.{MethodValidation, ValidationResult}

import scala.concurrent.ExecutionContext

class AccountsController @Inject()(accountsService: AccountsService) extends Controller {
  implicit val ec: ExecutionContext = MDCPropagatingExecutionContext.Implicits.global

  import AccountsController._

  get("/pools/:pool_name/wallets/:wallet_name/accounts") { request: AccountRequest =>
    info(s"GET accounts $request")
    accountsService.accounts(request.user, request.pool_name, request.wallet_name).recover {
      case pnfe: WalletPoolNotFoundException => responseSerializer.serializeBadRequest(
        Map("response" -> "Wallet pool doesn't exist", "pool_name" -> request.pool_name),
        response)
      case wnfe: WalletNotFoundException => responseSerializer.serializeBadRequest(
        Map("response"->"Wallet doesn't exist", "wallet_name" -> request.wallet_name),
        response)
      case e: Throwable => responseSerializer.serializeInternalError(response, e)
    }
  }

  get("/pools/:pool_name/wallets/:wallet_name/accounts/next") { request: AccountCreationInfoRequest =>
    info(s"GET account creation info $request")
    accountsService.nextAccountCreationInfo(request.user, request.pool_name, request.wallet_name, request.account_index).recover {
      case pnfe: WalletPoolNotFoundException => responseSerializer.serializeBadRequest(
        Map("response" -> "Wallet pool doesn't exist", "pool_name" -> request.pool_name),
        response)
      case wnfe: WalletNotFoundException => responseSerializer.serializeBadRequest(
        Map("response" -> "Wallet doesn't exist", "wallet_name" -> request.wallet_name),
        response)
      case e: Throwable => responseSerializer.serializeInternalError(response, e)
    }
  }

  get("/pools/:pool_name/wallets/:wallet_name/accounts/:account_index") { request: AccountRequest =>
    info(s"GET account $request")
    accountsService.account(request.account_index.get, request.user, request.pool_name, request.wallet_name).map {
      case Some(view) => responseSerializer.serializeOk(view, response)
      case None => responseSerializer.serializeNotFound(Map("response" -> "Account doesn't exist", "account_index" -> request.account_index), response)
    }.recover {
      case pnfe: WalletPoolNotFoundException => responseSerializer.serializeBadRequest(
        Map("response" -> "Wallet pool doesn't exist", "pool_name" -> request.pool_name),
        response)
      case wnfe: WalletNotFoundException => responseSerializer.serializeBadRequest(
        Map("response"->"Wallet doesn't exist", "wallet_name" -> request.wallet_name),
        response)
      case e: Throwable => responseSerializer.serializeInternalError(response, e)
    }
  }

  get("/pools/:pool_name/wallets/:wallet_name/accounts/:account_index/operations") { request: OperationRequest =>
    info(s"GET account operation $request")
    accountsService.accountOperation(
      request.user,
      request.account_index,
      request.pool_name,
      request.wallet_name,
      OperationQueryParams(request.previous, request.next, request.batch, request.full_op)).recover {
        case pnfe: WalletPoolNotFoundException => responseSerializer.serializeBadRequest(
          Map("response" -> "Wallet pool doesn't exist", "pool_name" -> request.pool_name),
          response)
        case onfe: OperationNotFoundException => responseSerializer.serializeBadRequest(
          Map("response" -> "Operation cursor doesn't exist", "next_cursor" -> request.next, "previous_cursor" -> request.previous),
          response)
        case wnfe: WalletNotFoundException => responseSerializer.serializeBadRequest(
          Map("response"->"Wallet doesn't exist", "wallet_name" -> request.wallet_name),
          response)
        case anfe: AccountNotFoundException => responseSerializer.serializeBadRequest(
          Map("response"->"Account doesn't exist", "account_index" -> request.account_index),
          response)
        case e: Throwable => responseSerializer.serializeInternalError(response, e)
      }
  }

  filter[AccountCreationFilter]
    .post("/pools/:pool_name/wallets/:wallet_name/accounts") { request: Request =>
      val walletName = request.getParam("wallet_name")
      val poolName = request.getParam("pool_name")
      info(s"CREATE account $request, Parameters(user: ${request.user.get.id}, pool_name: $poolName, wallet_name: $walletName), Body(${request.accountCreationBody}")
      accountsService.createAccount(request.accountCreationBody,request.user.get,poolName,walletName).recover {
        case iae: InvalidArgumentException => responseSerializer.serializeBadRequest(
          Map("response"-> iae.msg, "pool_name" -> poolName, "wallet_name"->walletName),
          response)
        case pnfe: WalletPoolNotFoundException => responseSerializer.serializeBadRequest(
          Map("response" -> "Wallet pool doesn't exist", "pool_name" -> poolName),
          response)
        case wnfe: WalletNotFoundException => responseSerializer.serializeBadRequest(
          Map("response"->"Wallet doesn't exist", "wallet_name" -> walletName),
          response)
        case e: Throwable => responseSerializer.serializeInternalError(response, e)
      }
  }

  delete("/pools/:pool_name/wallets/:wallet_name/accounts") { request: AccountRequest =>
    info(s"DELETE account $request")
    //TODO
  }

  private val responseSerializer: ResponseSerializer = ResponseSerializer.newInstance()
}

object AccountsController {
  case class AccountRequest(
                           @RouteParam pool_name: String,
                           @RouteParam wallet_name: String,
                           @RouteParam account_index: Option[Int],
                           request: Request
                           ) extends RichRequest(request) {
    @MethodValidation
    def validatePoolName: ValidationResult = CommonMethodValidations.validateName("pool_name", pool_name)

    @MethodValidation
    def validateWalletName: ValidationResult = CommonMethodValidations.validateName("wallet_name", wallet_name)

    override def toString: String = s"$request, Parameters(user: ${user.id}, pool_name: $pool_name, wallet_name: $wallet_name, account_index: $account_index)"
  }

  case class AccountCreationInfoRequest(
                                         @RouteParam pool_name: String,
                                         @RouteParam wallet_name: String,
                                         @QueryParam account_index: Option[Int],
                                         request: Request
                                       ) extends RichRequest(request) {
    @MethodValidation
    def validatePoolName: ValidationResult = CommonMethodValidations.validateName("pool_name", pool_name)

    @MethodValidation
    def validateWalletName: ValidationResult = CommonMethodValidations.validateName("wallet_name", wallet_name)

    @MethodValidation
    def validateAccountIndex: ValidationResult = CommonMethodValidations.validateOptionalAccountIndex(account_index)

    override def toString: String = s"$request, Parameters(user: ${user.id}, pool_name: $pool_name, wallet_name: $wallet_name, account_index: $account_index)"
  }
  case class OperationRequest(
                             @RouteParam pool_name: String,
                             @RouteParam wallet_name: String,
                             @RouteParam account_index: Int,
                             @QueryParam next: Option[UUID],
                             @QueryParam previous: Option[UUID],
                             @QueryParam batch: Int = 20,
                             @QueryParam full_op: Int = 0,
                             request: Request
                             ) extends RichRequest(request) {
    @MethodValidation
    def validatePoolName: ValidationResult = CommonMethodValidations.validateName("pool_name", pool_name)

    @MethodValidation
    def validateWalletName: ValidationResult = CommonMethodValidations.validateName("wallet_name", wallet_name)

    @MethodValidation
    def validateAccountIndex: ValidationResult = ValidationResult.validate(account_index >= 0, s"account_index: index can not be less than zero")

    @MethodValidation
    def validateBatch: ValidationResult = ValidationResult.validate(batch > 0, "batch: batch should be greater than zero")

    override def toString: String = s"$request, Parameters(user: ${user.id}, pool_name: $pool_name, wallet_name: $wallet_name, account_index: $account_index, next: $next, previous: $previous, batch: $batch, full_op: $full_op)"
  }
}