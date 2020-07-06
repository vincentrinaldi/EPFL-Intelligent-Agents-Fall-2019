package template;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;

import java.util.*;

public class CSP {

    private final static double round_error = 0.001;

    private final Map<Vehicle, VehiclePlan> vehiclePlanMap;
    private final List<Vehicle> vehicles;

    private CSP(Map<Vehicle, VehiclePlan> vehiclePlanMap, List<Vehicle> vehicles) {
        this.vehiclePlanMap = vehiclePlanMap;
        this.vehicles = vehicles;
    }

    /** Get total cost for an agent and several vehicles */
    public double getTotalCost() {
        double cost = 0;
        for (Vehicle vehicle : vehicles) {
                cost += vehicle.costPerKm() * vehiclePlanMap.get(vehicle).getTotalDistance();
        }
        return cost;
    }

    /**  Compute Logist.Plan for each vehicles */
    public List<Plan> computeAllLogistPlans() {
        List<Plan> plans = new ArrayList<Plan>();
        for (Vehicle vehicle: vehicles) {
            plans.add(vehiclePlanMap.get(vehicle).computeLogistPlan(vehicle));
        }
        return plans;
    }

    /** Compute a plan by assigning all task to the vehicle with the largest capacity */
    public static CSP selectInitialPlan(List<Vehicle> vehicles, TaskSet tasks) {

        //initialSolution by giving an equal number of tasks to each vehicle
        /*

        Map<Vehicle, VehiclePlan> vPlanMap = new HashMap<Vehicle, VehiclePlan>();
        Iterator<Task> ta = tasks.iterator();
        double taskPerVehicle = Math.ceil(tasks.size() / (double) vehicles.size());

        int i = 1;
        int vehicleIndex = 0;
        Vehicle vehicle = vehicles.get(0);
        while(ta.hasNext()){
            Task task = ta.next();
            if(i%taskPerVehicle==0){
                vehicleIndex++;
                vehicle = vehicles.get(vehicleIndex);
            }
            if (task.weight < vehicle.capacity()) {
                vPlanMap.put(vehicle, vPlanMap.getOrDefault(vehicle, new VehiclePlan(vehicle)).appendTask(task));
            }
            else {
                for (Vehicle other : vehicles) {
                    if (task.weight < other.capacity()){
                        vPlanMap.put(other, vPlanMap.getOrDefault(other, new VehiclePlan(other)).appendTask(task));
                        break;
                    }
                    else {
                        System.out.println("Unsolvable problem since biggest task cannot fit in biggest vehicle");
                        return null;
                    }
                }
            }
            i++;
        }*/

        //initialSolution by giving all tasks to biggest vehicle
        Map<Vehicle, VehiclePlan> vPlanMap = new HashMap<Vehicle, VehiclePlan>();
        Vehicle bestVehicle = vehicles.get(0);
        for (Vehicle vehicle:vehicles) {
            if (vehicle.capacity() > bestVehicle.capacity()) {
                bestVehicle = vehicle;
            }
            vPlanMap.put(vehicle, new VehiclePlan(vehicle));
        }
        
        for (Task task:tasks) {
            if (task.weight > bestVehicle.capacity()) {
                System.out.println("Unsolvable problem since biggest task cannot fit in biggest vehicle");
                return null;
            } else {
                vPlanMap.put(bestVehicle, vPlanMap.get(bestVehicle).appendTask(task));
            }
        }

        return new CSP(vPlanMap, vehicles);
    }

    /** Compute neighbouring solutions */
    public Set<CSP> chooseNeighbours() {
        Set<CSP> neighboursPlans = new HashSet<CSP>();

        // Choose a random vehicle such that he has available tasks to do
        Vehicle vehicle_i;
        int index;
        do {
            index = new Random().nextInt(vehicles.size());
            vehicle_i = vehicles.get(index);
        } while (vehiclePlanMap.get(vehicle_i).getActionList().isEmpty());

        VehiclePlan vPlan_i = vehiclePlanMap.get(vehicle_i);

        // New plan from giving the first task to another vehicle
        for (Vehicle vehicle_j: vehicles) {
            if (!vehicle_j.equals(vehicle_i)) {
                VehiclePlan vPlan_j = vehiclePlanMap.get(vehicle_j);
                neighboursPlans.add(changingVehicle(vPlan_i, vPlan_j));
            }
        }

        // All plans with reordered tasks for vehicle i
        Set<CSP> cspList = changingTaskOrder(vPlan_i);
        neighboursPlans.addAll(cspList);
        return neighboursPlans;
    }

