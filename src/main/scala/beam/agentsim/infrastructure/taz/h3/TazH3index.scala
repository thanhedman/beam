package beam.agentsim.infrastructure.taz.h3

case class TazH3index(hex: List[String], resolution: Int) {


}

object TazH3index {
  private val H3 = com.uber.h3core.H3Core.newInstance
  def buildFromGeofence(geofence: Array[com.vividsolutions.jts.geom.Coordinate]): Option[TazH3index] = {
    import scala.collection.JavaConverters._
    import com.uber.h3core.util.GeoCoord
    val points = geofence.map(coord => new GeoCoord(coord.y, coord.x)).toList.asJava
    // here we assume that all geo fences do not have holes
    val holes = List.empty[java.util.List[GeoCoord]].asJava
    var (hex, res) = (H3.polyfillAddress(points, holes, 0), 0)
    while(hex.isEmpty && res <= 15) {
      res = res + 1
      hex = H3.polyfillAddress(points, holes, res)
    }
    if(hex.isEmpty) None else Some(TazH3index(hex.asScala.toList, res))
  }
}

