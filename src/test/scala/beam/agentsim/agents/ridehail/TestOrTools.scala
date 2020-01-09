package beam.agentsim.agents.ridehail

import beam.sim.BeamHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.google.ortools.linearsolver.MPConstraint
import com.google.ortools.linearsolver.MPObjective
import com.google.ortools.linearsolver.MPSolver
import com.google.ortools.linearsolver.MPSolver.OptimizationProblemType
import com.google.ortools.linearsolver.MPVariable
import org.scalatest.{Matchers, WordSpecLike}

class TestOrTools extends WordSpecLike with Matchers with BeamHelper {

  import com.google.ortools.linearsolver.MPObjective
  import com.google.ortools.linearsolver.MPSolver
  import com.google.ortools.linearsolver.MPSolver.OptimizationProblemType
  import com.google.ortools.linearsolver.MPVariable
  import org.junit.Test
  import org.slf4j.Logger
  import org.slf4j.LoggerFactory

  private val LOGGER = LoggerFactory.getLogger(classOf[Nothing])

  "Vehicle" must {
    "be correctly initialized" in {
      LOGGER.info("Loading native library jniortools.")
      System.loadLibrary("jniortools")
      LOGGER.info("Running the stuff.")
      val solver = new MPSolver("LinearExample", OptimizationProblemType.GLOP_LINEAR_PROGRAMMING)
      val infinity = MPSolver.infinity
      // x is a continuous non-negative variable
      val x = solver.makeNumVar(0.0, infinity, "x")
      // Maximize x
      val objective = solver.objective
      objective.setCoefficient(x, 1)
      objective.setMaximization()
      // x <= 10
      val c = solver.makeConstraint(-infinity, 10.0)
      c.setCoefficient(x, 1)
      solver.solve
      assert(1 == solver.numVariables)
      assert(1 == solver.numConstraints)
      assert(10 == solver.objective.value)
      assert(10 == x.solutionValue)
    }

  }
}
