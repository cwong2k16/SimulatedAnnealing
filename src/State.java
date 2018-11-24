import java.util.HashMap;

public class State {
	private String name;
	private int stateID;
	private HashMap<Integer, District> districts;
	// private VoteData voteData;
	
	public HashMap<Integer, District> getDistricts(){
		return districts;
	}
	
	/*
	public HashMap<Integer, Precinct> getPrecincts(){
		return precincts;
	}
	
	public VoteData getVoteData(){
	return voteData;
	*/
}