    /** Take the first task from the tasks of one vehicle, give it to another*/
    private CSP changingVehicle(VehiclePlan vPlan1, VehiclePlan vPlan2) {
        Map<Vehicle, VehiclePlan> newVPlanMap = new HashMap<Vehicle, VehiclePlan>(vehiclePlanMap);

        Task firstTask = vPlan1.getActionList().get(0).getTask();
        if (firstTask.weight > vPlan2.getVehicle().capacity()) {
            return new CSP(newVPlanMap, vehicles);
        }
        
        VehiclePlan vPlan1Without = vPlan1.removeTask(firstTask);
        VehiclePlan vPlan2With = vPlan2.appendTask(firstTask);

        newVPlanMap.put(vPlan1Without.getVehicle(), vPlan1Without);
        newVPlanMap.put(vPlan2With.getVehicle(), vPlan2With);

        return new CSP(newVPlanMap, vehicles);
    }

    /** Set of CSP with all possible combination of swapped tasks */
    public Set<CSP> changingTaskOrder(VehiclePlan vPlan) {
        Set<CSP> cspSet = new HashSet<CSP>();
        List<TaskAction> actions = vPlan.getActionList();
        for (TaskAction pickup : actions) {
            if (!pickup.getIsPickup()) continue;

            // Remove the action only if it's a pickup one
            Task pickupTaskToRemove = pickup.getTask();
            VehiclePlan vPlan1Without = vPlan.removeTask(pickupTaskToRemove);

            // Create the corresponding delivery action linked to the removed pickUp one
            TaskAction delivery = new TaskAction(pickupTaskToRemove, false);

            for (int id1 = 0; id1 < vPlan1Without.getActionList().size() + 1; id1++) {
                // Insert the pickup on every id1
                VehiclePlan vPlan2With = vPlan1Without.insertActionAtPosition(pickup, id1);

                for (int id2 = id1 + 1; id2 <= vPlan2With.getActionList().size(); id2++) {
                    // Insert the delivery on every id2 > id1
                    VehiclePlan vPlanFinal = vPlan2With.insertActionAtPosition(delivery, id2);

                    // Check whether the planCapacity is fulfilled (we check it only one time at the end)
                    if (hasEnoughCapacity(vPlanFinal)) {
                        Map<Vehicle, VehiclePlan> newVPlanMap = new HashMap<Vehicle, VehiclePlan>(vehiclePlanMap);
                        newVPlanMap.put(vPlanFinal.getVehicle(), vPlanFinal);
                        cspSet.add(new CSP(newVPlanMap, vehicles));
                    }
                }
            }
        }
        return cspSet;
    }

    /** Compute if the vehicle plan fulfilled capacity constraint throughout the vehicle's journey */
    public boolean hasEnoughCapacity(VehiclePlan vehiclePlan) {
        int vehicleLoad = 0;

        for (TaskAction action : vehiclePlan.getActionList()) {
            vehicleLoad = action.getIsPickup() ?
                    vehicleLoad + action.getTask().weight:
                    vehicleLoad - action.getTask().weight;

            if (vehicleLoad > vehiclePlan.getVehicle().capacity()) {
                return false;
            }
        }
        return true;
    }

    /** Stochastic chose on the next CSP */
    public static CSP localChoice(Set<CSP> neighbours, CSP old) {
      ArrayList<CSP> bests = new ArrayList<CSP>();
        //double bestCost = neighbours.stream().findFirst().orElseThrow(NullPointerException::new).getTotalCost();
      	double bestCost = neighbours.stream().findFirst().orElseThrow().getTotalCost();

        for (CSP csp : neighbours) {
            double cost = csp.getTotalCost();
            // Dealing with computation round error
            if (cost < bestCost + round_error && cost > bestCost - round_error) {
                bests.add(csp);
            } else if (bestCost > cost) {
                // Guarantee to have computed new best cost
                bestCost = cost;
                bests.clear();
                bests.add(csp);
            }
        }
        Collections.shuffle(bests);

        // New best optimal CSP
        if(old.getTotalCost() > bestCost) {
            return bests.get(0);
        }
        // Old CSP returned
        if (Math.random() > 0.5) {
            return old;
        }
        // Best of neighbours solutions
        return bests.get(0);
    }

    /** Override methods */
    @Override
    public boolean equals(Object o) {
    	if (o == this) return true;
        if (o == null) return false;
		if (getClass() != o.getClass()) return false;

        CSP otherCSP = (CSP) o;
        if (otherCSP.vehiclePlanMap == null) {
        	if (vehiclePlanMap != null) return false;
        } else {
        	if (!otherCSP.vehiclePlanMap.equals(vehiclePlanMap)) return false;
        }
        if (otherCSP.vehicles == null) {
        	if (vehicles != null) return false;
        } else {
        	if (!otherCSP.vehicles.equals(vehicles)) return false;
        }
        if (Math.abs(otherCSP.getTotalCost() - getTotalCost()) > round_error) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vehiclePlanMap, vehicles, getTotalCost());
    }
    
    @Override
    public String toString() {
        return "CSP [vehiclePlanMap=" + vehiclePlanMap + ", vehicles=" + vehicles + 
        		", cost=" + getTotalCost() + "]";
    }
}
