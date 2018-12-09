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

//import java.io.IOException;
//import javax.websocket.Session;

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
	private double max = 10;
	private double min = 0.00000000000000000000000000000000000000000000000000000000000000001;
	private double alpha = 0.9;
	private Precinct currSol;
	private double currCost; 
	private GeneratedState gs;
	private ObjectiveFunction objf = new ObjectiveFunction();
	private List<Precinct> allPrecincts = new ArrayList<>();
	private List<District> remainingDistricts = new ArrayList<>();
	private List<String> moves = new LinkedList<String>(); 
	private int moveCount = 0;
	private Random random = new Random();
	private PrecisionModel pm = new PrecisionModel(100);
	private GeometryPrecisionReducer reducer = new GeometryPrecisionReducer(pm);
	private Map<Integer,List> neighbors;
	private Map<Integer, List> boundaries;
	private final int clusterNumber = 10;
//	private Session session;
	
    private Integer populationPrec = 100;
    private Integer compactnessPrec = 100;
    private Integer politicalPrec = 100;
	
//     Thread variables
    private volatile boolean running = true;
    private volatile boolean paused = false;
    private final Object pauseLock = new Object();
	
	public SimulatedAnnealing(StateID stateID, Integer populationPrec, Integer compactnessPrec, Integer politicalPrec){
		this.stateID = stateID;
		this.populationPrec = populationPrec;
		this.compactnessPrec = compactnessPrec;
		this.politicalPrec = politicalPrec;
		this.gs = new GeneratedState(stateID);
		this.gs.copyAllData();
		this.setupSA(stateID);
//        this.session = session;
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
		Move move;
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
				
//				clusteringSolution(selectedDistrict);
				
				/* Select a random boundary precinct using this district */
				List<Integer> precincts = getBoundaries(selectedDistrict);
				currSol = this.gs.getPrecinct(precincts.get(random.nextInt(precincts.size())));
//				currSol = selectBoundaryPrecinct(selectedDistrict, 
//						gs.getPrecicntList(gs.getDistrictList().get(selectedDistrictID).getDistrictId()),
//						remainingDistricts);
				if(currSol!=null){
				
					/* Calculate cost before making temp move */
					currCost = ObjectiveFunction.
					calculateFairnessGeneratedDistrict(this.gs.getDistrictList(),
					this.gs.getPrecinctData(),
					populationPrec,compactnessPrec,politicalPrec);
					
					/* Gets list of neighbors from the selected boundary precinct */
					List<Integer> neighbors = getNeighbors(currSol, allPrecincts);
					
					/* Loops through list of neighbors and finds neighbor with different district ID */
					for(Integer p: neighbors) {
						if(this.gs.getPrecinct(p).getDistrictId() != currSol.getDistrictId()) {
							selectedPrecinct = this.gs.getPrecinct(p);
							break;
						}
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
							move = makeMove(currSol, currSol.getDistrictId(), selectedPrecinct.getDistrictId());
//		                    sendMove(move);
							
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
					}
				}
			}
			max *= alpha;
//			System.out.println("max: " + max);
//			System.out.println("min: " + min);
//			System.out.println(moveCount);
//			System.out.println(newFairness);
		}
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
	    	makeMove(precinct, toDistrict, fromDistrict);
	    }

	    private double temporaryMove(Precinct precinct, Integer fromDistrict, Integer toDistrict){
	        Move move = makeMove(precinct,fromDistrict,toDistrict);
	        double fairness = ObjectiveFunction.calculateFairnessGeneratedDistrict(this.gs.getDistrictList(),
	                this.gs.getPrecinctData(),populationPrec,compactnessPrec,politicalPrec);
	        revertMove(move, precinct, fromDistrict, toDistrict);
	        return fairness;
	    }
	    
	    /* This generates 10 boundary precincts and returns the highest temporary move */
	    private Precinct clusteringSolution(District selectedDistrict){
	    	Precinct bestSolution = null; // return this precinct, should have the highest cost
	    	Precinct swapPrecinct = null; // bordering precinct with different district ID
	    	double highestCost = 0;
			
	    	for(int i = 0; i < clusterNumber; i++){
				currSol = selectBoundaryPrecinct(selectedDistrict, 
						gs.getPrecicntList(selectedDistrict.getDistrictId()),
						remainingDistricts);
				if(currSol!=null){
				
				/* Gets list of neighbors from the selected boundary precinct */
				List<Integer> neighbors = getNeighbors(currSol, allPrecincts);
				
					/* Loops through list of neighbors and finds neighbor with different district ID */
					for(Integer p: neighbors) {
						if(this.gs.getPrecinct(p).getDistrictId() != currSol.getDistrictId()) {
							swapPrecinct = this.gs.getPrecinct(p);
							break;
						}
					}
				
					/* Get the cost after the temporary move */
					if(swapPrecinct!=null && currSol.getDistrictId()!=swapPrecinct.getDistrictId()){
						double newCost = temporaryMove(currSol, currSol.getDistrictId(), swapPrecinct.getDistrictId());
						if (newCost > highestCost){
							highestCost = newCost;
							bestSolution = currSol;
						}
					}
				}	
	    	}
	    	
	    	return bestSolution;
	    }
		
