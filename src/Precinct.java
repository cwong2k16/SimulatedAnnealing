
public class Precinct {
	private int id;
	private String name;
	private boolean atBoundary;
	private int[] neighbors;
	private float cost;
	
	public Precinct(int id, String name, boolean atBoundary, int[] neighbors, float cost){
		this.id = id;
		this.name = name;
		this.atBoundary = atBoundary;
		this.neighbors = neighbors;
		this.cost = cost;
	}
	
	public int getId(){
		return this.id;
	}
	
	/* Precinct cost should be based on a formula taking into account user's selected weights and value of
	 * compactness, political fairness, population, etc.
	 */
	public float getCost(){
		return this.cost;
	}
	
	public int[] getNeighbors(){
		return neighbors;
	}
}
