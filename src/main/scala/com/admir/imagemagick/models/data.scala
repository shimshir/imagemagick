package com.admir.imagemagick.models

import java.io.File

import enumeratum._
import enumeratum.values._

import scala.util.Try
import scala.util.matching.Regex

object data {

  type GeneralError = String
  type RemoteUri = String

  sealed abstract class ProductType(val value: Int) extends IntEnumEntry
  object ProductType extends IntEnum[ProductType] {
    val values = findValues

    case object Poster extends ProductType(100)
    case object FramedArt extends ProductType(200)
    case object Canvas extends ProductType(300)
    case object Acrylic extends ProductType(400)
    case object AluDibond extends ProductType(500)
  }

  sealed abstract class Orientation(val value: String) extends StringEnumEntry
  object Orientation extends StringEnum[Orientation] {
    val values = findValues

    case object Landscape extends Orientation("L")
    case object Portrait extends Orientation("P")
    case object Square extends Orientation("X")
  }

  sealed abstract class ImageSize(val x: Int, val y: Int) extends EnumEntry {
    val ratio: Float = x.toFloat / y

    def sameRatioSizes: Set[ImageSize] =
      ImageSize.values.filter(imageSize => Math.abs(imageSize.ratio - this.ratio) < 0.01).toSet

    def subSizes: Set[ImageSize] = sameRatioSizes.filter(_.x <= this.x)
  }
  object ImageSize extends Enum[ImageSize] {
    val values = findValues

    def fromXY(x: Int, y: Int): ImageSize = {
      fromXYOption(x, y).get
    }

    def fromXYOption(x: Int, y: Int): Option[ImageSize] = {
      values.find(imageSize => imageSize.x == x && imageSize.y == y)
    }

    object `20x20` extends ImageSize(20, 20)
    object `30x30` extends ImageSize(30, 30)
    object `50x50` extends ImageSize(50, 50)
    object `70x70` extends ImageSize(70, 70)
    object `100x100` extends ImageSize(100, 100)

    object `20x30` extends ImageSize(20, 30)
    object `40x60` extends ImageSize(40, 60)
    object `60x90` extends ImageSize(60, 90)
    object `80x120` extends ImageSize(80, 120)
    object `100x150` extends ImageSize(100, 150)
  }

  case class ImageMeta(
                        designerId: Int,
                        designId: Int,
                        productType: ProductType,
                        orientation: Orientation,
                        size: ImageSize,
                        extension: String
                      ) {
    def subSizeVersions: Set[ImageMeta] = {
      size.subSizes.map(subSize => copy(size = subSize))
    }

    override def toString: String = {
      s"$designerId-$designId-${productType.value}${orientation.value}-${size.x}x${size.y}.$extension"
    }
  }
  object ImageMeta {
    val nameRegex: Regex = raw"(\d+)-(\d+)-(\d+)(\w+)-(\d+)x(\d+)\.(\w+)".r
    def fromFilename(filename: String): ImageMeta = {
      filename match {
        case nameRegex(designerId, designId, productTypeStr, orientationStr, x, y, extension) =>
          ImageMeta(
            designerId = designerId.toInt,
            designId = designId.toInt,
            productType = ProductType.withValue(productTypeStr.toInt),
            orientation = Orientation.withValue(orientationStr),
            size = ImageSize.fromXY(x.toInt, y.toInt),
            extension = extension
          )
      }
    }

    def fromFilenameOption(filename: String): Option[ImageMeta] = Try(fromFilename(filename)).toOption
  }

  case class InternalReport(newlyCreatedFiles: Set[File], uploadedFileUris: Set[RemoteUri]) {
    def addFile(file: File): InternalReport = this.copy(newlyCreatedFiles = newlyCreatedFiles + file)
    def addRemoteUri(remoteUri: RemoteUri): InternalReport = this.copy(uploadedFileUris = uploadedFileUris + remoteUri)
  }

  object InternalReport {
    def empty: InternalReport = InternalReport(Set(), Set())
  }
}
