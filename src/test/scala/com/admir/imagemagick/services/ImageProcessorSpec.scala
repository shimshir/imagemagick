package com.admir.imagemagick.services

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.admir.imagemagick.models.data._
import org.scalatest._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class ImageProcessorSpec extends FlatSpec with Matchers {
  implicit val actorSystem: ActorSystem = ActorSystem("image-magick")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = actorSystem.dispatcher

  val remoteFolderLocation = "host/some/remote/folder"

  "run" should "return the expected list of remote urls" in {
    val imageFileServiceMock = new ImageFileService {
      def move(from: String, to: String): Future[Either[GeneralError, File]] =
        Future.successful(Right(new File(to)))
      def scale(input: File, size: ImageSize): Future[Either[GeneralError, File]] = {
        input.getParent shouldEqual "/tmp"
        val newFilePath =
          input.getParent + java.io.File.separatorChar + ImageMeta.fromFilename(input.getName).copy(size = size).toString
        Future.successful(Right(new File(newFilePath)))
      }
      def delete(file: File): Future[Either[GeneralError, Unit]] = Future.successful(Right(()))
      def uploadToS3(file: File): Future[Either[GeneralError, RemoteUri]] =
        Future.successful(Right(s"$remoteFolderLocation/${file.getName}"))
    }

    val imageProcessor = new ImageProcessor(tmpFolderPath = "/tmp", imageFileService = imageFileServiceMock)
    val imageFiles = Seq(
      new File("/original/folder/1-2-100P-80x120.jpg"),
      new File("/original/folder/1-3-100P-60x90.jpg"),
      new File("/original/folder/23-2-300X-70x70.jpg")
    )

    val remoteUris = Await.result(imageProcessor.run(imageFiles), 5.seconds)
    remoteUris.toSeq.sorted.foreach(println)
  }
}
