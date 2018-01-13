package com.admir.imagemagick.services

import java.io.File
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.admir.imagemagick.models.data._
import org.scalamock.scalatest.MockFactory
import org.scalatest._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class ImageProcessorSpec extends FlatSpec with Matchers with MockFactory {
  implicit val actorSystem: ActorSystem = ActorSystem("image-magick")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = actorSystem.dispatcher

  val remoteFolderLocation = "host/some/remote/folder"

  "ideal run" should "return the expected list of remote urls" in {
    // define preconditions
    val imageFiles = Seq(
      new File("/original/folder/1-2-100P-80x120.jpg"),
      new File("/original/folder/1-3-100P-60x90.jpg"),
      new File("/original/folder/23-2-300X-70x70.jpg")
    )

    val originalImagesCount = imageFiles.size
    val expectedGeneratedImageMetas = imageFiles.map(ImageMeta.fromFile).flatMap(_.subSizeVersions)
    val generatedImagesCount = expectedGeneratedImageMetas.size

    // define mock and mock expectations
    val mockedImageFileService = mock[ImageFileService]

    mockedImageFileService.move _ expects(*, *) onCall ((_, to) => Future.successful(Right(new File(to)))) repeat originalImagesCount
    mockedImageFileService.scale _ expects(*, *) onCall { (input, size) =>
      input.getParent shouldEqual "/tmp"
      val inputFolder = input.getParent
      val targetFileName = ImageMeta.fromFilename(input.getName).copy(size = size).filename
      val newFilePath = inputFolder + java.io.File.separatorChar + targetFileName
      Future.successful(Right(new File(newFilePath)))
    } repeat generatedImagesCount
    mockedImageFileService.delete _ expects * returning Future.successful(Right(())) repeat originalImagesCount + generatedImagesCount
    mockedImageFileService.uploadToS3 _ expects * onCall ((file: File) => Future.successful(Right(s"$remoteFolderLocation/${file.getName}"))) repeat generatedImagesCount

    // execute the the functionality under test
    val imageProcessor = new ImageProcessor(tmpFolderPath = "/tmp", imageFileService = mockedImageFileService)
    val remoteUris = Await.result(imageProcessor.run(imageFiles), 5.seconds)

    // check assertions
    remoteUris.map(Paths.get(_).getFileName.toString) should contain only (expectedGeneratedImageMetas.map(_.filename): _*)
  }

  "run with errors" should "return the expected list of remote urls" in {
    // define preconditions
    val imageFile = new File("/original/folder/1-2-100P-80x120.jpg")

    val originalImagesCount = 1
    val expectedGeneratedImageMetas = ImageMeta.fromFile(imageFile).subSizeVersions.toSeq
    val generatedImagesCount = expectedGeneratedImageMetas.size

    // define mock and mock expectations
    val mockedImageFileService = mock[ImageFileService]

    mockedImageFileService.move _ expects(*, *) onCall ((_, to) => Future.successful(Right(new File(to)))) repeat originalImagesCount

    // - mock an error when scaling to 40x60
    mockedImageFileService.scale _ expects(*, *) onCall { (input, size) =>
      val inputFolder = input.getParent
      val targetFileName = ImageMeta.fromFilename(input.getName).copy(size = size).filename
      val newFilePath = inputFolder + java.io.File.separatorChar + targetFileName
      if (newFilePath.endsWith("40x60.jpg")) {
        Future.successful(Left("Scaling error"))
      } else {
        Future.successful(Right(new File(newFilePath)))
      }
    } repeat generatedImagesCount
    // - delete should be called one time less than expected since one image was not generated (scaled)
    mockedImageFileService.delete _ expects * returning Future.successful(Right(())) repeat originalImagesCount + generatedImagesCount - 1
    // - uploadToS3 should be called one time less than expected since one image was not generated (scaled)
    mockedImageFileService.uploadToS3 _ expects * onCall { (file: File) =>
      if (file.getName.endsWith("80x120.jpg")) {
        Future.successful(Left("Upload error"))
      } else {
        Future.successful(Right(s"$remoteFolderLocation/${file.getName}"))
      }
    } repeat generatedImagesCount - 1

    // execute the the functionality under test
    val imageProcessor = new ImageProcessor(tmpFolderPath = "/tmp", imageFileService = mockedImageFileService)
    val remoteUris = Await.result(imageProcessor.run(Seq(imageFile)), 5.seconds)

    // check assertions
    remoteUris.map(Paths.get(_).getFileName.toString) should contain only (
      // "1-2-100P-40x60.jpg" is not here cause the scaling fails
      // "1-2-100P-80x120.jpg" is not here cause the uploading fails
      "1-2-100P-20x30.jpg",
      "1-2-100P-60x90.jpg"
    )
  }
}
