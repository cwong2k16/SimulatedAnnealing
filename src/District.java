import java.util.HashMap;

public class District {
	private String name;
	private int id;
	// private State state; // TO-DO: implement State
	private int population;
	private HashMap<Integer, Precinct> precincts;
	private HashMap<Integer, Precinct> adjacentPrecincts;
	// private VoteData voteData; // TO-DO: implement VoteData
	
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
	*/
	
	public HashMap<Integer, Precinct> getAdjacentPrecincts(){
		return adjacentPrecincts;
	}
	
	public void addPrecinct(Precinct p){
		precincts.put(p.getId(), p);
	}
}
