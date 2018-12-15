import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import java.io.IOException;
import javax.websocket.Session;

import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.precision.GeometryPrecisionReducer;

import com.fasterxml.jackson.databind.ObjectMapper;

import gerrymandering.model.District;
import gerrymandering.model.Precinct;

public class SimulatedAnnealing implements Runnable{
	private StateID stateID;
	private float max = 10000000000000000000000000000000000000f;
	private float min = 0.00000000000000000000000000000000000000000001f;
	private float alpha = 0.9f;
	private Precinct currSol;
	private double currCost; 
	private GeneratedState gs;
	private List<Precinct> allPrecincts = new ArrayList<>();
	private List<District> remainingDistricts = new ArrayList<>();
	private List<String> moves = new LinkedList<String>(); 
	private int moveCount = 0;
	private Random random = new Random();
	private PrecisionModel pm = new PrecisionModel(100);
	private Map<Integer,List> neighbors;
	private Map<Integer, List> boundaries;
	private boolean useSecondVariant = true;
	private Session session;
	
    private Integer populationPrec = 100;
    private Integer compactnessPrec = 100;
    private Integer politicalPrec = 100;
	
//     Thread variables
    private volatile boolean running = true;
    private volatile boolean paused = false;
    private final Object pauseLock = new Object();
	
	public SimulatedAnnealing(StateID stateID, Integer populationPrec, Integer compactnessPrec, Integer politicalPrec, Session session){
		this.stateID = stateID;
		this.populationPrec = populationPrec;
		this.compactnessPrec = compactnessPrec;
		this.politicalPrec = politicalPrec;
		this.gs = new GeneratedState(stateID);
		this.gs.copyAllData();
		this.setupSA(stateID);
        this.session = session;
	}

	public double acceptanceProbability(double oldCost, double newCost, double max){
		// if new > old, definitely move
		if(newCost>oldCost){
			return 1.0;
		}
		
		// if new < old, maybe move...
		double formula = Math.exp((newCost - oldCost)/max);
		return formula;
	}
	
	public void run(){
		int selectedDistrictID = 0;
		District selectedDistrict = null;
		Precinct selectedPrecinct = null;
		Move move = null;
		double newFairness = 0;
		while(running) {
			synchronized (pauseLock){
				if(!running) {
					break;
				}
				if(paused) {
					try {
						pauseLock.wait();
					}
					catch(InterruptedException ex) {
						break;
					}
					if(!running) {
						break;
					}
				}
			}
			/* Reference: http://katrinaeg.com/simulated-annealing.html */
			if(max<min){
                newFairness = ObjectiveFunction.
                        calculateFairnessGeneratedDistrict(this.gs.getDistrictList(),
                                this.gs.getPrecinctData(),
                                populationPrec,compactnessPrec,politicalPrec);
                System.out.println("Old Fairness: " + ObjectiveFunction.databaseStateFairness(stateID) +
                        " New Fairness: " + newFairness);
				this.running = false;
			}
			else{
				/* Gets ID of selected district at random */
				selectedDistrictID = selectedDistrict();
				
				/* Gets selected district using the random ID */
				selectedDistrict = gs.getDistrictList().get(selectedDistrictID);
				
				/* Select a random boundary precinct using this district */
				List<Integer> precincts = getBoundaries(selectedDistrict);
				currSol = this.gs.getPrecinct(precincts.get(random.nextInt(precincts.size())));
				
				if(currSol!=null){
				
					/* Calculate cost before making temp move */
					currCost = ObjectiveFunction.
					calculateFairnessGeneratedDistrict(this.gs.getDistrictList(),
					this.gs.getPrecinctData(),
					populationPrec,compactnessPrec,politicalPrec);
					
					/* Gets list of neighbors from the selected boundary precinct */
					List<Integer> neighbors = getNeighbors(currSol, allPrecincts);
					
					/* Loops through list of neighbors and finds neighbor with different district ID */
					if(!this.useSecondVariant){
						selectedPrecinct = selectFirstFit(neighbors);
					}
					
					else{
						selectedPrecinct = selectBestFit(neighbors);
					}
					
					/* Get the cost after the temporary move */
					if(selectedPrecinct!=null && currSol.getDistrictId()!=selectedPrecinct.getDistrictId()){
						double newCost = temporaryMove(currSol, currSol.getDistrictId(), selectedPrecinct.getDistrictId());
						double ac = acceptanceProbability(currCost, newCost, max);
//						System.out.println("Move from: " + currSol.getDistrictId() + " to " + selectedPrecinct.getDistrictId());
						if(ac > Math.random()){
							boundaries.get("" + selectedPrecinct.getDistrictId()).add(currSol.getPrecinctId());
							boundaries.get("" + currSol.getDistrictId()).remove(
									boundaries.get("" + currSol.getDistrictId()).indexOf(currSol.getPrecinctId()));
							
							neighbors = getNeighbors(currSol, allPrecincts);
							for(int i = 0; i < neighbors.size(); i++){
								
								/* Absolute monstrosity ... */
								/* if boundaries of current precinct we want to move does not contain neighbors and its neighbor district IDs are equal to current precinct
								 * district ID, then add these to the boundaries for curr precinct's district ID
								 */
								if(!boundaries.get("" + currSol.getDistrictId()).contains(this.gs.getPrecinct(neighbors.get(i)).getPrecinctId()) && 
									this.gs.getPrecinct(neighbors.get(i)).getDistrictId() == currSol.getDistrictId()){
									boundaries.get("" + currSol.getDistrictId()).add(this.gs.getPrecinct(neighbors.get(i)).getPrecinctId());
								}
							}
									
							move = makeMove(currSol, currSol.getDistrictId(), selectedPrecinct.getDistrictId());
		                    sendMove(move);
							
			                newFairness = ObjectiveFunction.
			                        calculateFairnessGeneratedDistrict(this.gs.getDistrictList(),
			                                this.gs.getPrecinctData(),
			                                populationPrec,compactnessPrec,politicalPrec);
//			                System.out.println("Old Fairness: " + ObjectiveFunction.databaseStateFairness(stateID) +
//			                        " New Fairness: " + newFairness);
							
			                moveCount++;
			                System.out.println(moveCount+" Move Made: " + move.toString());
							moves.add(move.toString());
							currCost = newCost;
						}
						else
						{
							moveCount++;
							if(move!=null)
							System.out.println(moveCount+" Move Rejected: " + move.toString());
						}
					}
				}
			}
			max *= alpha;
		}
	}
	
