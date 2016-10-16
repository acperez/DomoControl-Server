package models.config

sealed trait System extends Serializable {
  val id: String
  val Code: Int
  override def toString: String = id
}

// Philips Hue
object PhilipsHue extends System {
  val id = "Philips Hue"
  val Code = 0
}

object System {
  def apply(value: String) = value match {
    case PhilipsHue.`id` => PhilipsHue
  }

  def apply(value: Int) = value match {
    case PhilipsHue.Code => PhilipsHue
  }
}


