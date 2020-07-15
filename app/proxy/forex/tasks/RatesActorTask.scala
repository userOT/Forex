package proxy.forex.tasks

import akka.actor.ActorSystem
import javax.inject.Inject
import proxy.forex.model.CurrencyValues
import proxy.forex.services.RatesService

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class RatesActorTask @Inject()(actorSystem: ActorSystem, ratesService: RatesService)(implicit executionContext: ExecutionContext) {

  actorSystem.scheduler.scheduleAtFixedRate(initialDelay = 0.seconds, interval = 4.minute) { () =>
    ratesService.update(CurrencyValues.all)
  }
}

