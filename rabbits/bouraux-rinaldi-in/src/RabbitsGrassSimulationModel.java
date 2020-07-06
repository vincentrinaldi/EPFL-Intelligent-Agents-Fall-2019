import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;

import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.gui.Object2DDisplay;

import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.util.SimUtilities;

import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.Sequence;

import java.awt.Color;
import java.util.ArrayList;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author Bouraux Léopold, Rinaldi Vincent
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {

	private static final int GRID_SIZE = 20;
	private static final int NUM_INIT_RABBITS = 50;
	private static final int NUM_INIT_GRASS = 50;
	private static final int GRASS_GROWTH_RATE = 50;
	private static final int BIRTH_THRESHOLD = 30;
	private static final int RABBIT_INIT_ENERGY = 25;
	private static final int BIRTH_DAMAGE = 10;

	private int gridSize = GRID_SIZE;		   			//Size of grid
	private int numInitRabbits = NUM_INIT_RABBITS; 		//Initial number of rabbits
	private int numInitGrass = NUM_INIT_GRASS;			//Initial number of grass
	private int grassGrowthRate = GRASS_GROWTH_RATE;	//Grass growth rate
	private int birthThreshold = BIRTH_THRESHOLD;		//Rabbit reproduction energy threshold
	private int rabbitInitEnergy = RABBIT_INIT_ENERGY;	//Energy of a new rabbit
	private int birthDamage = BIRTH_DAMAGE;				//Energy lost after giving birth

	private RabbitsGrassSimulationSpace rgsSpace;
	private DisplaySurface displaySurf;
	private Schedule schedule;
	private ArrayList<RabbitsGrassSimulationAgent> rabbitList;

	private OpenSequenceGraph amountOfRabbitsAndGrass;  // Graph of number of rabbits and grass

	public static void main(String[] args) {

		System.out.println("Rabbit skeleton");

		SimInit init = new SimInit();
		RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
		// Do "not" modify the following lines of parsing arguments
		if (args.length == 0) // by default, you don't use parameter file nor batch mode
			init.loadModel(model, "", false);
		else
			init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));

	}

	public void begin() {
		// TODO Auto-generated method stub
		buildModel();
		buildSchedule();
		buildDisplay();
		displaySurf.display();

		amountOfRabbitsAndGrass.display();
	}

	public String[] getInitParam() {
		// TODO Auto-generated method stub
		// Parameters to be set by users via the Repast UI slider bar
		// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want
		String[] params = { "GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold", "RabbitInitEnergy", "BirthDamage"};
		return params;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return "Rabbits Grass Simulation";
	}

	public Schedule getSchedule() {
		// TODO Auto-generated method stub
		return schedule;
	}

	public void setup() {
		// TODO Auto-generated method stub
		rgsSpace = null;
		rabbitList = new ArrayList<RabbitsGrassSimulationAgent>();
		schedule = new Schedule(1);

		if (displaySurf != null) {
			displaySurf.dispose();
		}
		displaySurf = null;
		displaySurf = new DisplaySurface(this, "Rabbits Grass Simulation Window");
		registerDisplaySurface("Rabbits Grass Simulation Window", displaySurf);

		if (amountOfRabbitsAndGrass != null) {
			amountOfRabbitsAndGrass.dispose();
		}
		amountOfRabbitsAndGrass = null;
		amountOfRabbitsAndGrass = new OpenSequenceGraph("Amount Of Rabbits And Grass", this);
		this.registerMediaProducer("Plot", amountOfRabbitsAndGrass);
	}

	public void buildModel() {
		rgsSpace = new RabbitsGrassSimulationSpace(gridSize);
		rgsSpace.spreadGrass(numInitGrass);

		for (int i = 0; i < numInitRabbits; i++) {
			addNewRabbit();
		}
		for (int i = 0; i < rabbitList.size(); i++) {
			RabbitsGrassSimulationAgent rgsa = rabbitList.get(i);
			rgsa.report();
		}
	}

	public void buildSchedule() {
		class RabbitsGrassSimulationStep extends BasicAction {
			public void execute() {
				SimUtilities.shuffle(rabbitList);
				for (int i = 0; i < rabbitList.size(); i++){
					RabbitsGrassSimulationAgent rgsa = rabbitList.get(i);
					rgsa.step();
				}
				rgsSpace.spreadGrass(grassGrowthRate);
				reapDeadRabbits();
				rabbitBirth();
				displaySurf.updateDisplay();
			}
		}
		schedule.scheduleActionBeginning(0, new RabbitsGrassSimulationStep());

		class RabbitsGrassSimulationCountLiving extends BasicAction {
			public void execute(){
				countLivingRabbits();
			}
		}
		schedule.scheduleActionAtInterval(10, new RabbitsGrassSimulationCountLiving());

		class RabbitsGrassSimulationUpdateRabbitsAndGrassInSpace extends BasicAction {
			public void execute() {
				amountOfRabbitsAndGrass.step();
			}
		}
		schedule.scheduleActionAtInterval(10, new RabbitsGrassSimulationUpdateRabbitsAndGrassInSpace());
	}

	public void buildDisplay() {
		ColorMap map = new ColorMap();
		for (int i = 1; i < 16; i++) {
			map.mapColor(i, new Color(0, (int)(i * 8 + 127), 0));
		}
		map.mapColor(0, Color.black);

		Value2DDisplay displayGrass = new Value2DDisplay(rgsSpace.getCurrentEnergySpace(), map);
		Object2DDisplay displayRabbits = new Object2DDisplay(rgsSpace.getCurrentAgentSpace());
		displayRabbits.setObjectList(rabbitList);

		displaySurf.addDisplayableProbeable(displayGrass, "Grass");
		displaySurf.addDisplayableProbeable(displayRabbits, "Rabbit");

		amountOfRabbitsAndGrass.addSequence("Amount Of Rabbits", new TotalRabbitsInSpace(), Color.red);
		amountOfRabbitsAndGrass.addSequence("Amount Of Grass", new TotalGrassInSpace(), Color.green);
	}

	private void addNewRabbit() {
		RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(rabbitInitEnergy);
		rabbitList.add(a);
		rgsSpace.addRabbit(a);
	}

	private void reapDeadRabbits() {
		for (int i = (rabbitList.size() - 1); i >= 0; i--) {
			RabbitsGrassSimulationAgent rabbitAgent = rabbitList.get(i);
			if (rabbitAgent.getEnergy() < 1) {
				rgsSpace.removeRabbitAt(rabbitAgent.getX(), rabbitAgent.getY());
				rabbitList.remove(i);
			}
		}
	}

	private void rabbitBirth() {
		for (int i = (rabbitList.size() - 1); i >= 0; i--) {
			RabbitsGrassSimulationAgent rabbitAgent = rabbitList.get(i);
			int currEnergy = rabbitAgent.getEnergy();
			if (currEnergy >= birthThreshold) {
				int newEnergy;
				if (currEnergy - birthDamage < birthThreshold) {
					newEnergy = currEnergy - birthDamage;
				} else {
					 newEnergy = birthThreshold - 1;
				}
				rabbitAgent.setEnergy(newEnergy);
				addNewRabbit();
			}
		}
	}

	private void countLivingRabbits() {
		System.out.println("Reporting current remaining living rabbits...");
		int livingRabbits = 0;
		for (int i = 0; i < rabbitList.size(); i++) {
			RabbitsGrassSimulationAgent rgsa = rabbitList.get(i);
			if (rgsa.getEnergy() > 0) {
				livingRabbits++;
				rgsa.report();
			}
		}
		System.out.println("Number of current remaining living rabbits is " + livingRabbits + " !");
		System.out.println("Current total amount of grass is " + rgsSpace.getTotalGrass() + ".");
	}

	class TotalRabbitsInSpace implements DataSource, Sequence {
		public Object execute() {
			return Double.valueOf(getSValue());
		}

		public double getSValue() {
			return rabbitList.size();
		}
	}

	class TotalGrassInSpace implements DataSource, Sequence {
		public Object execute() {
			return Double.valueOf(getSValue());
		}

		public double getSValue() {
			return rgsSpace.getTotalGrass();
		}
	}

	public int getGridSize() {
		return gridSize;
	}

	public void setGridSize(int gs) {
		this.gridSize = gs;
	}

	public int getNumInitRabbits() {
		return numInitRabbits;
	}

	public void setNumInitRabbits(int nir) {
		this.numInitRabbits = nir;
	}

	public int getNumInitGrass() {
		return numInitGrass;
	}

	public void setNumInitGrass(int nig) {
		this.numInitGrass = nig;
	}

	public int getGrassGrowthRate() {
		return grassGrowthRate;
	}

	public void setGrassGrowthRate(int ggr) {
		this.grassGrowthRate = ggr;
	}
	
	public int getBirthThreshold() {
		return birthThreshold;
	}

	public void setBirthThreshold(int bt) {
		this.birthThreshold = bt;
	}

	public int getRabbitInitEnergy() {
		return rabbitInitEnergy;
	}

	public void setRabbitInitEnergy(int rie) {
		this.rabbitInitEnergy = rie;
	}
	
	public int getBirthDamage() {
		return birthDamage;
	}

	public void setBirthDamage(int bd) {
		this.birthDamage = bd;
	}
}
