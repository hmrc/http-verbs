import uk.gov.hmrc.playcrosscompilation.AbstractPlayCrossCompilation
import uk.gov.hmrc.playcrosscompilation.PlayVersion.{Play25, Play26, Play27}

object PlayCrossCompilation extends AbstractPlayCrossCompilation(defaultPlayVersion = Play25)
