package services.philips_hue

import java.awt.Color

import com.philips.lighting.model.{PHLight, PHLightState}

object LightUtils {

  def createSwitchState(status: Boolean): PHLightState = {
    val state = new PHLightState()
    state.setOn(status)
    state
  }

  def createColorStateFromRGB(color: Seq[Int]): PHLightState = {
    val colorHSV = rgbToHsv(color.head, color(1), color.last)
    createColorStateFromHSV(colorHSV)
  }

  def createColorStateFromHSV(color: Seq[Float]): PHLightState = {
    val state = new PHLightState()
    state.setOn(true)

    state.setHue(hueCorrection(color.head * (65535.0f / 360.0f)))
    state.setSaturation((color(1) * 254).toInt)
    state.setBrightness((color(2) * 254).toInt)

    //val colorValue: Int = Color.HSBtoRGB(color.head, color(1), color(2))
    //val xy: Seq[Float] = PHUtilities.calculateXY(colorValue, "LCT001")
    //state.setX(xy(0))
    //state.setY(xy(1))

    state
  }

  def getColor(light: PHLight): Int = {
    val hue: Float = hueRaw(light.getLastKnownLightState.getHue / (65535.0f / 360.0f))
    val sat: Float = light.getLastKnownLightState.getSaturation / 254.0f
    val brightness: Float = light.getLastKnownLightState.getBrightness / 254.0f

    val hsv: Seq[Float] = Seq(hue, sat, brightness)
    Color.HSBtoRGB(hsv.head, hsv(1), hsv(2))
  }

  def hueCorrection(hue: Float): Int = {
    // Red - Yellow   -> y = 15834 / 10922.5 * x
    // Yellow - Green -> y = (9666 * x + 67369980) / 10922.5
    // Green - Blue   -> y = (21420 * x + 89127600) / 21845
    // Blue - Magenta -> y = (9180 * x + 111409500) / 10922.5
    // Magenta - Red  -> y = (9435 * x + 97483312.5) / 10922.5

    hue match {
      case _ if hue < 10922.5f =>
        Math.round(1.449668116f * hue)

      case _ if hue < 21845 =>
        Math.round(0.884962234f * hue + 6168.0f)

      case _ if hue < 43690 =>
        Math.round(0.980544747f * hue + 4080.0f)

      case _ if hue < 54612.5f =>
        Math.round(0.840466926f * hue + 10200.0f)

      case _ =>
        Math.round(0.86381323f * hue + 8925.0f)
    }
  }

  def hueRaw(hue: Float): Int = {
    hue match {
      case _ if hue < 15834.5f =>
        Math.round(hue / 1.449668116f)

      case _ if hue < 25500 =>
        Math.round(1.129991724f * hue - 6969.788950962f)

      case _ if hue < 46920 =>
        Math.round(1.01984127f * hue - 4160.952380952f)

      case _ if hue < 56100 =>
        Math.round(1.189814815f * hue - 12136.111111111f)

      case _ =>
        Math.round(1.157657658f * hue - 10332.094594595f)
    }
  }

  //	public static int hueCorrection(float hue) {
  //	// Red      #FF0000 (255,0,0)    (0º,100%,100%)        0    0
  //	// Yellow   #FFFF00 (255,255,0)  (60º,100%,100%)   10922,5  15834
  //	// Green    #00FF00 (0,255,0)    (120º,100%,100%)  21845    25500
  //	// Blue     #0000FF (0,0,255)    (240º,100%,100%)  43690    46920
  //	// Magenta  #FF00FF (255,0,255)  (300º,100%,100%)  54612,5  56100
  //	// Red      #FF0000 (255,0,0)    (300º,100%,100%)  65535    65535
  //
  //	double a1 = 3.10134003142978 * Math.pow(10,-19) * Math.pow(hue, 5);
  //	double a2 = -5.572016014209 * Math.pow(10,-14) * Math.pow(hue, 4);
  //	double a3 = 3.62369615294603 * Math.pow(10,-9) * Math.pow(hue, 3);
  //	double a4 = -0.0001041196 * Math.pow(hue, 2);
  //	double a5 = 2.2227969787 * hue;
  //	return (int)(a1 + a2 + a3 + a4 + a5 - (4.75263561196466 * Math.pow(10, -11)));
  //}



  def rgbToHsv(r: Float, g: Float, b: Float): Array[Float] = {
    val min = Math.min(r, Math.min(g, b))
    val max = Math.max(r, Math.max(g, b))
    val delta = max - min

    val v = max / 255

    if (delta == 0) {
      Array[Float](0, 0, v)
    } else {
      val s = delta / max

      val rawH =
        if (r == max) ((g - b) / delta) * 60
        else if (g == max) (2 + ((b - r) / delta)) * 60
        else (4 + ((r - g) / delta)) * 60

      val h =
        if (rawH < 0) rawH + 360
        else rawH

      Array[Float](h, s, v)
    }
  }
}
