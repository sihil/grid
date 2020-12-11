package com.gu.mediaservice.lib.cleanup

import com.gu.mediaservice.lib.config.{CommonConfig, SupplierMatch}
import com.gu.mediaservice.model.Image


/**
  * An image processor has a single apply method that takes an `Image` and returns an `Image`. This can be used
  * to modify the image in any number of ways and is primarily used to identify and allocate images from different
  * suppliers and also to clean and conform metadata.
  */
trait ImageProcessor {

  def apply(image: Image): Image
  def description: String = getClass.getCanonicalName

  /**************************
    * Remove the below methods for the codebase merge
    **************************/
  def getMatcher(parserName: String, matches: List[SupplierMatch]): Option[SupplierMatch] =
    matches.find(m => m.name == parserName)

  def matchesCreditOrSource(image: Image, parserName: String, supplierMatches: List[SupplierMatch])= {
    getMatcher(parserName, supplierMatches) match {
      case Some(m) => (image.metadata.credit, image.metadata.source) match {
        case (Some(credit), _) if m.creditMatches.map(_.toLowerCase).exists(credit.toLowerCase.matches) => true
        case (_, Some(source)) if m.sourceMatches.map(_.toLowerCase).exists(source.toLowerCase.matches) => true
        case _ => false
      }
      case _ => false
    }
    /****************************************/
  }
}


trait ComposedImageProcessor extends ImageProcessor {
  def processors: Seq[ImageProcessor]
}

object ImageProcessor {

  val identity: ImageProcessor = new ImageProcessor {
    override def apply(image: Image): Image = image
    override def description: String = "identity"
  }
  /** A convenience method that creates a new ComposedImageProcessor from the provided image processors
    * @param name The string name used to identify this composition
    * @param imageProcessors the underlying image processors that are to be composed
    * @return a new image processor that composes the provided image processors in order
    * */
  def compose(name: String, imageProcessors: ImageProcessor*): ComposedImageProcessor = new ComposedImageProcessor {
    def apply(image: Image): Image =
      imageProcessors
        .foldLeft(image) { case (i, processor) => processor(i) }

    override def description: String = imageProcessors
      .map(_.description)
      .mkString(s"$name(", "; ", ")")

    override def processors: Seq[ImageProcessor] = imageProcessors
  }
}

/**
  * An image processor that simply composes a number of other image processors together.
  * @param imageProcessors the underlying image processors that are to be applied when this imageProcessor is used
  */
class ComposeImageProcessors(val imageProcessors: ImageProcessor*) extends ComposedImageProcessor {
  val underlying: ComposedImageProcessor = ImageProcessor.compose(getClass.getCanonicalName, imageProcessors:_*)
  override def apply(image: Image): Image = underlying.apply(image)
  override def description: String = underlying.description
  override def processors: Seq[ImageProcessor] = underlying.processors
}
