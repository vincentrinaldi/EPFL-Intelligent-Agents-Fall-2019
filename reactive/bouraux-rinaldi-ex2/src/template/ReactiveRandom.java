package template;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.Random;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

public class ReactiveRandom implements ReactiveBehavior {
	
	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;
	
	private HashMap<City, List<State>> cityToListOfPossibleStatesMap = new HashMap<City, List<State>>();
	private HashMap<State, List<VehicleAction>> stateToListOfPossibleActionsMap = new HashMap<State, List<VehicleAction>>();
	private HashMap<State, Double> stateToProbabilityOfOccurenceMap = new HashMap<State, Double>();
	private HashMap<VehicleAction, Double> actionToRewardMap = new HashMap<VehicleAction, Double>();
	private HashMap<State, VehicleAction> stateToBestActionMap = new HashMap<State, VehicleAction>();
	private HashMap<State, Double> stateToMaxAccumulatedValueMap = new HashMap<State, Double>();

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;
		
		fillHashMaps(topology, td, agent);
		
		offlineRLAlgo(topology);
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		City vehicleLocation = vehicle.getCurrentCity();

		// The agent can totally ignore an available task with a probability (1 - pPickup)
		if (availableTask == null || random.nextDouble() > pPickup || vehicleLocation != availableTask.pickupCity) {
			State currentState = new State(vehicleLocation, false, null);
			VehicleAction actionToPerform = stateToBestActionMap.get(currentState);
			action = new Move(actionToPerform.getToCity());
		} else {
			State currentState = new State(vehicleLocation, true, availableTask.deliveryCity);
			VehicleAction actionToPerform = stateToBestActionMap.get(currentState);
			if (actionToPerform.getIsPickupAction()) {
				action = new Pickup(availableTask);
			} else {
				action = new Move(actionToPerform.getToCity());
			}
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
	
	// Fill the predefined HashMaps for the Reinforcement Learning Algorithm
	public void fillHashMaps(Topology topology, TaskDistribution td, Agent agent) {
		for (City c1 : topology.cities()) {
			double probabilityOfAnyTaskToOccurInACity = 0;
			List<State> listOfStatesForThisCity = new ArrayList<State>(); 
			
			for (City c2 : topology.cities()) {	
				if (c1 != c2) {
					State stateWithTask = new State(c1, true, c2);
					listOfStatesForThisCity.add(stateWithTask);
					
					stateToProbabilityOfOccurenceMap.put(stateWithTask, td.probability(c1, c2));
					probabilityOfAnyTaskToOccurInACity += td.probability(c1, c2);
					
					stateToMaxAccumulatedValueMap.put(stateWithTask, Double.valueOf(-10000));
					
					List<VehicleAction> listOfActionsForThisState = new ArrayList<VehicleAction>();
					
					VehicleAction pickupAction = new VehicleAction(c1, c2, true);
					listOfActionsForThisState.add(pickupAction);
					
					double cost = c1.distanceTo(c2) * agent.vehicles().get(0).costPerKm();
					actionToRewardMap.put(pickupAction, td.reward(c1, c2) - cost);
					
					for (City neighbor : c1.neighbors()) {
						VehicleAction moveAction = new VehicleAction(c1, neighbor, false);
						listOfActionsForThisState.add(moveAction);
					}
					
					stateToListOfPossibleActionsMap.put(stateWithTask, listOfActionsForThisState);
				}
			}
			
			State stateWithoutTask = new State(c1, false, null);
			listOfStatesForThisCity.add(stateWithoutTask);
			
			stateToProbabilityOfOccurenceMap.put(stateWithoutTask, 1 - probabilityOfAnyTaskToOccurInACity);
			
			stateToMaxAccumulatedValueMap.put(stateWithoutTask, Double.valueOf(-10000));
			
			List<VehicleAction> listOfActionsForThisState = new ArrayList<VehicleAction>();
			
			for (City neighbor : c1.neighbors()) {
				VehicleAction moveAction = new VehicleAction(c1, neighbor, false);
				listOfActionsForThisState.add(moveAction);
				
				double cost = c1.distanceTo(neighbor) * agent.vehicles().get(0).costPerKm();
				actionToRewardMap.put(moveAction, Double.valueOf(-cost));
			}
			
			stateToListOfPossibleActionsMap.put(stateWithoutTask, listOfActionsForThisState);
			
			cityToListOfPossibleStatesMap.put(c1, listOfStatesForThisCity);
		}
	}
	
	// Reinforcement Learning Algorithm to execute before launching the simulation
	public void offlineRLAlgo(Topology topology) {
		boolean notGoodEnough = true;
		
		while (notGoodEnough) {
			notGoodEnough = false;
			
			for (City city : topology.cities()) {
				for (State possibleStateOfCity : cityToListOfPossibleStatesMap.get(city)) {
					VehicleAction bestAction = stateToBestActionMap.get(possibleStateOfCity);
					double maxAccumulatedValue = stateToMaxAccumulatedValueMap.get(possibleStateOfCity);
					
					for (VehicleAction possibleActionOfState : stateToListOfPossibleActionsMap.get(possibleStateOfCity)) {
						double accumulatedValue = actionToRewardMap.get(possibleActionOfState);
						
						for (State possibleStateOfDestination : cityToListOfPossibleStatesMap.get(possibleActionOfState.getToCity())) {
							accumulatedValue += this.pPickup * stateToProbabilityOfOccurenceMap.get(possibleStateOfDestination) * stateToMaxAccumulatedValueMap.get(possibleStateOfDestination);
						}
						
						if (maxAccumulatedValue < accumulatedValue) {
							maxAccumulatedValue = accumulatedValue;
							bestAction = possibleActionOfState;
							notGoodEnough = true;
						}
					}
					
					stateToBestActionMap.put(possibleStateOfCity, bestAction);
					stateToMaxAccumulatedValueMap.put(possibleStateOfCity, maxAccumulatedValue);
				}
			}
		}
	}
}
