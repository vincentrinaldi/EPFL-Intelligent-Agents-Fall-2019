package template;

import logist.topology.Topology.City;

import java.util.Objects;

public class State {
	
	private City currentCity;
	private boolean hasTask;
	private City destinationCityOfTask;

	public State(City currentCity, boolean hasTask, City destinationCityOfTask) {
		this.currentCity = currentCity;
		this.hasTask = hasTask;
		this.destinationCityOfTask = destinationCityOfTask;
	}
	
	public City getCurrentCity() {
		return currentCity;
	}

	public void setCurrentCity(City currentCity) {
		this.currentCity = currentCity;
	}
	
	public boolean getHasTask() {
		return hasTask;
	}

	public void setHasTask(boolean hasTask) {
		this.hasTask = hasTask;
	}

	public City getDestinationCityOfTask() {
		return destinationCityOfTask;
	}

	public void setDestinationCityOfTask(City destinationCityOfTask) {
		this.destinationCityOfTask = destinationCityOfTask;
	}
	
	@Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null) return false;
		if (getClass() != o.getClass()) return false;
		
        State otherState = (State) o;
        if (otherState.currentCity == null) {
        	if (currentCity != null) return false;
        } else {
        	if (!otherState.currentCity.equals(currentCity)) return false;
        }
        if (otherState.hasTask != hasTask) return false;
        if (otherState.destinationCityOfTask == null) {
        	if (destinationCityOfTask != null) return false;
        } else {
        	if (!otherState.destinationCityOfTask.equals(destinationCityOfTask)) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentCity, hasTask, destinationCityOfTask);
    }
}
