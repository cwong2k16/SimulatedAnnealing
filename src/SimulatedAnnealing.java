import java.util.List;

public class SimulatedAnnealing {
	private double max = 1.0;
	private double min = 0.0001;
	private double alpha = 0.9;
	private List<Precinct> p;
	private float currCost; // or "old" cost
	
	public SimulatedAnnealing(List<Precinct> p){
		this.p = p;
		
		/* Start off the simulated anneaing algorithm with a randomly chosen precinct */
		Precinct prec = selectRandomPrecinct(this.p);
		
		/* Calculate the current cost of precinct by calling the calculateObjFunc */
		currCost = calculateObjFunc(prec);
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
	
	/* Main logic of the algorithm */
	public boolean run(){
		while(max > min){
			int i = 1;
			while(i<=100){
				Precinct newSol = selectRandomPrecinct(p);
				i++;
			}
			max *= alpha;
		}
	}
}
