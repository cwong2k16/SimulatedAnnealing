import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;

//import javax.websocket.Session;
import java.io.IOException;

import gerrymandering.model.District;
import gerrymandering.model.Precinct;

public class SimulatedAnnealing implements Runnable{
	private StateID stateID;
	private double max = 1.0;
	private double min = 0.0001;
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
//	private Session session;
	
    private Integer populationPrec = 100;
    private Integer compactnessPrec = 100;
    private Integer politicalPrec = 100;
	
    // Thread variables
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
		this.setupSA();
//        this.session = session;
	}

	public double acceptanceProbability(double oldCost, double newCost, double max){
		// if new is better than old, definitely want to move this
		if(newCost > oldCost){
			return 1;
		}
		
		// if new is worse than old, MAYBE want to move this
		double formula = Math.exp((newCost - oldCost)/max);
		return formula;
	}
	
	public void run(){
		int selectedDistrictID = 0;
		District selectedDistrict = null;
		Precinct selectedPrecinct = null;
		Move move;
		double newFairness;
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
			if(max<=min){
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
				currSol = selectBoundaryPrecinct(selectedDistrict, 
						gs.getPrecicntList(selectedDistrictID),
						remainingDistricts);
				
				/* Calculate cost before making temp move */
				currCost = ObjectiveFunction.
				calculateFairnessGeneratedDistrict(this.gs.getDistrictList(),
				this.gs.getPrecinctData(),
				populationPrec,compactnessPrec,politicalPrec);
				
				/* Gets list of neighbors from the selected boundary precinct */
				List<Precinct> neighbors = getNeighbors(currSol, allPrecincts);
				
				/* Loops through list of neighbors and finds the neighbor with a different district ID */
				for(Precinct p: neighbors) {
					if(p.getDistrictId()!=currSol.getDistrictId()) {
						selectedPrecinct = p;
						break;
					}
				}
				
				/* Get the cost after the temporary move */
				System.out.println("a: " + currSol.getDistrictId() + " " + selectedPrecinct.getDistrictId());
				double newCost = temporaryMove(currSol, currSol.getDistrictId(), selectedPrecinct.getDistrictId());

				System.out.println("b: " + currSol.getDistrictId() + " " + selectedPrecinct.getDistrictId());
				/* Run the new cost and the curr cost into the acceptance probability*/
				double ac = acceptanceProbability(currCost, newCost, max);
				if(ac > Math.random()){
	                newFairness = ObjectiveFunction.
	                        calculateFairnessGeneratedDistrict(this.gs.getDistrictList(),
	                                this.gs.getPrecinctData(),
	                                populationPrec,compactnessPrec,politicalPrec);
	                System.out.println("Old Fairness: " + ObjectiveFunction.databaseStateFairness(stateID) +
	                        " New Fairness: " + newFairness);
					move = makeMove(currSol, currSol.getDistrictId(), selectedPrecinct.getDistrictId());
//                    sendMove(move);
	                moveCount++;
	                System.out.println(moveCount+" Move Made: " + move.toString());
					moves.add(move.toString());
					currCost = newCost;
				}
			}
			max *= alpha;
		}
	}
	
		public int selectedDistrict(){
			remainingDistricts = new ArrayList<>();
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
	        precinct.setDistrictId(toDistrict);
	        this.gs.addPrecinct(toDistrict, precinct);
	        this.gs.removePrecinct(fromDistrict, precinct);
			this.allPrecincts.remove(precinct);
	        return move;
	    }
	    private Move revertMove(Move move){
	        Move reverseMove = new Move(move.getPrecinctID(),move.getToDistrictID(),move.getFromDistrictID());
	        Precinct precinct = this.gs.getPrecinct(reverseMove.getPrecinctID());
	        precinct.setDistrictId(reverseMove.getToDistrictID());
	        this.gs.removePrecinct(reverseMove.getFromDistrictID(),precinct);
	        this.allPrecincts.add(precinct);
	        return reverseMove;
	    }

	    private double temporaryMove(Precinct precinct, Integer fromDistrict, Integer toDistrict){
	        Move move = makeMove(precinct,fromDistrict,toDistrict);
	        double fairness = ObjectiveFunction.calculateFairnessGeneratedDistrict(this.gs.getDistrictList(),
	                this.gs.getPrecinctData(),populationPrec,compactnessPrec,politicalPrec);
	        revertMove(move);
	        return fairness;
	    }
		
//	    private void sendMove(Move move){
//	        try {
//	            session.getBasicRemote().sendText(move.toString());
//	        } catch (IOException e) {
//	            e.printStackTrace();
//	        }
//	    }
//	
		public void setupSA(){	
			for(Entry<Integer, List<Precinct>> entry : gs.getPrecinctData().entrySet()) {
				List<Precinct> currPrecs = entry.getValue();
				for(Precinct p: currPrecs) {
					allPrecincts.add(p);
				}
			}
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
			if(pbi.intersects(dbi)) {
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
						if(pbi.intersects(rdbi))
						return p;
				}
			}
			/******  Else go back and check the next precinct in List ******/
		}
		return null;
    }
    
    public List<Precinct> getNeighbors(Precinct boundaryPrecinct, List<Precinct> allPrecincts){
    	
		String bp = null, pb = null;
		String bps = null, pbs = null;
		JSONObject bpobj = null, pbobj = null;
		JSONObject gobj = null;
		Geometry bpi = null , pbi = null; 
		GeoJsonReader reader = new GeoJsonReader();
/****** Boundary Precinct Geometry ******/		
		bp = boundaryPrecinct.getBoundaryJSON();
		try {
			bpobj = new JSONObject(bp);
			gobj = bpobj.getJSONObject("geometry");
			bps = gobj.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		try {
			bpi = reader.read(bps);
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
/******  Neighbor Precinct Geometry ******/
        List<Precinct> realNeighborList = new ArrayList<Precinct>();  //List of neighbor precincts to be returned
        for (Precinct p: allPrecincts) {
			pb = p.getBoundaryJSON();	
			try {
				pbobj = new JSONObject(pb);
				gobj = pbobj.getJSONObject("geometry");
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
			if(pbi.intersects(bpi)) {
				realNeighborList.add(p);
			}
		}
        return realNeighborList;
    }
}