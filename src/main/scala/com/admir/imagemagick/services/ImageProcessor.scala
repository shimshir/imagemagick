package com.admir.imagemagick.services

import java.io.File

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.admir.imagemagick.models.data._

import scala.concurrent.{ExecutionContext, Future}

class ImageProcessor(tmpFolderPath: String, imageFileService: ImageFileService)
                    (implicit ec: ExecutionContext,
                     mat: Materializer) {

  /**
    * Parallelism settings for the IO operations, might be externalized into a configuration
    */
  private val moveFileParallelism = 4
  private val scaleParallelism = 4
  private val uploadParallelism = 4
  private val deleteFileParallelism = 4

  def run(files: Seq[File]): Future[Set[RemoteUri]] = {
    val internalReport =
      Source(files.toList)
        .mapAsync(moveFileParallelism)(file =>
          imageFileService.move(file.getCanonicalPath, s"$tmpFolderPath/${file.getName}"))
        .collect {
          case Right(file) => file

          /** we might log the Left case here or handle the case when moving the file fails */
        }
        .map(movedFile => (movedFile, ImageMeta.fromFilenameOption(movedFile.getName)))
        .collect {
          case (movedFile, Some(imageMeta)) => imageMeta.size.subSizes.map(size => (movedFile, size))

          /** we might log the None case here or handle the case when the file name is invalid */
        }
        .mapConcat(identity)
        .mapAsync(scaleParallelism) { case (movedFile, subSize) => imageFileService.scale(movedFile, subSize) }
        .collect {
          case Right(scaledFile) => scaledFile

          /** we might log the Left case here or handle the case when scaling failed */
        }
        .mapAsync(uploadParallelism)(scaledFile =>
          imageFileService.uploadToS3(scaledFile).map(uploadEither => (scaledFile, uploadEither)))
        .fold(InternalReport.empty)(foldIntoReport)
        .runWith(Sink.head)

    val cleanupResult =
      Source.fromFuture(internalReport)
        .mapConcat(
          /**
            * depending on the behaviour of moveFile,
            * i.e. if the move operation deletes the original file or just copies it,
            * we might chose to skip adding the original files to all the files to be deleted
            */
          _.newlyCreatedFiles ++ files
        )
        .mapAsync(deleteFileParallelism)(imageFileService.delete)
        .runWith(Sink.seq)

    for {
      _ <- cleanupResult
      report <- internalReport
    } yield report.uploadedFileUris
  }

  private def foldIntoReport(reportAcc: InternalReport, file_uploadEither: (File, GeneralError Either RemoteUri)) =
    file_uploadEither match {
      case (file, Left(generalError)) =>

        /** we might log the error here or handle the case when uploading failed */
        reportAcc.addFile(file)
      case (file, Right(remoteUri)) =>
        reportAcc.addFile(file).addRemoteUri(remoteUri)
    }
}