		private Precinct selectBestFit(List<Integer> neighbors) {
			Precinct precinct = null;
			double fairness = 0;
			for(Integer p: neighbors) {
				if(this.gs.getPrecinct(p).getDistrictId() != currSol.getDistrictId()) {
					double thisFairness = temporaryMove(currSol, currSol.getDistrictId(), this.gs.getPrecinct(p).getDistrictId());
					if(fairness < thisFairness){
						fairness = thisFairness;
						precinct = this.gs.getPrecinct(p);
					}
				}
			}
			return precinct;
	}

		private Precinct selectFirstFit(List<Integer> neighbors) {
			Precinct precinct = null;
			for(Integer p: neighbors) {
				if(this.gs.getPrecinct(p).getDistrictId() != currSol.getDistrictId()) {
					precinct = this.gs.getPrecinct(p);
					break;
				}
			}
			return precinct;
	}

		public int selectedDistrict(){
			remainingDistricts.clear();
			int randDist = (int)(random.nextInt(gs.getDistrictList().size()));
			for(int i = 0; i < gs.getDistrictList().size(); i++){
				if(i != randDist) {
					remainingDistricts.add(gs.getDistrictList().get(i));
				}
			}
			return randDist;
		}
		
	    private Move makeMove(Precinct precinct, Integer fromDistrict, Integer toDistrict) {
	        Move move = new Move(precinct.getPrecinctId(), fromDistrict, toDistrict);
	        this.gs.removePrecinct(fromDistrict, precinct);
	        precinct.setDistrictId(toDistrict);
	        this.gs.addPrecinct(toDistrict, precinct);
	        return move;
	    }
	    
	    private void revertMove(Move move, Precinct precinct, Integer fromDistrict, Integer toDistrict){
	    	Move newMove = makeMove(precinct, toDistrict, fromDistrict);
	    	moves.add(newMove.toString());
	    	sendData(moves);
	    }

	    private double temporaryMove(Precinct precinct, Integer fromDistrict, Integer toDistrict){
	        Move move = makeMove(precinct,fromDistrict,toDistrict);
	        moves.add(move.toString());
	        sendData(moves);
	        double fairness = ObjectiveFunction.calculateFairnessGeneratedDistrict(this.gs.getDistrictList(),
	                this.gs.getPrecinctData(),populationPrec,compactnessPrec,politicalPrec);
	        revertMove(move, precinct, fromDistrict, toDistrict);
	        return fairness;
	    }
		
	    private void sendMove(Move move){
	        try {
	            session.getBasicRemote().sendText(move.toString());
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	
	public void setupSA(StateID stateID){	
		for(Entry<Integer, List<Precinct>> entry : gs.getPrecinctData().entrySet()) {
			List<Precinct> currPrecs = entry.getValue();
			for(Precinct p: currPrecs) {
				allPrecincts.add(p);
			}
		}
		String state = "";
		if(StateID.NE == stateID){
			state = "Nebraska";
		}
		else if(StateID.KS == stateID){
			state = "Kansas";
		}
		else if(StateID.IA == stateID){
			state = "Iowa";
		}
		byte[] mapData = null; // this is for neighbors, format = {122: [123,124], 137: [138, 139]}; 
		byte[] mapData2 = null; // this is for boundary precincts, format = same as above
		
		try {
			mapData = Files.readAllBytes(Paths.get("c://users//c//workspace//CSE308//src//" + state + ".json"));
			mapData2 = Files.readAllBytes(Paths.get("c://users//c//workspace//CSE308//src//" + state + "Bp.json"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		neighbors = new HashMap<>();
		boundaries = new HashMap<>();
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			neighbors = objectMapper.readValue(mapData, HashMap.class);
			boundaries = objectMapper.readValue(mapData2, HashMap.class);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void useSecondVariantAlgo(boolean use){
		this.useSecondVariant = use;
	}
	
	public List<String> getMoves(){
		return moves;
	}
	
    public String getMovesJson(){
        return Utilities.moveJsonList(moves);
    }

    public void resume() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
    }

    public void stop() {
        running = false;
        resume();
    }

    public void pause() {
        paused = true;
    }
    public void finish(){
        this.running = false;
    }
    
	public List<Integer> getBoundaries(District district){
		return boundaries.get("" + district.getDistrictId());
	}

    public List<Integer> getNeighbors(Precinct boundaryPrecinct, List<Precinct> allPrecincts){
    	return neighbors.get("" + boundaryPrecinct.getPrecinctId());
    }
}