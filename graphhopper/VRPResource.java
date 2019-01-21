package com.graphhopper.resources;

import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopperAPI;
import com.graphhopper.util.PointList;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.lang.Math;

// Adding the custom VRP API to the GraphHopper server.
// For this to work, adding
// environment.jersey().register(VRPResource.class);
// is required in GraphHopperBundle.java in the runRegularGraphHopper or
// runPtGraphHopper function (preferably both)
@Path("vrp")
@Produces(MediaType.APPLICATION_JSON)
public class VRPResource {

	// Making a wrapper class for ArrayList<Integer>
	private class ArrayListWrapper {
		private ArrayList<Integer> arr;
	  
		public ArrayListWrapper() {
			this.arr = new ArrayList<Integer>();
		}
		
		public void add(int x) {
			this.arr.add(x);
		}
		
		public void add(int i, int x) {
			this.arr.add(i, x);
		}
		
		public void addAll(int i, int[] c, int n) {
			for(int j = n - 1; j >= 0; j--)
				this.arr.add(i, c[j]);
		}
		
		public void remove(int i) {
			this.arr.remove(i);
		}
		
		public int get(int i) {
			return this.arr.get(i);
		}
		
		public int size() {
			return this.arr.size();
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(VRPResource.class);
	private final GraphHopperAPI graphHopper;

	// Inject makes sure the constructor is properly called
	@Inject
	public VRPResource(GraphHopperAPI graphHopper) {
		this.graphHopper = graphHopper;
	}
	
	// This function is called when a request is sent to /vrp/.
	// It creates a matrix of distances between any two points
	// then it runs the VRP algorithm and gets the path points of the result.
	// Finally, it packs them into a JSON object that is returned as a Response.
	@GET
	public Response getVRP(
		@QueryParam("p") List<GHPoint> requestPoints,
		@QueryParam("c") int cars,
		@QueryParam("t") char type
	) {
		int points = requestPoints.size();
		double[][] matrix = new double[points][points];
		
		buildMatrix(matrix, requestPoints);
		
		ArrayListWrapper[] paths = (type == 'g') ? vrpGreedy(matrix, cars, points) : vrpSA(matrix, cars, points, type);	
		
		PointList[] routes = new PointList[cars];
		
		logger.info("Getting routes...");
		
		for(int i = 0; i < cars; i++) {
			logger.info("Getting route for car " + (i+1) + "/" + cars);
			routes[i] = new PointList(0, false);
			for(int j = 0; j < paths[i].size() - 1; j++) {
				GHRequest request = new GHRequest(requestPoints.get(paths[i].get(j)), requestPoints.get(paths[i].get(j+1)));
				GHResponse response = graphHopper.route(request);
				routes[i].add(response.getBest().getPoints());
			}
		}
		
		logger.info("Building JSON...");
		
		ObjectNode json = JsonNodeFactory.instance.objectNode();
		ArrayNode jsonPathList = json.putArray("paths");
		
		for(int i = 0; i < cars; i++) {
			ObjectNode jsonPath = jsonPathList.addObject();
			jsonPath.putPOJO("points", routes[i].toLineString(false));
		}
		
		json.put("distance", calcDistance(paths, matrix, cars));
		json.put("maxDistance", calcMaxDistance(paths, matrix, cars));
		json.put("carsUtilized", calcUtilizedCars(paths, matrix, cars));
		
		logger.info("Success!");
		return Response.ok(json).build();
	}
	
	// Builds the matrix using simple requests.
	private void buildMatrix(double[][] matrix, List<GHPoint> requestPoints) {
		int points = requestPoints.size();
		logger.info("Building matrix...");
		
		for(int i = 0; i < points; i++) {
			logger.info("Calculating point " + (i + 1) + "/" + points);
			for(int j = 0; j < points; j++) {
				if(i == j) {
					matrix[i][j] = 0;
				}
				else {
					GHRequest request = new GHRequest(requestPoints.get(i), requestPoints.get(j));
					GHResponse response = graphHopper.route(request);
					matrix[i][j] = response.getBest().getDistance();
				}
			}
		}
	}
	
	// Greedy VRP implementation. It builds the paths by finding the shortest distance
	// between any car's current position and an unvisited point.
	private ArrayListWrapper[] vrpGreedy(double[][] matrix, int cars, int length) {
		ArrayListWrapper[] paths = new ArrayListWrapper[cars];
		ArrayList<Integer> visited = new ArrayList<Integer>();
		visited.add(0);
		logger.info("Using Greedy Algorithm...");
		
		for(int i = 0; i < cars; i++) {
			paths[i] = new ArrayListWrapper();
			paths[i].add(0);
		}
		
		while(visited.size() < length) {
			int to = 0;
			double minVal = Integer.MAX_VALUE;
			int car = 0;
			ArrayList<Integer> candidates = new ArrayList<Integer>();
		
			// Candidates are points that a vehicle is currently on (from)
			for(int i = 0; i < cars; i++)
				candidates.add(paths[i].get(paths[i].size() - 1));
		  
			// Finding the lowest cost between a candidate and an unvisited point
			for(int i = 0; i < candidates.size(); i++) {
				for(int j = 0; j < length; j++) {
					if(visited.indexOf(j) == -1) {
						if(matrix[candidates.get(i)][j] < minVal) {
							minVal = matrix[candidates.get(i)][j];
							to = j;
							car = i;
						}
					}
				}
			}
			paths[car].add(to);
			visited.add(to);
		}
		logger.info("Fitness: " + calcFitness(paths, matrix, cars));
		
		return paths;
	}
	
	// Calculate number of cars with non-zero distance
	private int calcUtilizedCars(ArrayListWrapper[] paths, double[][] matrix, int cars) {
		int count = 0;
		for(int i = 0; i < cars; i++)
			if(paths[i].size() > 1)
				count++;
		return count;
	}
	
	// Finds the longest distance a single car has to travel
	private double calcMaxDistance(ArrayListWrapper[] paths, double[][] matrix, int cars) {
		double max = 0;
		for(int i = 0; i < cars; i++) {
			double distance = 0;
			for(int j = 0; j < paths[i].size() - 1; j++)
				distance += matrix[paths[i].get(j)][paths[i].get(j+1)];
			if(distance > max)
				max = distance;
		}
		return max;
	}
	
	// Calculates the total distance of the VRP solution
	private double calcDistance(ArrayListWrapper[] paths, double[][] matrix, int cars) {
		double distance = 0;
		for(int i = 0; i < cars; i++)
			for(int j = 0; j < paths[i].size() - 1; j++)
				distance += matrix[paths[i].get(j)][paths[i].get(j+1)];
		return distance;
	}
	
	// Calculates the fitness of a VRP solution for simulates annealing purposes.
	// The fitness is the total distance plus longest distance by a single car.
	private double calcFitness(ArrayListWrapper[] paths, double[][] matrix, int cars) {
		double[] distances = new double[cars];
		for(int i = 0; i < cars; i++) {
			distances[i] = 0;
			for(int j = 0; j < paths[i].size() - 1; j++)
				distances[i] += matrix[paths[i].get(j)][paths[i].get(j+1)];
		}
		double sum = 0;
		double max = 0;
		for(int i = 0; i < cars; i++) {
			sum += distances[i];
			if(distances[i] > max)
				max = distances[i];
		}
		
		return (sum + max)/1000;
	}
	
	// Generates a random permutation as the initial state for the
	// simulated annealing algorithm.
	private ArrayListWrapper[] randomPermutation(int cars, int length) {
		ArrayListWrapper[] paths = new ArrayListWrapper[cars];
		ArrayList<Integer> points = new ArrayList<Integer>();
		Random r = new Random();
		for(int i = 0; i < cars; i++) {
			paths[i] = new ArrayListWrapper();
			paths[i].add(0);
		}
		// Creates a pool of indices of points in order to be randomly selected
		// without order into a random car.
		for(int i = 1; i < length; i++)
			points.add(i);
		while(points.size() > 0) {
			int car = r.nextInt(cars);
			int point = r.nextInt(points.size());
			paths[car].add(points.get(point));
			points.remove(point);
		}
		
		return paths;
	}
	
	// Performs a deep copy of a VRP solution.
	private ArrayListWrapper[] copyPaths(ArrayListWrapper[] paths, int cars) {
		ArrayListWrapper[] newPaths = new ArrayListWrapper[cars];
		for(int i = 0; i < cars; i++) {
			newPaths[i] = new ArrayListWrapper();
			for(int j = 0; j < paths[i].size(); j++)
				newPaths[i].add(paths[i].get(j));
		}
		return newPaths;
	}
	
	// Simulated annealing implementation. It begins with a random inital state
	// and slowly converges upon some local minimum. The slower the convergence rate
	// (COOLING) the less likely it is to be stuck in some local min. Parameters are
	// determined by the user's choice.
	private ArrayListWrapper[] vrpSA(double[][] matrix, int cars, int length, char type) {
		logger.info("Using SA Algorithm...");
		
		int index = 0; // Very fast
		if(type == '1') index = 1; // Fast
		else if(type == '2') index = 2; // Moderate
		else if(type == '3') index = 3; // Slow
		else if(type == '4') index = 4; // Thourough
		
		logger.info("Selected setting " + index);
		
		double[] COOLING_ARR = {0.95, 0.97, 0.98, 0.99, 0.995};
		int[] ITERS_ARR = {5000, 10000, 15000, 20000, 25000};
		double[] TEMP_FINISH_ARR = {1.0, 0.75, 0.5, 0.25, 0.1};
		
		double COOLING = COOLING_ARR[index];
		int ITERS = ITERS_ARR[index];
		double TEMP_START = 100;
		double TEMP_FINISH = TEMP_FINISH_ARR[index];
		double temp = TEMP_START;
		Random r = new Random();
		
		ArrayListWrapper[] bestPaths = randomPermutation(cars, length);
		double bestFitness = calcFitness(bestPaths, matrix, cars);
		ArrayListWrapper[] bestestPaths = copyPaths(bestPaths, cars);
		double bestestFitness = bestFitness;
		
		while(temp > TEMP_FINISH) {
			temp *= COOLING;
			double startFitness = bestestFitness;
			
			// Perform ITERS iterations of generating a neighboring solution before lowering
			// the temperature of the system.
			for(int k = 0; k < ITERS; k++) {
				ArrayListWrapper[] paths = copyPaths(bestPaths, cars);
				double mutation = r.nextDouble();
				
				int car1 = r.nextInt(cars);
				int car2 = r.nextInt(cars);
				if(mutation < 0.99) {
					while(paths[car1].size() == 1)
						car1 = r.nextInt(cars);
				}
				int point1 = r.nextInt(Math.max(paths[car1].size() - 1, 1)) + 1;
				int point2 = r.nextInt(paths[car2].size()) + 1;
				
				// Selects a random point, removes it from the solution, and reinserts it
				// in a random place, possibly even the same one.
				if(mutation < 0.99) {
					paths[car2].add(point2, paths[car1].get(point1));
					if(car1 == car2 && point1 > point2)
						point1++;
					paths[car1].remove(point1);
				}
				// Selects a random number of points starting from the beginning of a path and
				// moves it to the beginning of another path. This prevents cars being unused
				// and maximizes efficiency and load distribution.
				else if(mutation < 1.0) {
					int[] points = new int[point1 - 1];
					for(int i = 0; i < point1 - 1; i++) {
						points[i] = paths[car1].get(1);
						paths[car1].remove(1);
					}
					paths[car2].addAll(1, points, point1 - 1);
				}
				
				double newFitness = calcFitness(paths, matrix, cars);
				
				// If a better solution in found, overwrite the previous best. In the case
				// of a worse solution, there is a chance that it'll still be accepted.
				// This prevents solutions getting stuck in a local minumum.
				// Acceptance rate lowers with temerature.
				if((newFitness < bestFitness) || (Math.exp((bestFitness - newFitness) / temp) > r.nextDouble())) {
					bestPaths = copyPaths(paths, cars);
					bestFitness = newFitness;
					if(newFitness > bestFitness)
						logger.info("Accepted!");
				}
				
				if(newFitness < bestestFitness) {
					bestestPaths = copyPaths(paths, cars);
					bestestFitness = newFitness;
				}
			}
			
			// If a new best solution is found, keep the temperature unchanged.
			if(bestestFitness < startFitness) {
				temp /= COOLING;
				logger.info("Temperature: " + temp + " / " + TEMP_FINISH);
				logger.info("Best fitness: " + bestestFitness);
			}
		}
		
		return bestestPaths;
	}
}