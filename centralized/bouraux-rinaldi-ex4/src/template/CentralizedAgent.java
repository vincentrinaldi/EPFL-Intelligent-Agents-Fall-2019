package template;

//the list of imports
import java.util.List;

import logist.LogistSettings;

import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.io.File;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public class CentralizedAgent implements CentralizedBehavior {
	
	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private long timeout_setup;
	private long timeout_plan;

	/** Setup method */
	@Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
	}


	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
	    long timeStart = System.currentTimeMillis();

	    // Compute an initial plan randomly
	    CSP bestPlan = CSP.selectInitialPlan(vehicles, tasks);
	    CSP currPlan = bestPlan;

	    // Set used to compare the solution with a non-optimal solution already calculated before.
	    Set<CSP> alreadyComputedNotOptimalPlans = new HashSet<CSP>();
	    alreadyComputedNotOptimalPlans.add(bestPlan);
	
	    while (System.currentTimeMillis() - timeStart < timeout_plan * 0.9) {
			assert currPlan != null;

			// Find neighbour solutions
			Set<CSP> neighbours = currPlan.chooseNeighbours();
	        neighbours.removeAll(alreadyComputedNotOptimalPlans);

	        // Choose the one among all neighbours
	        CSP newPlan = CSP.localChoice(neighbours, currPlan);

	        if (newPlan != null) {
	            alreadyComputedNotOptimalPlans.add(newPlan);
	            currPlan = newPlan;
	            neighbours.clear();

	            // Check if new solution is the best one encountered until then
				if (currPlan.getTotalCost() < bestPlan.getTotalCost()) {
	                bestPlan = currPlan;
	                System.out.println("New minima found : "+ bestPlan.getTotalCost());
	            }
	        }
	    }

		assert bestPlan != null;

	    // Convert a CSP into a list of Logist.Plan
		List<Plan> plans = bestPlan.computeAllLogistPlans();
	
	    long timeEnd = System.currentTimeMillis();
	    long duration = timeEnd - timeStart;
	    System.out.println("Plan generation: " + duration + "ms   -   best cost found: " + bestPlan.getTotalCost());
	    
	    return plans;
	}
}