//	    private void sendMove(Move move){
//	        try {
//	            session.getBasicRemote().sendText(move.toString());
//	        } catch (IOException e) {
//	            e.printStackTrace();
//	        }
//	    }
//	
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
//		for(Map.Entry<Integer, List> item: boundaries.entrySet()){
//			System.out.println(item.getValue().size());
//		}
//		System.exit(0);
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
    
public Precinct selectBoundaryPrecinct(District d, List<Precinct> districtPrecincts, List<District> remainingDistricts){
    	
		String db = d.getBoundary();
		String dbs = null, pbs = null, rdbs = null; 
		String pb= null, rdb= null;
		JSONObject dobj = null, pobj = null, rdobj = null;
		JSONObject gobj = null;
		Geometry pbi = null, dbi = null, rdbi = null; 
		GeoJsonReader reader = new GeoJsonReader();
		
/******  District Geometry ******/
		try {
			dobj = new JSONObject(db);			
			gobj = dobj.getJSONObject("geometry");
			dbs = gobj.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}		
		try {
			dbi = reader.read(dbs);
//			System.out.println("printing dbi   "+ dbi.toString());
		} catch (ParseException e) {
			e.printStackTrace();
		}	
/******  Precinct Geometry ******/
		for (Precinct p: districtPrecincts) {
			pb = p.getBoundaryJSON();	
			try {
				pobj = new JSONObject(pb);
				gobj = pobj.getJSONObject("geometry");
				pbs = gobj.toString();
			} catch (JSONException e) {
				e.printStackTrace();
			}	 
			try {
				 pbi = reader.read(pbs);
//					System.out.println("printing pbi   "+ pbi.toString());
			} catch (ParseException e) {
				e.printStackTrace();
			}
/******  Remaining District Geometry ******/
			/******  If Precinct is a boundary precinct compare with Remaining District Geometry ******/
			pbi = reducer.reduce(pbi);
			if(pbi.intersects(reducer.reduce(dbi))) {
				for (District d1 : remainingDistricts) {
						rdb = d1.getBoundary();
						try {
							rdobj = new JSONObject(rdb);
							gobj = rdobj.getJSONObject("geometry");
							rdbs = gobj.toString();
						} catch (JSONException e) {
							e.printStackTrace();
						}
						try {
							 rdbi = reader.read(rdbs);
//								System.out.println("printing rdbi   "+ rdbi.toString());
						} catch (ParseException e) {
							e.printStackTrace();
						}
						if(pbi.intersects(reducer.reduce(rdbi)))
						return p;
				}
			}
			/******  Else go back and check the next precinct in List ******/
		}
		return null;
    }
    
	public List<Integer> getBoundaries(District district){
		return boundaries.get("" + district.getDistrictId());
	}

    public List<Integer> getNeighbors(Precinct boundaryPrecinct, List<Precinct> allPrecincts){
    	
    	return neighbors.get("" + boundaryPrecinct.getPrecinctId());
//		String bp = null, pb = null;
//		String bps = null, pbs = null;
//		JSONObject bpobj = null, pbobj = null;
//		JSONObject gobj = null;
//		Geometry bpi = null , pbi = null; 
//		GeoJsonReader reader = new GeoJsonReader();
///****** Boundary Precinct Geometry ******/		
//		bp = boundaryPrecinct.getBoundaryJSON();
//		try {
//			bpobj = new JSONObject(bp);
//			gobj = bpobj.getJSONObject("geometry");
//			bps = gobj.toString();
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
//		try {
//			bpi = reader.read(bps);
//			bpi = reducer.reduce(bpi).buffer(0);
//		} catch (ParseException e1) {
//			e1.printStackTrace();
//		}
///******  Neighbor Precinct Geometry ******/
//        List<Precinct> realNeighborList = new ArrayList<Precinct>();  //List of neighbor precincts to be returned
//        for (Precinct p: allPrecincts) {
//			pb = p.getBoundaryJSON();	
//			try {
//				pbobj = new JSONObject(pb);
//				gobj = pbobj.getJSONObject("geometry");
//				pbs = gobj.toString();
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//			try {
//				 pbi = reader.read(pbs);
//				 pbi = reducer.reduce(pbi).buffer(0);
////					System.out.println("printing pbi   "+ pbi.toString());
//			} catch (ParseException e) {
//				e.printStackTrace();
//			}
//			if(pbi.intersects(bpi)) {
//				if(boundaryPrecinct.getPrecinctId()!=p.getPrecinctId())
//				realNeighborList.add(p);
//			}
//		}
//        return realNeighborList;
    }
}