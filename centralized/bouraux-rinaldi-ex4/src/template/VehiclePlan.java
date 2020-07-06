package template;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class VehiclePlan {
	

	private final Vehicle vehicle;
    private final LinkedList<TaskAction> actionList;
    
    /** Initial constructor & copy*/
    public VehiclePlan(Vehicle vehicle) {
        this(vehicle, new LinkedList<TaskAction>());
    }

    public VehiclePlan(Vehicle vehicle, List<TaskAction> actions) {
        this.vehicle = vehicle;
        this.actionList = new LinkedList<TaskAction>(actions);
    }

    /** Getters */
    public LinkedList<TaskAction> getActionList() {
        return actionList;
    }
    
    public Vehicle getVehicle() {
        return vehicle;
    }

    /** Function to modify a Vehicle plan, used to find a first plan and neighbour plans */
    public VehiclePlan appendTask(Task taskToAppend) {
        VehiclePlan vPlan = new VehiclePlan(vehicle, actionList);
        vPlan.actionList.add(new TaskAction(taskToAppend, true));
        vPlan.actionList.add(new TaskAction(taskToAppend, false));
        return vPlan;
    }

    public VehiclePlan removeTask(Task taskToRemove) {
        VehiclePlan vPlan = new VehiclePlan(vehicle, actionList);
        for (TaskAction action : actionList) {
            Task currTask = action.getTask();
            if (currTask.equals(taskToRemove)) {
                vPlan.actionList.remove(action);
            }
        }
        return vPlan;
    }

    public VehiclePlan insertActionAtPosition(TaskAction action, int position) {
        VehiclePlan vPlan = new VehiclePlan(vehicle, actionList);
        vPlan.actionList.add(position, action);
        return vPlan;
    }

    /** Total distance of a vehicle, used to compute its total cost */
    public double getTotalDistance() {
        double distance = 0;
        City currCity = vehicle.getCurrentCity();

        City nextCity;
        for (TaskAction taskAction : actionList) {

            if (taskAction.getIsPickup()) {
                nextCity = taskAction.getTask().pickupCity;
            } else {
                nextCity = taskAction.getTask().deliveryCity;
            }

            if (!currCity.equals(nextCity)) {
                distance += currCity.distanceTo(nextCity);
                currCity = nextCity;
            }
        }
        return distance;
    }

    /** Define the plan for a vehicle through its List of actions */
    public Plan computeLogistPlan(Vehicle vehicle) {
        City currCity = vehicle.getCurrentCity();
        Plan plan = new Plan(currCity);
        
        City nextCity;
        for (TaskAction taskAction: actionList) {
        	if (taskAction.getIsPickup()) {
        		nextCity = taskAction.getTask().pickupCity;
        	} else {
        		nextCity = taskAction.getTask().deliveryCity;
        	}

            if (!currCity.equals(nextCity)) {
                for (City city : currCity.pathTo(nextCity)) {
                    plan.appendMove(city);
                }
                currCity = nextCity;
            }

            if (taskAction.getIsPickup()) {
                plan.appendPickup(taskAction.getTask());
            } else {
                plan.appendDelivery(taskAction.getTask());
            }
        }
        
        return plan;
    }

    /** Override methods */
    @Override
    public boolean equals(Object o) {
    	if (o == this) return true;
        if (o == null) return false;
		if (getClass() != o.getClass()) return false;

        VehiclePlan otherVehiclePlan = (VehiclePlan) o;
        if (otherVehiclePlan.vehicle == null) {
        	if (vehicle != null) return false;
        } else {
        	if (!otherVehiclePlan.vehicle.equals(vehicle)) return false;
        }
        if (otherVehiclePlan.actionList == null) {
        	if (actionList != null) return false;
        } else {
        	if (!otherVehiclePlan.actionList.equals(actionList)) return false;
        }
        if (otherVehiclePlan.getTotalDistance() != getTotalDistance()) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vehicle, actionList, getTotalDistance());
    }
    
    @Override
    public String toString() {
        return "VehiclePlan [vehicle=" + vehicle + ", actionList=" + actionList + ", distance=" + getTotalDistance() + "]";
    }
}
