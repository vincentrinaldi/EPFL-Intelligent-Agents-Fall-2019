package template;

import logist.task.Task;

import java.util.Objects;

public class TaskAction {
	private Task task;
    private boolean isPickup;
    
    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public boolean getIsPickup() {
        return isPickup;
    }

    public void setIsPickup(boolean isPickup) {
        this.isPickup = isPickup;
    }

    public TaskAction(Task task, boolean isPickup) {
    	this.task = task;
        this.isPickup = isPickup;
    }
    
    @Override
    public boolean equals(Object o) {
    	if (o == this) return true;
        if (o == null) return false;
		if (getClass() != o.getClass()) return false;
		
        TaskAction otherTaskAction = (TaskAction) o;
        if (otherTaskAction.task == null) {
        	if (task != null) return false;
        } else {
        	if (!otherTaskAction.task.equals(task)) return false;
        }
        if (otherTaskAction.isPickup != isPickup) return false;
        
        return true;
    }

    @Override
    public int hashCode() {
    	return Objects.hash(task, isPickup);
    }

    @Override
    public String toString() {
        return "[task=(" + task.pickupCity + ", "+ task.deliveryCity + "), isPickup=" + isPickup + "]";
    }
}
