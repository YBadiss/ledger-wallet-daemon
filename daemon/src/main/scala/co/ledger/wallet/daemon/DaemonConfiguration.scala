package co.ledger.wallet.daemon

import com.typesafe.config.ConfigFactory
import org.spongycastle.util.encoders.Base64
import slick.jdbc.JdbcProfile

import scala.collection.mutable.ListBuffer
import scala.util.Try

object DaemonConfiguration {
  private val config = ConfigFactory.load()
  private val PERMISSION_CREATE_USER: Int = 0x01

  val adminUsers: Seq[String] = if (config.hasPath("demo_users")) {
    val usersConfig = config.getConfigList("demo_users")
    var users = new ListBuffer[String]()
    for (i <- 0 until usersConfig.size()) {
      val userConfig = usersConfig.get(i)
      val username = userConfig.getString("username")
      val password = userConfig.getString("password")
      users += s"Basic ${Base64.toBase64String(s"$username:$password".getBytes)}"
    }
    users.toList
  } else List[String]()

  val whiteListUsers: Seq[(String, Long)] = if (config.hasPath("whitelist")){
    val usersConfig = config.getConfigList("whitelist")
    var users = new ListBuffer[(String, Long)]()
    for (i <- 0 until usersConfig.size()) {
      val userConfig = usersConfig.get(i)
      val pubKey = userConfig.getString("key")
      val permissions = if (Try(userConfig.getBoolean("account_creation")).getOrElse(false)) PERMISSION_CREATE_USER else 0
      users += ((pubKey, permissions))
    }
    users.toList
  } else List[(String, Long)]()

  val authTokenDuration: Int =
    if (config.hasPath("authentication.token_duration")) config.getInt("authentication.token_duration") * 1000
    else 30000

  val dbProfileName: String = Try(config.getString("database_engine")).toOption.getOrElse("sqlite3")

  val dbProfile: JdbcProfile = dbProfileName match {
    case "sqlite3" =>
      slick.jdbc.SQLiteProfile
    case "postgres" =>
      slick.jdbc.PostgresProfile
    case "h2mem1" =>
      slick.jdbc.H2Profile
    case others => throw new Exception(s"Unknown database backend $others")
  }

}