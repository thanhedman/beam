package beam.router.skim

import beam.agentsim.events.ScalaEvent
import beam.sim.BeamServices
import beam.utils.{FileUtils, ProfilingUtils}
import com.typesafe.scalalogging.LazyLogging
import org.matsim.api.core.v01.events.Event
import org.matsim.core.controler.events.IterationEndsEvent
import org.matsim.core.controler.listener.IterationEndsListener
import org.matsim.core.events.handler.BasicEventHandler
import org.supercsv.io.CsvMapReader
import org.supercsv.prefs.CsvPreference

import scala.collection.{immutable, mutable}

trait H3SkimmerKey
trait H3SkimmerValue

abstract class H3Skimmer {
  protected var skim: mutable.Map[H3SkimmerKey, H3SkimmerValue] = mutable.Map()
  protected var persistedSkim: immutable.Map[H3SkimmerKey, H3SkimmerValue] = read
  protected def cvsFileHeader: String
  protected def cvsFileName: String
  protected def keyValToStrMap(keyVal: (H3SkimmerKey, H3SkimmerValue)): immutable.Map[String, String]
  protected def strMapToKeyVal(strMap: immutable.Map[String, String]): (H3SkimmerKey, H3SkimmerValue)
  private def read: immutable.Map[H3SkimmerKey, H3SkimmerValue] = {
    import scala.collection.JavaConverters._
    val mapReader = new CsvMapReader(FileUtils.readerFromFile(cvsFileName), CsvPreference.STANDARD_PREFERENCE)
    val res = mutable.Map[H3SkimmerKey, H3SkimmerValue]()
    try {
      val header = mapReader.getHeader(true)
      var line: java.util.Map[String, String] = mapReader.read(header: _*)
      while (null != line) {
        val (key, value) = strMapToKeyVal(line.asScala.toMap)
        res.put(key, value)
        line = mapReader.read(header: _*)
      }
    } finally {
      if (null != mapReader)
        mapReader.close()
    }
    res.toMap
  }
  def write(event: org.matsim.core.controler.events.IterationEndsEvent) = {
    val filePath = event.getServices.getControlerIO.getIterationFilename(
      event.getServices.getIterationNumber,
      cvsFileName + ".csv.gz"
    )
    val writer = org.matsim.core.utils.io.IOUtils.getBufferedWriter(filePath)
    writer.write(cvsFileHeader+"\n")
    persistedSkim.map(keyValToStrMap).foreach(row =>
      writer.write(cvsFileHeader.split(",").map(row(_)).mkString(",")+"\n")
    )
    writer.close()
  }
  def getPersistedSkim(key: H3SkimmerKey) = persistedSkim(key)



}

object H3Skimmer {
  abstract class H3SkimmerEvent(time: Double) extends Event(time) with ScalaEvent {
    def getKeyVal: (H3SkimmerKey, H3SkimmerValue)
  }
  class H3SkimmerEventHandler(skimmer: H3Skimmer, beamServices: BeamServices) extends BasicEventHandler with IterationEndsListener with LazyLogging {
    override def handleEvent(event: Event): Unit = {
      event match {
        case e: H3SkimmerEvent => skimmer.skim.put(e.getKeyVal._1, e.getKeyVal._2)
        case _ => // None
      }
    }
    override def notifyIterationEnds(event: IterationEndsEvent): Unit = {
      skimmer.persistedSkim = skimmer.skim.toMap
      skimmer.skim = mutable.Map()
      if (beamServices.beamConfig.beam.outputs.writeSkimsInterval > 0 && event.getIteration % beamServices.beamConfig.beam.outputs.writeSkimsInterval == 0) {
        ProfilingUtils.timed(s"write to ${skimmer.cvsFileName} on iteration ${event.getIteration}", x => logger.info(x)) {
          skimmer.write(event)
        }
      }
    }
  }
}



