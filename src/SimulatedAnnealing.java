import java.util.List;

public class SimulatedAnnealing {
	private double max = 1.0;
	private double min = 0.0001;
	private double alpha = 0.9;
	private List<Precinct> p;
	private Precinct currSol;
	private double currCost; // or "old" cost
	
	public SimulatedAnnealing(List<Precinct> p){
		this.p = p;
		
		/* Start off the simulated annealing algorithm with a randomly chosen precinct */
		currSol = selectRandomPrecinct(this.p);
		
		/* Calculate the current cost of precinct by calling the calculateObjFunc */
		currCost = calculateObjFunc(currSol);
	}
	
	private float calculateObjFunc(Precinct p) {
		/* Dummy solution for now... */
		/* getCost() should be based on compactness, population, partisan fairness, etc. */
		/* along with user's weights */
		return p.getCost();
	}

	public Precinct selectRandomPrecinct(List<Precinct> p){
		/* Generate a random index according to size of precinct list */
		int rand = (int)(Math.random() * p.size()-1);
		
		/* Return a precinct from the list according to the random index */
		return p.get(rand);
	}
	
	public Precinct getNeighbor(Precinct p){
		/* getNeighbors returns an array of neighbor IDs */
		int[] neighbors = p.getNeighbors();
		
		/* Get a random neighbor ID */
		int randNeighborId = (int)(Math.random() * neighbors.length-1);
		int neighborNum = neighbors[randNeighborId];
		
		/* Stream API to return a random neighboring precinct according to the neighborID */
		Precinct prec = this.p.stream()
				  .filter((precinct) -> precinct.getId() == neighborNum)
				  .findFirst()
				  .orElse(null);
		
		/* return the randomly generated neighbor precinct */
		return prec;
	}
	
	/* Acceptance Probability formula: e * (new - old)/max */
	public double acceptanceProbability(double oldCost, double newCost, double max){
		double formula = Math.E * (newCost - oldCost)/max;
		return max;
	}
	
	/* Main logic of the algorithm */
	/* Reference: http://katrinaeg.com/simulated-annealing.html */
	public SAResult run(){
		while(max > min){
			int i = 1;
			while(i<=100){
				Precinct newSol = getNeighbor(currSol);
				double newCost = calculateObjFunc(newSol);
				double ac = acceptanceProbability(currCost, newCost, max);
				if(ac > Math.random()){
					currSol = newSol;
					currCost = newCost; 
				}
				i++;
			}
			max *= alpha;
		}
		SAResult res = new SAResult(currSol, currCost);
		return res;
	}
}
