package beam.utils

import beam.agentsim.infrastructure.taz.TAZTreeMap
import beam.utils.H3Utils.toJtsCoordinate
import com.uber.h3core.util.GeoCoord
import com.vividsolutions.jts.geom.{Coordinate, Geometry}
import org.opengis.feature.simple.SimpleFeature
import org.matsim.api.core.v01.Coord
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation

object H3Utils {

  val H3 = com.uber.h3core.H3Core.newInstance

  def main(args: Array[String]): Unit = {
    test
    println("End")
  }

  def test = {
    import scala.collection.JavaConverters._
    val lat = 37.775938728915946
    val lng = -122.41795063018799
    val res = 9
    val hexAddr = H3.geoToH3Address(lat, lng, res)
    val hexAddrs = H3.h3ToChildren(hexAddr, res+1).asScala.toList :+ hexAddr :+ H3.h3ToParentAddress(hexAddr, res-1)
    val geoCoords = H3.h3ToGeoBoundary(hexAddr)
    val out = hexToJtsPolygon(geoCoords)
    println(out)
    writeHexToShp(hexAddrs, "output/test/polygons.shp")
  }

  def test2 = {
    val geography = Array(
      new Coord(-122.4089866999972145, 37.813318999983238),
      new Coord(-122.3805436999997056, 37.7866302000007224),
      new Coord(-122.3544736999993603, 37.7198061999978478),
      new Coord(-122.5123436999983966, 37.7076131999975672),
      new Coord(-122.5247187000021967, 37.7835871999971715),
      new Coord(-122.4798767000009008, 37.8151571999998453)
    )
    val h3index = geoToH3index(geography, 8)
    println(h3index)
    writeHexToShp(h3index, "output/test/polygons.shp")
  }

  def test3 = {
    val geography = Array(
      new Coord(-122.49118500000002, 37.646534999085276),
      new Coord(-122.49168500000003, 37.6448349990853),
      new Coord(-122.48997500000002, 37.637748999085346),
      new Coord(-122.48958500000003, 37.63303499908539),
      new Coord(-122.49358500000002, 37.62983499908543),
      new Coord(-122.49529425555099, 37.62975360596395),
      new Coord(-122.49408500000004, 37.644034999085314),
      new Coord(-122.49118500000002, 37.646534999085276)
    )
    val h3index = geoToH3index(geography, 9)
    println(h3index)
    writeHexToShp(h3index, "output/test/polygons.shp")
  }

  def sfBay = {
    val toWGS84: GeotoolsTransformation = new GeotoolsTransformation("epsg:26910", "EPSG:4326")
    import scala.collection.JavaConverters._
    val tazMap = TAZTreeMap.fromShapeFile("test/input/sf-light/shape/sf-light-tazs.shp", "taz")
    println("toH3index")
    tazMap.tazQuadTree.values().asScala.take(1).head.geography.map(toWGS84.transform).foreach(println)
    val h3index =
      tazMap.tazQuadTree.values().asScala.flatMap(taz => geoToH3index(taz.geography.map(toWGS84.transform), 9))
    println("writing")
    writeHexToShp(h3index.toList, "output/test/polygons.shp")
  }

  def geoToH3index(g: Array[Coord], resolution: Int = 0): List[String] = {
    import scala.collection.JavaConverters._
    import com.uber.h3core.util.GeoCoord
    val points = g.map(c => new GeoCoord(c.getY, c.getX)).toList.asJava
    val holes = List.empty[java.util.List[GeoCoord]].asJava
    H3.polyfillAddress(points, holes, resolution).asScala.toList
  }

  def hexToJtsPolygon(coord: java.util.List[GeoCoord]): com.vividsolutions.jts.geom.Polygon = {
    import scala.collection.JavaConverters._
    new com.vividsolutions.jts.geom.GeometryFactory()
      .createPolygon(coord.asScala.map(toJtsCoordinate).toArray :+ toJtsCoordinate(coord.get(0)))
  }

  def toJtsCoordinate(in: GeoCoord): com.vividsolutions.jts.geom.Coordinate = {
    new com.vividsolutions.jts.geom.Coordinate(in.lng, in.lat)
  }

  def writeHexToShp(hex: List[String], filename: String) = {
    import org.matsim.core.utils.gis.ShapeFileWriter
    import org.matsim.core.utils.geometry.geotools.MGC
    import org.matsim.core.utils.gis.PolygonFeatureFactory
    import scala.collection.JavaConverters._
    val pf: PolygonFeatureFactory = new PolygonFeatureFactory.Builder()
      .setCrs(MGC.getCRS("EPSG:4326"))
      .setName("nodes")
      .addAttribute("ID", classOf[String])
      .create()
    println("here")
    val polygons = hex.map(
      h =>
        pf.createPolygon(
          H3.h3ToGeoBoundary(h).asScala.map(c => new Coordinate(c.lng, c.lat)).toArray,
          Array[Object](h),
          null
      )
    )
    println("there")
    ShapeFileWriter.writeGeometries(polygons.asJavaCollection, filename)
  }

}
