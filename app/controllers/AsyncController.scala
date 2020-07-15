package controllers

import javax.inject._
import play.api.libs.json.Json
import play.api.mvc._
import proxy.forex.model.CurrencyValues.CurrencyUnsupported
import proxy.forex.services.RatesService
import proxy.forex.services.RatesService.CacheIsNotReady
import validators.CurrencyExchangeRequestValidator

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal


@Singleton
class AsyncController @Inject()(cc: ControllerComponents,
                                ratesService: RatesService,
                                currencyExchangeValidator: CurrencyExchangeRequestValidator)
                               (implicit exec: ExecutionContext) extends AbstractController(cc) {

  def index: Action[AnyContent] = Action.async {
    Future.successful(Ok("App started"))
  }

  def rates: Action[AnyContent] = Action.async { implicit rq =>
    currencyExchangeValidator.validateToRun { pairs =>
      ratesService.convert(pairs).map(result => Ok(Json.toJson(result)))
    }.recoverWith {
      case error: CacheIsNotReady => Future.successful(Accepted(error.message))
      case error: CurrencyUnsupported => Future.successful(BadRequest(error.message))
      case NonFatal(_) => Future.successful(InternalServerError("Internal server error"))
    }
  }
}