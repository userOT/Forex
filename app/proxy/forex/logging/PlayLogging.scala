package proxy.forex.logging

import play.api.{Logger, Logging}

trait PlayLogging extends Logging {

  override protected[this] val logger : Logger = Logger(logName)

  private def logName = this.getClass.getName.stripSuffix("$")

}
