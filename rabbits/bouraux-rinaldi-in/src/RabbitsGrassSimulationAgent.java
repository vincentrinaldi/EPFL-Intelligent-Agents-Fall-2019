import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;

import java.awt.Color;

/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author Bouraux Léopold, Rinaldi Vincent
 */

public class RabbitsGrassSimulationAgent implements Drawable {
	
	private int x;
	private int y;
	private int vX;
	private int vY;
	private int energy;
	
	private static int IDNumber = 0;
	private int ID;
	
	private RabbitsGrassSimulationSpace rgsSpace;
	
	public RabbitsGrassSimulationAgent(int initEnergy) {
	    x = -1;
	    y = -1;
	    setVxVy();
	    energy = initEnergy;
	    
	    IDNumber++;
	    ID = IDNumber;
	}

	public void draw(SimGraphics arg0) {
		// TODO Auto-generated method stub		
		if (energy >= 10) {
			arg0.drawFastRoundRect(Color.white);
		} else {
			arg0.drawFastRoundRect(Color.gray);
		}
	}
	
	public void step() {
		setVxVy();
		int newX = x + vX;
		int newY = y + vY;

		Object2DGrid grid = rgsSpace.getCurrentAgentSpace();
		newX = (newX + grid.getSizeX()) % grid.getSizeX();
		newY = (newY + grid.getSizeY()) % grid.getSizeY();

		if (tryMove(newX, newY)) {
			int grass = rgsSpace.takeGrassAt(x, y);
			energy += grass;
		}
		
		energy--;
	}

	private boolean tryMove(int newX, int newY) {
		return rgsSpace.moveRabbitAt(x, y, newX, newY);
	}

	private void setVxVy() {
	    this.vX = 0;
	    this.vY = 0;
	    
	    int nextMove = (int) Math.floor(Math.random() * 4);
	    if (nextMove == 0) {
	        this.vX = -1;
	    } else if (nextMove == 1) {
	    	this.vX = 1;
	    } else if (nextMove == 2) {
	    	this.vY = -1;
	    } else {
	    	this.vY = 1;
	    }
	}

	public String getID() {
	    return "A-" + ID;
	}
	
	public int getX() {
		// TODO Auto-generated method stub
		return x;
	}

	public int getY() {
		// TODO Auto-generated method stub
		return y;
	}
	
	public void setXY(int newX, int newY) {
	    this.x = newX;
	    this.y = newY;
	}
	
	public int getEnergy() {
	    return energy;
	}
	  
	public void setEnergy(int energy) {
		this.energy = energy;
	}
	
	public void setRabbitsGrassSimulationSpace(RabbitsGrassSimulationSpace rgss) {
	    this.rgsSpace = rgss;
	}
	
	public void report(){
	    System.out.println(getID() + " at " + x + ", " + y + " has " + getEnergy() + " energy.");
	}
}
