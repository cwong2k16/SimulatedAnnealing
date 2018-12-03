import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;

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
	private Move move; 
	private List<String> moves = new LinkedList<String>(); 
	private int moveCount = 0;
	
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
			
		int randDist = (int)(Math.random() * gs.getDistrictList().size()-1);
		
		List<District> remainingDistricts = new ArrayList<District>();
		for(int i = 0; i < gs.getDistrictList().size(); i++){
			if(i != randDist) {
				remainingDistricts.add(gs.getDistrictList().get(i));
			}
		}
		
		for(Entry<Integer, List<Precinct>> entry : gs.getPrecinctData().entrySet()) {
			List<Precinct> currPrecs = entry.getValue();
			for(Precinct p: currPrecs) {
				allPrecincts.add(p);
			}
		}
		boolean districtExists = gs.getDistrictList().get(randDist)!=null;
//		System.out.println("Line 58 (param values): " + districtExists + " " + gs.getPrecicntList(gs.getDistrictList().get(randDist).getDistrictId()).size()
//							+" " + remainingDistricts.size());
		currSol = selectBoundaryPrecinct(gs.getDistrictList().get(randDist), 
										gs.getPrecicntList(gs.getDistrictList().get(randDist).getDistrictId()),
										remainingDistricts);
//		boolean currSolExists = currSol!=null;
//		System.out.println("Line 64 (return value): " + currSolExists);

		currCost = calculateObjFunc(currSol);
	}
	
	private double calculateObjFunc(Precinct p) {
		int newObjVal = 0;
		return newObjVal;
	}

	public double acceptanceProbability(double oldCost, double newCost, double max){
		double formula = Math.exp((newCost - oldCost)/max);
		return formula;
	}
	
	public void run(){
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
			Precinct newSol = null;
			
			List<Precinct> neighbors = getNeighbors(currSol, allPrecincts);
			System.out.println("neighbors size: " + neighbors.size());
			for(Precinct p: neighbors) {
				if(p.getDistrictId()!=currSol.getDistrictId()) {
					newSol = p;
					break;
				}
			}
			double newCost = calculateObjFunc(newSol);
			double ac = acceptanceProbability(currCost, newCost, max);
			if(ac > Math.random()){
				move = new Move(currSol.getPrecinctId(), currSol.getDistrictId(), newSol.getDistrictId());
                moveCount++;
                System.out.println(moveCount+" Move Made: " + move.toString());
				moves.add(move.toString());
				currSol = newSol;
				currCost = newCost;
			}
			max *= alpha;
			System.out.println(max);
			System.out.println(min);
			if(max<=min){
                newFairness = ObjectiveFunction.
                        calculateFairnessGeneratedDistrict(this.gs.getDistrictList(),
                                this.gs.getPrecinctData(),
                                populationPrec,compactnessPrec,politicalPrec);
                System.out.println("Old Fairness: " + ObjectiveFunction.databaseStateFairness(stateID) +
                        " New Fairness: " + newFairness);
				this.running = false;
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