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
		
		public int get(int i) {
			return this.arr.get(i);
		}
		
		public int getSize() {
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
		@QueryParam("point") List<GHPoint> requestPoints,
		@QueryParam("cars") int cars
	) {
		int points = requestPoints.size();
		double[][] matrix = new double[points][points];
		
		buildMatrix(matrix, requestPoints);
		
		ArrayListWrapper[] paths = vrpGreedy(matrix, cars, points);	
		
		PointList[] routes = new PointList[cars];
		
		logger.info("Getting routes...");
		
		for(int i = 0; i < cars; i++) {
			logger.info("Getting route for car " + (i+1) + "/" + cars);
			routes[i] = new PointList(0, false);
			for(int j = 0; j < paths[i].getSize() - 1; j++) {
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
				candidates.add(paths[i].get(paths[i].getSize() - 1));
		  
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
		
		return paths;
	}
}