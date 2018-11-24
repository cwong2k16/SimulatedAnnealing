import java.util.HashMap;

public class VoteData {
	private int voters;
	private Party party;
	private int votingPec;
	private int age;
	private int year;
	private HashMap<Ethnicity, Integer> demographics;
	
	public VoteData(int voters, Party party, int votingPec, int age, int year,
			HashMap<Ethnicity, Integer> demographics) {
		this.voters = voters;
		this.party = party;
		this.votingPec = votingPec;
		this.age = age;
		this.year = year;
		this.demographics = demographics;
	}
	
	public int getVoters() {
		return voters;
	}
	public Party getParty() {
		return party;
	}
	public int getVotingPec() {
		return votingPec;
	}
	public int getAge() {
		return age;
	}
	public int getYear() {
		return year;
	}
	public HashMap<Ethnicity, Integer> getDemographics() {
		return demographics;
	}
}
