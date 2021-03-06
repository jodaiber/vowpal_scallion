package premise.vw

import scala.io.Source
import org.eintr.loglady.Logging 
import java.{util => ju, io => ji}

import com.twitter.util.Eval
import java.io.File
import org.clapper.argot._
import ArgotConverters._

                

object Train extends Logging {

  def main(args: Array[String]) = { 
    val parser = new ArgotParser("vw_train")
    val model_file_opt = parser.option[String](List("model"), "m", "")
        
    try {
      parser.parse(args)

      val model_file = model_file_opt.value.get
      log.info("model=[%s]" format (model_file))

      // Load the model configuration
      val model_config: VWConfig = new Eval()(new File(model_file))

      // Create a new vw factory for a binary classifier
      val vw_factory = new VWBinaryClassifierFactory { }

      // Run an sgd version of the model (make a vw model with this config)
      val vw_sgd = vw_factory.make(model_config)

      // Each training line is actually an Iterable of chunks of a full training instance
      vw_sgd.train(Source.fromInputStream(System.in)("UTF-8").getLines.map(x=>Seq(x)))

      // Now run the same data through bfgs (for fun) using the sgd model as input
      val vw_bfgs = vw_factory.make(
        model_config
          .copy(bfgs=true, incremental=true, passes=5)
      )
      vw_bfgs.train(Source.fromInputStream(System.in)("UTF-8").getLines.map(x=>Seq(x)))

      log.info("done training.")
    }

    catch {
      case e: ArgotUsageException => log.error(e.message)
    }

  }
}
