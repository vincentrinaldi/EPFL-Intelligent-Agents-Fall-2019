import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author Bouraux Léopold, Rinaldi Vincent
 */

public class RabbitsGrassSimulationSpace {
	
	private final Object2DGrid grassSpace;	
	private final Object2DGrid rabbitSpace;
	
	public RabbitsGrassSimulationSpace(int gridDim) {
        grassSpace = new Object2DGrid(gridDim, gridDim);
        rabbitSpace = new Object2DGrid(gridDim, gridDim);

        for (int i = 0; i < gridDim; i++) {
            for (int j = 0; j < gridDim; j++) {
                grassSpace.putObjectAt(i, j, Integer.valueOf(0));
            }
        }
    }
	
	public Object2DGrid getCurrentEnergySpace() {
	    return grassSpace;
	}

	public Object2DGrid getCurrentAgentSpace() {
	    return rabbitSpace;
	}
	
	public boolean isCellOccupied(int x, int y) {
	    boolean cellOccupied = false;
	    if (rabbitSpace.getObjectAt(x, y) != null) cellOccupied = true;
	    return cellOccupied;
	}
	
	public void spreadGrass(int amountGrass) {
        for (int i = 0; i < amountGrass; i++) {
            int x = (int) (Math.random()*(grassSpace.getSizeX()));
            int y = (int) (Math.random()*(grassSpace.getSizeY()));

            int currentGrassVal = getGrassAt(x, y);
            grassSpace.putObjectAt(x, y, Integer.valueOf(currentGrassVal + 1));
        }
    }
	
	public int getGrassAt(int x, int y) {
        int i;
        if (grassSpace.getObjectAt(x, y) != null) {
            i = ((Integer) grassSpace.getObjectAt(x, y)).intValue();
        } else {
            i = 0;
        }
        return i;
    }
	
	public int takeGrassAt(int x, int y) {
	    int grass = getGrassAt(x, y);
	    grassSpace.putObjectAt(x, y, Integer.valueOf(0));
	    return grass;
	}
	
	public int getTotalGrass() {
	    int totalGrass = 0;
	    for (int i = 0; i < grassSpace.getSizeX(); i++) {
	        for (int j = 0; j < grassSpace.getSizeY(); j++) {
	            totalGrass += getGrassAt(i, j);
	        }
	    }
	    return totalGrass;
	}
	
	public RabbitsGrassSimulationAgent getRabbitAt(int x, int y) {
	    RabbitsGrassSimulationAgent agent = null;
	    if (rabbitSpace.getObjectAt(x, y) != null) {
	        agent = (RabbitsGrassSimulationAgent) rabbitSpace.getObjectAt(x,y);
	    }
	    return agent;
	}
	
	public boolean moveRabbitAt(int x, int y, int newX, int newY) {
	    boolean agentMoved = false;
	    if (isCellOccupied(newX, newY) == false) {
	      RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent) rabbitSpace.getObjectAt(x, y);
	      removeRabbitAt(x,y);
	      rgsa.setXY(newX, newY);
	      rabbitSpace.putObjectAt(newX, newY, rgsa);
	      agentMoved = true;
	    }
	    return agentMoved;
	}
	
	public boolean addRabbit(RabbitsGrassSimulationAgent agent) {
	    boolean agentAdded = false;
	    int count = 0;
	    int countLimit = 10 * rabbitSpace.getSizeX() * rabbitSpace.getSizeY();

	    while ((agentAdded == false) && (count < countLimit)) {
	        int x = (int) (Math.random()*(rabbitSpace.getSizeX()));
	        int y = (int) (Math.random()*(rabbitSpace.getSizeY()));
	        if (isCellOccupied(x, y) == false) {
	            rabbitSpace.putObjectAt(x, y, agent);
	            agent.setXY(x,y);
	            agent.setRabbitsGrassSimulationSpace(this);
	            agentAdded = true;
	        }
	        count++;
	    }

	    return agentAdded;
	}
	
	public void removeRabbitAt(int x, int y) {
	    rabbitSpace.putObjectAt(x, y, null);
	}
}
