package co.ledger.wallet.daemon.services

import javax.inject.{Inject, Singleton}

import co.ledger.wallet.daemon.database.DefaultDaemonCache.User
import co.ledger.wallet.daemon.database.{DaemonCache, UserDto}
import co.ledger.wallet.daemon.exceptions.UserAlreadyExistException
import co.ledger.wallet.daemon.utils.HexUtils
import org.bitcoinj.core.Sha256Hash
import org.spongycastle.util.encoders.Base64

import scala.concurrent.Future

@Singleton
class UsersService @Inject()(daemonCache: DaemonCache, ecdsa: ECDSAService) extends DaemonService {

  def user(username: String, password: String): Future[Option[User]] = {
    user(pubKey(username, password))
  }

  def user(pubKey: String, permissions: Long = 0): Future[Option[User]] = {
    daemonCache.getUser(pubKey)
  }

  /**
    * Create new user with public key and permissions.
    *
    * @param publicKey the public key string
    * @param permissions the permissions level
    * @return Future of created user id.
    * @throws UserAlreadyExistException if user already exist.
    */
  def createUser(publicKey: String, permissions: Long = 0): Future[Long] = {
    info(LogMsgMaker.newInstance("Create user")
      .append("pub_key", publicKey)
      .append("permissions", permissions)
      .toString())
    daemonCache.createUser(UserDto(publicKey, permissions))
  }

  /**
    * Create new user with username and password.
    *
    * @param username the username string.
    * @param password the password string.
    * @return Future of created user id.
    * @throws UserAlreadyExistException if user already exist.
    */
  def createUser(username: String, password: String): Future[Long] = {
    assert(username != null && !username.isEmpty, "username must be provided")
    assert(password != null && !password.isEmpty, "password must be provided")
    info(LogMsgMaker.newInstance("Create user")
      .append("username", username)
      .toString())

    createUser(pubKey(username, password))
  }

  private def pubKey(username: String, password: String) = {
    val user = s"Basic ${Base64.toBase64String(s"$username:$password".getBytes)}"
    val privKey = Sha256Hash.hash(user.getBytes)
    HexUtils.valueOf(ecdsa.computePublicKey(privKey))
  }
}