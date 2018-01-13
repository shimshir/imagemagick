package com.admir.imagemagick.services

import java.io.File

import com.admir.imagemagick.models.data.{GeneralError, ImageSize, RemoteUri}

import scala.concurrent.Future

trait ImageFileService {
  def move(from: String, to: String): Future[GeneralError Either File]

  def scale(input: File, size: ImageSize): Future[GeneralError Either File]

  def uploadToS3(file: File): Future[GeneralError Either RemoteUri]

  def delete(file: File): Future[GeneralError Either Unit]
}
