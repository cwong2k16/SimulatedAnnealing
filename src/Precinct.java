
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
	
	public float getCost(){
		return this.cost;
	}
}
