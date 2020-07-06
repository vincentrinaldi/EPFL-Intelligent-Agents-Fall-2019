package template;

import java.util.ArrayList;
import java.util.List;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Delivery;
import logist.plan.Action.Pickup;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

class State {

	private City currentCity;
	private TaskSet notPickedTasks;
	private TaskSet deliveringTasks;
	private double remainingCapacity;
	private ArrayList<Action> actions; // Represents all the actions that led the agent to this state
	private double currentCost;

	public State(City currentCity, TaskSet notPickedTasks, TaskSet deliveringTasks, double remainingCapacity, ArrayList<Action> actions, double currentCost) {
		this.currentCity = currentCity;
		this.notPickedTasks = notPickedTasks;
		this.deliveringTasks = deliveringTasks;
		this.remainingCapacity = remainingCapacity;
		this.actions = actions;
		this.currentCost = currentCost;
	}

	public State(State copy) {
		this(copy.getCurrentCity(), copy.getNotPickedTasks().clone(), copy.getDeliveringTasks().clone(),
				copy.getRemainingCapacity(), new ArrayList<Action>(copy.getActions()), copy.getCurrentCost());
	}

	public boolean isFinalState() {
		return notPickedTasks.isEmpty() && deliveringTasks.isEmpty();
	}

	public List<State> nextStates(Vehicle v){
		List<State> nextStates = new ArrayList<State>();
		
		if (isFinalState()) {
			return nextStates;
		}
		
		for (Task task: deliveringTasks) {
			// Time to deliver
			if (task.deliveryCity.equals(currentCity)) {
				State next = new State(this);
				TaskSet nextDeliveringTasks = next.getDeliveringTasks();
				nextDeliveringTasks.remove(task);
				ArrayList<Action> nextActions = next.getActions();
				nextActions.add(new Delivery(task));
				
				next.setDeliveringTasks(nextDeliveringTasks);
				next.setRemainingCapacity(remainingCapacity + task.weight);
				next.setActions(nextActions);
				nextStates.add(next);

			// Go to deliver destination by moving to nextCity
			} else {
				State next = new State(this);
				City nextCity = currentCity.pathTo(task.deliveryCity).get(0);
				ArrayList<Action> nextActions = new ArrayList<Action>(this.actions);
				nextActions.add(new Move(nextCity));

				next.setCurrentCost(currentCost + v.costPerKm() * currentCity.distanceTo(nextCity));
				next.setActions(nextActions);
				next.setCurrentCity(nextCity);
				nextStates.add(next);
			}
		}

		for (Task task: notPickedTasks) {
			if (remainingCapacity >= task.weight) {
				// Time to pickup
				if (task.pickupCity.equals(currentCity)) {
					State next = new State(this);
					TaskSet nextDelivTasks = next.getDeliveringTasks();
					nextDelivTasks.add(task);
					TaskSet nextNotPickedTasks = next.getNotPickedTasks();
					nextNotPickedTasks.remove(task);
					ArrayList<Action> nextActions = next.getActions();
					nextActions.add(new Pickup(task));

					next.setDeliveringTasks(nextDelivTasks);
					next.setNotPickedTasks(nextNotPickedTasks);
					next.setActions(nextActions);
					next.setRemainingCapacity(remainingCapacity - task.weight);
					nextStates.add(next);
					
				// Go to pickup destination by moving to next City
				} else {
					State next = new State(this);
					City nextCity = currentCity.pathTo(task.pickupCity).get(0);
					ArrayList<Action> nextActions = new ArrayList<Action>(this.actions);
					nextActions.add(new Move(nextCity));
					
					next.setActions(nextActions);
					next.setCurrentCity(nextCity);
					next.setCurrentCost(currentCost + v.costPerKm() * currentCity.distanceTo(nextCity));
					nextStates.add(next);
				}
			}

		}
		
		return nextStates;
	}

	@Override
	public boolean equals(Object obj) {
		if (getClass() != obj.getClass()) {
			return false;
		}
		State o = (State) obj;
		return notPickedTasks.equals(o.getNotPickedTasks()) && 
				deliveringTasks.equals(o.getDeliveringTasks()) &&
				remainingCapacity == o.getRemainingCapacity() &&
				currentCity.equals(o.getCurrentCity());
	}

	@Override
	public int hashCode() {
		int p = 41;
		int result = p *(int)(remainingCapacity);
		if(deliveringTasks != null)
			result += result*p + deliveringTasks.hashCode();
		if(notPickedTasks != null)
			result += result *p + notPickedTasks.hashCode();
		return result;
	}

	public City getCurrentCity() {
		return currentCity;
	}
	
	public void setCurrentCity(City currentCity) {
		this.currentCity = currentCity;
	}

	public TaskSet getNotPickedTasks() {
		return notPickedTasks;
	}
	
	public void setNotPickedTasks(TaskSet notPickedTasks) {
		this.notPickedTasks = notPickedTasks;
	}
	
	public TaskSet getDeliveringTasks() {
		return deliveringTasks;
	}

	public void setDeliveringTasks(TaskSet deliveringTasks) {
		this.deliveringTasks = deliveringTasks;
	}

	public double getRemainingCapacity() {
		return remainingCapacity;
	}
	
	public void setRemainingCapacity(double remainingCapacity) {
		this.remainingCapacity = remainingCapacity;
	}

	public ArrayList<Action> getActions() {
		return actions;
	}
	
	public void setActions(ArrayList<Action> actions) {
		this.actions = actions;
	}

	public double getCurrentCost() {
		return currentCost;
	}

	public void setCurrentCost(double currentCost) {
		this.currentCost = currentCost;
	}
}
