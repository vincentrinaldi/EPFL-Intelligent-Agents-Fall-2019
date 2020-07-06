package template;

import logist.simulation.Vehicle;
import logist.task.Task;

import java.util.ArrayList;
import java.util.List;

public class ComparableState implements Comparable<ComparableState> {
	
    private double h;
    State state;

    public ComparableState(State state, double heuristicCost) {
        this.state = state;
        this.h = heuristicCost;
    }

    public List<ComparableState> nextStates_(Vehicle v) {
        List<State> nextStates = state.nextStates(v);
        List<ComparableState> nextStates_ = new ArrayList<ComparableState>();

        for (State nextState: nextStates) {
            double cost = state.getCurrentCost() + v.costPerKm() * state.getCurrentCity().distanceTo(nextState.getCurrentCity());
            nextState.setCurrentCost(cost);
            double heuristicCost = heuristicCost();
            nextStates_.add(new ComparableState(nextState, heuristicCost));
        }
        
        return nextStates_;
    }

    public double heuristicCost() {
        double costHeuristic;
        double max = 0;
        
        if (!state.isFinalState()) {
            for (Task task : state.getNotPickedTasks()) {
                costHeuristic = state.getCurrentCity().distanceTo(task.pickupCity) + task.pickupCity.distanceTo(task.deliveryCity);
                if (max < costHeuristic) {
                    max = costHeuristic;
                }
            }
            for (Task task: state.getDeliveringTasks()) {
                costHeuristic = state.getCurrentCity().distanceTo(task.deliveryCity);
                if (max < costHeuristic) {
                    max = costHeuristic;
                }
            }
        }
        
        return max;
    }

    @Override
    public int compareTo(ComparableState other) {
        return Double.compare(this.getF(), other.getF());
    }

    public double getF() {
        return h + state.getCurrentCost();
    }
}
