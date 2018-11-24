import java.util.HashMap;
import java.util.List;

public class District {
	private String name;
	private int id;
	private State state;
	private int population;
	private HashMap<Integer, Precinct> precincts;
	private HashMap<Integer, Precinct> adjacentPrecincts;
	private List<Precinct> boundaryPrecincts;
	// private VoteData voteData; // TO-DO: implement VoteData
	
	public District(String name, int id, int population, HashMap<Integer, Precinct> precincts, HashMap<Integer, Precinct> adjacentPrecincts, List<Precinct> boundaryPrecincts){
		this.name = name;
		this.id = id;
		this.population = population;
		this.precincts = precincts;
		this.adjacentPrecincts = adjacentPrecincts;
		this.boundaryPrecincts = boundaryPrecincts;
	}
	
	public District getDistrict(int id){
		return null;
	}
	
	public int getPopulation(){
		return population;
	}
	
	/* Need to implement VoteData and BoundaryData...
	public VoteData getVoteData(){
		return voteData;
	}
	
	public BoundaryData boundaryData(){
		return boundaryData;
	}
	
	public List<Precinct> getBoundaryPrecincts(){
		return boundaryPrecincts;
	}
	*/
	
	public HashMap<Integer, Precinct> getAdjacentPrecincts(){
		return adjacentPrecincts;
	}
	
	public void addPrecinct(Precinct p){
		precincts.put(p.getId(), p);
	}
}
