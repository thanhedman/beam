package beam.physsim.jdeqsim

import java.io.File
import java.nio.file.Paths

import com.google.common.io.Files
import com.google.ortools.graph.LinearSumAssignment

object OrToolsExample {
  private def runAssignmentOn4x4Matrix(): Unit = {
    val numSources = 4
    val numTargets = 4
    val cost =
      Array(Array(90, 76, 75, 80), Array(35, 85, 55, 65), Array(125, 95, 90, 105), Array(45, 110, 95, 115))
    val expectedCost = cost(0)(3) + cost(1)(2) + cost(2)(1) + cost(3)(0)
    val assignment = new LinearSumAssignment
    var source = 0
    while (source < numSources) {
      var target = 0
      while ({ target < numTargets }) {
        assignment.addArcWithCost(source, target, cost(source)(target))

        target += 1
      }

      source += 1
    }
    if (assignment.solve eq LinearSumAssignment.Status.OPTIMAL) {
      System.out.println("Total cost = " + assignment.getOptimalCost + "/" + expectedCost)
      var node = 0
      while ({ node < assignment.getNumNodes }) {
        System.out.println(
          "Left node " + node + " assigned to right node " + assignment
            .getRightMate(node) + " with cost " + assignment.getAssignmentCost(node)
        )

        { node += 1; node }
      }
    } else System.out.println("No solution found.")
  }

  def main(args: Array[String]): Unit = { OrToolsExample.runAssignmentOn4x4Matrix() }

  new NativeDirLoader().load()

  System.loadLibrary("jniortools")
}

class NativeDirLoader {
  private val resourceDir = "/or-tools-mac"

  def load(): Unit = {
    val tempDir = new File(System.getProperty("java.io.tmpdir"))

    tempDir.mkdirs()
    tempDir.deleteOnExit()

    System.setProperty("java.library.path", tempDir.getAbsolutePath)

    val fieldSysPath = classOf[ClassLoader].getDeclaredField("sys_paths")
    fieldSysPath.setAccessible(true)
    fieldSysPath.set(null, null)

    def inputToFile(is: java.io.InputStream, f: java.io.File) {
      val fos = new java.io.FileOutputStream(f)
      fos.write(
        Stream.continually(is.read).takeWhile(-1 != _).map(_.toByte).toArray
      )
      fos.close()
    }
//    -Djava.library.path=/Users/e.zuykin/Projects/beam/src/main/resources/or-tools-mac
    val res = new File(getClass.getResource(resourceDir).toURI)

    res.listFiles().foreach { file =>
      val outputFile = new File(Paths.get(tempDir.getAbsolutePath, file.getName).toString)
      val inputStream = outputFile.getClass.getResource(resourceDir + "/" + file.getName).openStream()
      inputToFile(inputStream, outputFile)
      println(outputFile)
    }

    println()
  }
}
