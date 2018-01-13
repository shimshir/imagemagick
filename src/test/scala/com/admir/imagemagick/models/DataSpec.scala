package com.admir.imagemagick.models

import com.admir.imagemagick.models.data.{ImageMeta, ImageSize}
import com.admir.imagemagick.models.data.Orientation.Portrait
import com.admir.imagemagick.models.data.ProductType.Poster
import org.scalatest._

class DataSpec extends WordSpec with Matchers {
  "ImageMeta" should {
    "extract image meta data from the file name using 'fromFilename'" in {
      val expectedImageMeta = ImageMeta(
        designerId = 1,
        designId = 2,
        productType = Poster,
        orientation = Portrait,
        size = ImageSize.`80x120`,
        extension = "jpg"
      )
      val actualImageMeta = ImageMeta.fromFilename("1-2-100P-80x120.jpg")

      actualImageMeta shouldEqual expectedImageMeta
    }
  }

  "ImageSize" should {
    "read an ImageSize using 'fromXY'" in {
      // squares
      ImageSize.fromXY(20, 20) shouldEqual ImageSize.`20x20`
      // ... test other square sizes too in a real application

      // rectangles
      ImageSize.fromXY(20, 30) shouldEqual ImageSize.`20x30`
      // ... test other rectangle sizes too in a real application
    }

    "return None for an undefined size using 'fromXYOption'" in {
      ImageSize.fromXYOption(42, 24) shouldBe None
    }

    "get all sub sizes wih the same ratio using 'subSizes'" in {
      ImageSize.`100x100`.subSizes should contain only(
        ImageSize.`100x100`,
        ImageSize.`70x70`,
        ImageSize.`50x50`,
        ImageSize.`30x30`,
        ImageSize.`20x20`
      )

      ImageSize.`30x30`.subSizes should contain only(
        ImageSize.`30x30`,
        ImageSize.`20x20`
      )

      ImageSize.`20x20`.subSizes should contain only ImageSize.`20x20`


      ImageSize.`80x120`.subSizes should contain only(
        ImageSize.`80x120`,
        ImageSize.`60x90`,
        ImageSize.`40x60`,
        ImageSize.`20x30`
      )

      ImageSize.`40x60`.subSizes should contain only(
        ImageSize.`40x60`,
        ImageSize.`20x30`
      )

      ImageSize.`20x30`.subSizes should contain only ImageSize.`20x30`
    }
  }
}
