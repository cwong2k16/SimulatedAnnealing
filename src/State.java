import java.util.HashMap;

public class State {
	private String name;
	private StateID stateID;
	private HashMap<Integer, District> districts;
	// private VoteData voteData;
	
	public State(String name, StateID stateID, HashMap<Integer, District> districts){
		this.name = name;
		this.stateID = stateID;
		this.districts = districts;
	}
	
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
