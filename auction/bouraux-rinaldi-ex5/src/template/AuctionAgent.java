package template;

//the list of imports
import java.util.*;
//import java.util.stream.Collectors;

import logist.LogistPlatform;
import logist.LogistSettings;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
public class AuctionAgent implements AuctionBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;

    private long timeout_plan;
    private long timeout_bid;

    // expected weight on each roads of the world, the set contains only 2 towns
    private Map<Set<City>, Double> expectedWeightOnRoad;

    // expected reward for a task which is picked up in A and delivered in B
    private Map<Tuple<City, City>, Double> expectedReward_A_B;

    private CSP currentCSP;
    private CSP withBidCSP;

    private int roundNb = 0;
    private double currExpectedGain = 0;
    private int tasksWonNb = 0;
    private double totalGains = 0;
    private long totalReward = 0;


    @Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
        // timeouts
        long timeout_setup = LogistPlatform.getSettings().get(LogistSettings.TimeoutKey.SETUP);
        timeout_plan = LogistPlatform.getSettings().get(LogistSettings.TimeoutKey.PLAN);
        timeout_bid = LogistPlatform.getSettings().get(LogistSettings.TimeoutKey.BID);

        System.out.println("Timeout for plan phase: " + timeout_plan+ " ms");
        System.out.println("Timeout for setup phase: " + timeout_setup + " ms");
        System.out.println("Timeout for bid phase: " + timeout_bid+ " ms");


        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;

        fillMaps();

        //only way to creat an empty TaskSet ?
        TaskSet initSet = TaskSet.create(new Task[0]);
        //TaskSet initSet = agent.getTasks();
        currentCSP = CSP.selectInitialPlan(agent.vehicles(), initSet);
    }

    @Override
    public void auctionResult(Task previous, int winner, Long[] bids) {
        if (winner == agent.id()) {
            currentCSP = withBidCSP;
            tasksWonNb++;
            totalReward += previous.reward;
            totalGains = totalReward - currentCSP.getTotalCost();
            currExpectedGain = currentCSP.totalEstimatedGain(expectedWeightOnRoad, roundNb);
        }
        roundNb++;
        withBidCSP = new CSP();
    }

    @Override
    public Long askPrice(Task task) {
        System.out.println("Bidding for task " + task.id);

        long timeStart = System.currentTimeMillis();

        withBidCSP = findBestPlan(currentCSP.addTaskToNearestVehicle(task), timeStart, timeout_bid);

        //this is the cost involved if we win the bid in progress
        double marginalCost = withBidCSP.getTotalCost() - currentCSP.getTotalCost();
        System.out.println("   Marginal cost: " + marginalCost);

        //if large --> high chance to make profit in the future
        double marginalEstimGain = withBidCSP.totalEstimatedGain(expectedWeightOnRoad, roundNb) - currExpectedGain;
        System.out.println("   Marginal estimated gain: " + marginalEstimGain);

        double bidValue = bidStrategy(task, marginalCost, marginalEstimGain);

        System.out.println("   Placing bid for " + bidValue);

        return (long) Math.ceil(bidValue);
    }

    private double miniPathCostNormalised(Task task) {
        //Vehicle cheapestVehicle = agent.vehicles().stream().
        //        sorted(Comparator.comparing(Vehicle::costPerKm)).
        //        collect(Collectors.toList()).get(0);
    	Vehicle cheapestVehicle = agent.vehicles().get(0);
    	int minCostPerKm = agent.vehicles().get(0).costPerKm();
    	for (int i = 1 ; i < agent.vehicles().size() ; i++) {
    		if (agent.vehicles().get(i).costPerKm() < minCostPerKm) {
    			minCostPerKm = agent.vehicles().get(i).costPerKm();
    			cheapestVehicle = agent.vehicles().get(i);
    		}
    	}
    	
        double pathCost = task.pathLength() * cheapestVehicle.costPerKm();
        return pathCost * task.weight / cheapestVehicle.capacity();
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long timeStart = System.currentTimeMillis();

        // when there is no tasks, no need to compute a plan
        if (tasks.isEmpty()) {
            return currentCSP.computeAllLogistPlans();
        }

        // replace old tasks by the new ones in the old CSP
        CSP initialPlan = currentCSP.CSP_NewRound(tasks);

        CSP bestPlan = findBestPlan(initialPlan, timeStart, timeout_plan);

        List<Plan> plans = bestPlan.computeAllLogistPlans();

        long timeEnd = System.currentTimeMillis();
        long duration = timeEnd - timeStart;
        System.out.println("Plan generation: " + duration + "ms   -   best cost found: " + bestPlan.getTotalCost());
        return plans;
    }

    private CSP findBestPlan(CSP initialSolution, long timeStart, long timout) {

        CSP bestPlan = initialSolution;
        CSP currPlan = bestPlan;

        while (System.currentTimeMillis() - timeStart < timout * 0.95) {

            Set<CSP> neighbours = currPlan.chooseNeighbours();

            // Choose the one among all neighbours
            CSP newPlan = CSP.localChoice(neighbours, currPlan);

            if (newPlan != null) {
                currPlan = newPlan;
                neighbours.clear();

                if (currPlan.getTotalCost() < bestPlan.getTotalCost()) {
                    bestPlan = currPlan;
                    System.out.println("New minima found : "+ bestPlan.getTotalCost());
                }
            }
        }
        return currPlan;
    }

    private void fillMaps(){
        this.expectedWeightOnRoad = new HashMap<Set<City>, Double>();
        this.expectedReward_A_B = new HashMap<Tuple<City, City>, Double>();

        for (City cityA : topology.cities()) {
            for (City cityB : topology.cities()) {
                LinkedList<City> path = new LinkedList<City>(cityA.pathTo(cityB));
                while(!path.isEmpty()) {
                    City currCity = path.pollFirst();
                    if(path.isEmpty())
                        break;
                    City nextCity = path.getFirst();
                    Set<City> set = new HashSet<City>(Arrays.asList(currCity, nextCity));
                    double weight=0.0;
                    if(expectedWeightOnRoad.containsKey(set))
                        weight = expectedWeightOnRoad.get(set);
                    double newWeight = distribution.probability(currCity, nextCity) *
                            distribution.weight(currCity, nextCity);
                    expectedWeightOnRoad.put(set, weight+newWeight);
                }
                if(cityA.equals(cityB))
                    continue;
                double expectReward = distribution.probability(cityA, cityB) *
                        distribution.reward(cityA, cityB);
                expectedReward_A_B.put(new Tuple<City, City>(cityA, cityB), expectReward);
            }
        }
    }

    private double bidStrategy(Task task, double marginalCost, double marginalEstimGain) {
        double x;
        int deficitRounds = 8;
        int deadlineToProfit = 14;

        // 1: Take as much as tasks as possible
        if (roundNb + tasksWonNb < deficitRounds) {
            x = Math.max(
                    miniPathCostNormalised(task),
                    marginalCost - marginalEstimGain * 0.1);
        }
        // 2: Goal is to become profitable
        else if (totalGains < 0) {
            int den = Math.max(1, deadlineToProfit - roundNb);
            x = Math.max(
                    miniPathCostNormalised(task),
                    marginalCost - totalGains/ den);
        }
        // 3: Once profitable, bids are determined by the utility of the auctioned task
        else {
            x = Math.max(
                    miniPathCostNormalised(task),
                    marginalCost + Math.max(1, 1 - marginalEstimGain * 0.2));
        }
        return x;
    }

}
