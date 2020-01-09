package beam.sim

import ch.qos.logback.classic.util.ContextInitializer

object RunBeam extends BeamHelper {
  val logbackConfigFile = Option(System.getProperty(ContextInitializer.CONFIG_FILE_PROPERTY))
  if (logbackConfigFile.isEmpty)
    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback.xml")

  def main(args: Array[String]): Unit = {
    println("Test")
    try {
      System.loadLibrary("jniortools")
      //System.load("/Users/haitam/workspace/beam-github/pooling-test/lib/libjniortools.jnilib")
     // System.load("/Users/haitam/workspace/beam-github/pooling-test/lib/libortools.dylib")
    }
    catch {
      case e: UnsatisfiedLinkError =>
        System.err.println("yoooo Native code library failed to load.\n" + e)
    }

    print(beamAsciiArt)

    runBeamUsing(args)
    logger.info("Exiting BEAM")
    System.exit(0)
  }

}
