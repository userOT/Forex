import java.time.Clock

import proxy.forex.tasks.RatesActorTask
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class Module extends AbstractModule with AkkaGuiceSupport {

  override def configure(): Unit = {

    bind(classOf[RatesActorTask]).asEagerSingleton()

    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
  }

}
