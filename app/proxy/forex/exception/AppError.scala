package proxy.forex.exception

trait AppError extends Throwable {
  def message: String

  override def toString: String = message
}