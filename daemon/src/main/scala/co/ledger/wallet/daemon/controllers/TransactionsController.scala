package co.ledger.wallet.daemon.controllers

import co.ledger.wallet.daemon.async.MDCPropagatingExecutionContext
import co.ledger.wallet.daemon.controllers.requests.{CommonMethodValidations, RequestWithUser}
import co.ledger.wallet.daemon.database.DefaultDaemonCache.User
import co.ledger.wallet.daemon.models.FeeMethod
import co.ledger.wallet.daemon.services.TransactionsService
import co.ledger.wallet.daemon.utils.HexUtils
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.request.RouteParam
import com.twitter.finatra.validation.{MethodValidation, ValidationResult}
import javax.inject.Inject

import scala.concurrent.ExecutionContext

/**
  * The controller for transaction operations.
  *
  * User: Ting Tu
  * Date: 24-04-2018
  * Time: 11:07
  *
  */
class TransactionsController @Inject()(transactionsService: TransactionsService) extends Controller {
  implicit val ec: ExecutionContext = MDCPropagatingExecutionContext.Implicits.global

  import TransactionsController._

  /**
    * Transaction creation method.
    * Input json
    * {
    * recipient: recipient address,
    * fees_per_byte: optional(in satoshi),
    * fees_level: optional(SLOW, FAST, NORMAL),
    * amount: in satoshi,
    * exclude_utxos: map{txHash: index}
    * }
    *
    */
  post("/pools/:pool_name/wallets/:wallet_name/accounts/:account_index/transactions") { request: Request =>
    val (poolName, walletName, accountIndex) = (request.getParam("pool_name"), request.getParam("wallet_name"), request.getIntParam("account_index"))
    info(s"Create transaction $request")
    request match {
      case b: CreateBTCTransactionRequest =>
        transactionsService.createTransaction(
          b.transactionInfo,
          b.accountInfo)
      case e: CreateETHTransactionRequest =>
        info(s"Create transaction ${e.message}")
    }
  }

  /**
    * Send a signed transaction.
    * Input json
    * {
    * raw_transaction: the bytes,
    * signatures: [string],
    * pubkeys: [string]
    * }
    */
  post("/pools/:pool_name/wallets/:wallet_name/accounts/:account_index/transactions/sign") { request: PublishBTCTransactionRequest =>
    info(s"Sign transaction $request")
    transactionsService.signTransaction(request.rawTx, request.pairedSignatures, request.accountInfo)
  }

}

object TransactionsController {

  trait PushTransactionRequest extends RequestWithUser

  case class PublishETHTransactionRequest(
                                           raw_transaction: String,
                                           request: Request
                                         ) extends PushTransactionRequest

  case class PublishBTCTransactionRequest(
                                           @RouteParam pool_name: String,
                                           @RouteParam wallet_name: String,
                                           @RouteParam account_index: Int,
                                           raw_transaction: String,
                                           signatures: Seq[String],
                                           pubkeys: Seq[String],
                                           request: Request
                                         ) extends PushTransactionRequest {
    val accountInfo: AccountInfo = AccountInfo(pool_name, wallet_name, account_index, user)
    val rawTx: Array[Byte] = HexUtils.valueOf(raw_transaction)
    lazy val pairedSignatures: Seq[(Array[Byte], Array[Byte])] = signatures.zipWithIndex.map { case (sig, index) =>
      (HexUtils.valueOf(sig), HexUtils.valueOf(pubkeys(index)))
    }

    @MethodValidation
    def validateSignatures: ValidationResult = ValidationResult.validate(
      signatures.size == pubkeys.size,
      "signatures and pubkeys size not matching")

    override def toString: String = s"$request, Parameters($accountInfo, raw_transaction: $raw_transaction, signatures: $signatures, pubkeys: $pubkeys)"

  }

  trait CreateTransactionRequest extends RequestWithUser

  case class CreateETHTransactionRequest(
                                          message: String,
                                          request: Request
                                        ) extends CreateTransactionRequest

  case class CreateBTCTransactionRequest(@RouteParam pool_name: String,
                                         @RouteParam wallet_name: String,
                                         @RouteParam account_index: Int,
                                         recipient: String,
                                         fees_per_byte: Option[Long],
                                         fees_level: Option[String],
                                         amount: Long,
                                         exclude_utxos: Option[Map[String, Int]],
                                         request: Request) extends CreateTransactionRequest {

    val accountInfo: AccountInfo = AccountInfo(pool_name, wallet_name, account_index, user)
    val transactionInfo: BTCTransactionInfo = BTCTransactionInfo(recipient, fees_per_byte, fees_level, amount, exclude_utxos.getOrElse(Map[String, Int]()))

    @MethodValidation
    def validateFees: ValidationResult = CommonMethodValidations.validateFees(fees_per_byte, fees_level)

    override def toString: String = s"$request, Parameters($accountInfo, $transactionInfo)"
  }

  case class AccountInfo(poolName: String, walletName: String, index: Int, user: User) {
    override def toString: String = s"account_info(user: ${user.id}, pool_name: $poolName, wallet_name: $walletName, account_index: $index)"
  }

  trait TransactionInfo

  case class BTCTransactionInfo(recipient: String, feeAmount: Option[Long], feeLevel: Option[String], amount: Long, excludeUtxos: Map[String, Int]) extends TransactionInfo {
    lazy val feeMethod: Option[FeeMethod] = feeLevel.map { level => FeeMethod.from(level) }

    override def toString: String = s"transaction_info(recipient: $recipient, fee_ammount: $feeAmount, fee_level: $feeLevel, amount: $amount, exclude_utxos: $excludeUtxos)"
  }

  case class ETHTransactionInfo() extends TransactionInfo

}
