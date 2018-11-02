package co.ledger.wallet.daemon.services

import java.util.UUID

import javax.inject.{Inject, Singleton}
import co.ledger.wallet.daemon.async.MDCPropagatingExecutionContext
import co.ledger.wallet.daemon.database.DaemonCache
import co.ledger.wallet.daemon.database.DefaultDaemonCache.User
import co.ledger.wallet.daemon.models._
import co.ledger.wallet.daemon.schedulers.observers.SynchronizationResult

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccountsService @Inject()(defaultDaemonCache: DaemonCache) extends DaemonService {
  implicit val ec: ExecutionContext = MDCPropagatingExecutionContext.Implicits.global

  def accounts(user: User, poolName: String, walletName: String): Future[Seq[AccountView]] = {
    defaultDaemonCache.getAccounts(user.pubKey, poolName, walletName).flatMap { accounts =>
      Future.sequence(accounts.map { account => account.accountView })
    }
  }

  def account(accountIndex: Int, user: User, poolName: String, walletName: String): Future[Option[AccountView]] = {
    defaultDaemonCache.getAccount(accountIndex, user.pubKey, poolName, walletName).flatMap {
      case Some(account) => account.accountView.map(Option(_))
      case None => Future(None)
    }
  }

  def synchronizeAccount(accountIndex: Int, user: User, poolName: String, walletName: String): Future[Seq[SynchronizationResult]] ={
     defaultDaemonCache.syncOperations(user.pubKey, poolName, walletName, accountIndex)
  }

  def accountFreshAddresses(accountIndex: Int, user: User, poolName: String, walletName: String): Future[Seq[String]] = {
    defaultDaemonCache.getFreshAddresses(accountIndex, user.pubKey, poolName, walletName)
  }

  def accountDerivationPath(accountIndex: Int, user: User, poolName: String, walletName: String): Future[String] = {
    defaultDaemonCache.getDerivationPath(accountIndex, user.pubKey, poolName, walletName)
  }

  def nextAccountCreationInfo(user: User, poolName: String, walletName: String, accountIndex: Option[Int]): Future[AccountDerivationView] = {
    defaultDaemonCache.getNextAccountCreationInfo(user.pubKey, poolName, walletName, accountIndex).map(_.view)
  }

  def nextExtendedAccountCreationInfo(user: User, poolName: String, walletName: String, accountIndex: Option[Int]): Future[AccountExtendedDerivationView] = {
    defaultDaemonCache.getNextExtendedAccountCreationInfo(user.pubKey, poolName, walletName, accountIndex).map(_.view)
  }

  def accountOperations(
                         user: User,
                         accountIndex: Int,
                         poolName: String,
                         walletName: String,
                         queryParams: OperationQueryParams): Future[PackedOperationsView] = {
    (queryParams.next, queryParams.previous) match {
      case (Some(n), _) =>
        // next has more priority, using database batch instead queryParams.batch
        info(LogMsgMaker.newInstance("Retrieve next batch operation").toString())
        defaultDaemonCache.getNextBatchAccountOperations(user, accountIndex, poolName, walletName, n, queryParams.fullOp)
      case (_, Some(p)) =>
        info(LogMsgMaker.newInstance("Retrieve previous operations").toString())
        defaultDaemonCache.getPreviousBatchAccountOperations(user, accountIndex, poolName, walletName, p, queryParams.fullOp)
      case _ =>
        // new request
        info(LogMsgMaker.newInstance("Retrieve latest operations").toString())
        defaultDaemonCache.getAccountOperations(user, accountIndex, poolName, walletName, queryParams.batch, queryParams.fullOp)
    }
  }

  def accountOperation(user: User, uid: String, accountIndex: Int, poolName: String, walletName: String, fullOp: Int): Future[Option[OperationView]] = {
    defaultDaemonCache.getAccountOperation(user, uid, accountIndex, poolName, walletName, fullOp).flatMap {
      case Some(op) => op.operationView.map(Option(_))
      case None => Future.successful(None)
    }
  }

  def createAccount(accountCreationBody: AccountDerivationView, user: User, poolName: String, walletName: String): Future[AccountView] = {
    defaultDaemonCache.createAccount(accountCreationBody, user, poolName, walletName).flatMap(_.accountView)
  }

  def createAccountWithExtendedInfo(derivations: AccountExtendedDerivationView, user: User, poolName: String, walletName: String): Future[AccountView] = {
    defaultDaemonCache.createAccount(derivations, user, poolName, walletName).flatMap(_.accountView)
  }

}

case class OperationQueryParams(previous: Option[UUID], next: Option[UUID], batch: Int, fullOp: Int)