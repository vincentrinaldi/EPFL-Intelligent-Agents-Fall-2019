package template;

/* import table */
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.plan.Action;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR, NAIVE }
	
	/* Environment */
	Topology topology;
	TaskDistribution td;

	/* the properties of the agent */
	Agent agent;
	
	private int loop = 0; // Loop counter for experimental results

	/* the planning class */
	Algorithm algorithm;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		// initialize the planner
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// ...
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;
		
		System.out.println("Running " + algorithm.name() + " algorithm...");
		long startTime = System.currentTimeMillis();

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = aStarPlan(vehicle, tasks);
			break;
		case BFS:
			// ...
			plan = bfsPlan(vehicle, tasks);
			break;
		case NAIVE:
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}
		
		long endTime = System.currentTimeMillis();
		System.out.println("Computation done: " + (endTime - startTime) + "ms\n");

		return plan;
	}
	
	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City currCity = vehicle.getCurrentCity();
		Plan plan = new Plan(currCity);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : currCity.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			currCity = task.deliveryCity;
		}
		
		return plan;
	}
	
	private Plan bfsPlan(Vehicle vehicle, TaskSet tasks) {
		City currCity = vehicle.getCurrentCity();

		LinkedList<State> queueStates = new LinkedList<State>();
		ArrayList<State> checkedStates = new ArrayList<State>();

		State initState = new State(currCity , tasks, vehicle.getCurrentTasks(), vehicle.capacity(), new ArrayList<Action>(), 0);
		queueStates.add(initState);

		while (!queueStates.isEmpty()) {
			loop++;
			State state = queueStates.poll();
			if(state.isFinalState()) {
				Plan plan = new Plan(currCity , state.getActions());
				System.out.println("Total distance: " + plan.totalDistance());
				System.out.println("Total iterations: " + loop);
				return plan;
			}
			
			if(!checkedStates.contains(state)) {
				checkedStates.add(state);
				List<State> nextStates = state.nextStates(vehicle);
				queueStates.addAll(nextStates);
			}

		}
		throw new IllegalStateException("Failure");
	}
	
	private Plan aStarPlan(Vehicle vehicle, TaskSet tasks) {
		City currCity = vehicle.getCurrentCity();

		PriorityQueue<ComparableState> queueStates = new PriorityQueue<ComparableState>();
		HashSet<State> checkedStates = new HashSet<State>();

		State initState = new State(currCity, tasks, vehicle.getCurrentTasks(), vehicle.capacity(), new ArrayList<Action>(), 0);
		queueStates.add(new ComparableState(initState, 0));

		while (!queueStates.isEmpty()) {
			loop++;
			ComparableState node = queueStates.poll();
			State state = node.state;
			if (state.isFinalState()) {
				Plan plan = new Plan(currCity , state.getActions());
				System.out.println("Total distance: " + plan.totalDistance());
				System.out.println("Total iterations: " + loop);
				return plan;
			}

			if(!checkedStates.contains(state)){
				checkedStates.add(state);
				List<ComparableState> nextComparableStates = node.nextStates_(vehicle);
				Collections.sort(nextComparableStates);
				queueStates.addAll(nextComparableStates);
			}
		}
		throw new IllegalStateException("Failure");
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}
}
