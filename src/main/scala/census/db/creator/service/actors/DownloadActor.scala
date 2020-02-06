package census.db.creator.service.actors
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import census.db.creator.config.Config

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.Failure

case class DownloadMessage(url: String)

private case object CheckQueueMessage

class DownloadActor(config: Config, unzipActor: ActorRef)(
  private implicit val executionContext: ExecutionContext,
  private implicit val materializer: Materializer,
) extends Actor {
  private implicit val actorSystem: ActorSystem = context.system

  import scala.concurrent.duration._
  actorSystem.scheduler.schedule(Duration.Zero, 1.second, self, CheckQueueMessage)

  private val filesToDownload = mutable.Queue[String]()
  private val downloadingsInProgress = new AtomicInteger(0)

  override def receive: Receive = {
    case DownloadMessage(url) =>
      filesToDownload += url
    case CheckQueueMessage =>
      while (downloadingsInProgress.get() < 20 && filesToDownload.nonEmpty) {
        downloadUrl(filesToDownload.dequeue())
      }
  }

  private def downloadUrl(url: String) = {
    val fileName = url.split("/").last
    val downloadedFilesDir = "zips"
    val resultFileName = Paths.get(config.workingDir, downloadedFilesDir, fileName)
    new File(resultFileName.toString).delete()

    println(s"Downloading file $fileName")

    downloadingsInProgress.incrementAndGet()

    (for {
      zipFileStream <- Http().singleRequest(HttpRequest(uri = url))
      _             <- zipFileStream.entity.withoutSizeLimit().dataBytes.runWith(FileIO.toPath(resultFileName))
    } yield {
      println(s"Downloaded $fileName")
      unzipActor ! UnzipMessage(resultFileName.toString)
      downloadingsInProgress.decrementAndGet()
    }).andThen {
      case Failure(exception) =>
        exception.printStackTrace()
        throw exception
    }

  }

}
