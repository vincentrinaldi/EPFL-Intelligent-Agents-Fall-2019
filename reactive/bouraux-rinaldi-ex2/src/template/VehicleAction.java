package template;

import logist.topology.Topology.City;

import java.util.Objects;

public class VehicleAction {
	
	private City fromCity;
	private City toCity;
	private boolean isPickupAction;

	public VehicleAction(City fromCity, City toCity, boolean isPickupAction) {
		this.fromCity = fromCity;
		this.toCity = toCity;
		this.isPickupAction = isPickupAction;
	}
	
	public City getFromCity() {
		return fromCity;
	}

	public void setFromCity(City fromCity) {
		this.fromCity = fromCity;
	}

	public City getToCity() {
		return toCity;
	}

	public void setToCity(City toCity) {
		this.toCity = toCity;
	}
	
	public boolean getIsPickupAction() {
		return isPickupAction;
	}

	public void setIsPickupAction(boolean isPickupAction) {
		this.isPickupAction = isPickupAction;
	}
	
	@Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null) return false;
		if (getClass() != o.getClass()) return false;
		
        VehicleAction otherAction = (VehicleAction) o;
        if (otherAction.fromCity == null) {
        	if (fromCity != null) return false;
        } else {
        	if (!otherAction.fromCity.equals(fromCity)) return false;
        }
        if (otherAction.toCity == null) {
        	if (toCity != null) return false;
        } else {
        	if (!otherAction.toCity.equals(toCity)) return false;
        }
        if (otherAction.isPickupAction != isPickupAction) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromCity, toCity, isPickupAction);
    }
}
