/* Return results for Simulated Annealing: Solution Precinct, Cost */
public class SAResult {
	private Precinct solution;
	private double cost;
	
	public SAResult(Precinct solution, double cost) {
		this.solution = solution;
		this.cost = cost;
	}

	public Precinct getSolution() {
		return solution;
	}

	public void setSolution(Precinct solution) {
		this.solution = solution;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}
	
}
