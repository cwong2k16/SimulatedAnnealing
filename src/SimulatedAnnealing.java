import java.util.LinkedList;
import java.util.List;

import gerrymandering.model.District;
import gerrymandering.model.Precinct;

public class SimulatedAnnealing implements Runnable{
	private double max = 1.0;
	private double min = 0.0001;
	private double alpha = 0.9;
	private Precinct currSol;
	private double currCost; // or "old" cost
	private DataSingleton singleton = DataSingleton.getInstance();
	private GeneratedState gs;
	private List<Precinct> neighbors;
	private Move move; 
	private List<String> moves = new LinkedList<String>(); // add move.toString();
    private volatile boolean running = true;
    private volatile boolean paused = false;
    private final Object pauseLock = new Object();
	
	public SimulatedAnnealing(StateID stateID){
		gs = new GeneratedState(stateID);
		gs.copyAllData();
		
//		for(District d: singleton.getDistricts(stateID)){
//			gs.addDistrict(d);
//		}
//		int randDist = (int)(Math.random() * gs.getDistrictList().size()-1);
//		
//		for(Precinct p: singleton.getPrecincts(stateID, gs.getDistrictList().get(randDist).getDistrictId())) {
//				gs.addPrecinct(gs.getDistrictList().get(randDist).getDistrictId(), p);
//		}
		
		/* Sahil's to-do code below */
		// send gs.getDistrictList().get(randDist), and gs.getPrecicntList() //
		// neighbors = SahilsCode(gs.getDistrictList().get(randDist), gs.getPrecicntList());
			
		/* Start off the simulated annealing algorithm using a randomly selected boundary precinct */
		currSol = selectRandomPrecinct(neighbors);
		
		/* Calculate the current cost of precinct by calling the calculateObjFunc */
		currCost = calculateObjFunc(currSol);
	}
	
	private float calculateObjFunc(Precinct p) {
		/* Dummy solution for now... */
		/* getCost() should be based on compactness, population, partisan fairness, etc. */
		/* along with user's weights */
		return 0;
	}

	public Precinct selectRandomPrecinct(List<Precinct> p){
		int rand = (int)(Math.random() * p.size()-1);
		return p.get(rand);
	}
	
	/* Acceptance Probability formula: e * (new - old)/max */
	public double acceptanceProbability(double oldCost, double newCost, double max){
		double formula = Math.exp((newCost - oldCost)/max);
		return formula;
	}
	
	/* Main logic of the algorithm */
	/* Reference: http://katrinaeg.com/simulated-annealing.html */
	public void run(){
		while(running) {
			synchronized (pauseLock){
				if(!running) {
					break;
				}
				if(paused) {
					try {
						pauseLock.wait();
						/*
						 will cause this Thread to block until
                         another thread calls pauseLock.notifyAll()
                         Note that calling wait() will
                         relinquish the synchronized lock that this
                         thread holds on pauseLock so another thread
                         can acquire the lock to call notifyAll()
                         (link with explanation below this code)
                         */
					}
					catch(InterruptedException ex) {
						break;
					}
					if(!running) {
						break;
					}
				}
			}
			while(max > min){
				int i = 1;
				while(i<=100){
					Precinct newSol = getNeighbor(currSol);
					double newCost = calculateObjFunc(newSol);
					double ac = acceptanceProbability(currCost, newCost, max);
					if(ac > Math.random()){
						currSol = newSol;
						currCost = newCost; 
					}
					i++;
				}
				max *= alpha;
			}
		}
	}

    public void resume() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll(); // Unblocks thread
        }
    }

    public void stop() {
        running = false;
        // you might also want to interrupt() the Thread that is
        // running this Runnable, too, or perhaps call:
        resume();
        // to unblock
    }

    public void pause() {
        // you may want to throw an IllegalStateException if !running
        paused = true;
    }
    public void finish(){
        this.running = false;
    }
}
