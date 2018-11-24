import java.util.List;

public class SimulatedAnnealing {
	private double max = 1.0;
	private double min = 0.0001;
	private double alpha = 0.9;
	private Precinct p;
	private float oldCost;
	
	public SimulatedAnnealing(Precinct p){
		this.p = p;
		this.oldCost = calculateObjFunc(this.p);
	}
	
	private float calculateObjFunc(Precinct p) {
		/* Dummy solution for now... */
		return this.p.getCost();
	}

	public Precinct selectRandomPrecinct(List<Precinct> p){
		/* Generate a random index according to size of precinct list */
		int rand = (int)(Math.random() * p.size());
		
		/* Return a precinct from the list according to the random index */
		return p.get(rand);
	}
	
	public boolean run(){
		while(max > min){
			int i = 1;
			while(i<=100){
				int newSol = selectRandomPrecinct(p);
				i++;
			}
			max *= alpha;
		}
	}
}
