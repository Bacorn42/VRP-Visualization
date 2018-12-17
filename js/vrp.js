// Simple greedy algorithm for solving the VRP problem
// 
// Input:
// matrix - the graph adjacency matrix
// cars - the number of vehicle to route
// 
// Output:
// paths - sequence of points for each vehicle
function vrp(matrix, cars) {
  var paths = [];
  for(let i = 0; i < cars; i++)
    paths.push([0]);
  
  var visited = [0];
  
  while(visited.length != matrix.length) {
    let from = 0;
    let to = 0;
    let minVal = Number.MAX_SAFE_INTEGER;
    let car = 0;
    let candidates = [];
	
	// Candidates are points that a vehicle is currently on (from)
	for(let i = 0; i < paths.length; i++)
	  candidates.push(paths[i][paths[i].length - 1]);
	  
	// Finding the lowest cost between a candidate and an unvisited point
	for(let i = 0; i < candidates.length; i++) {
	  for(let j = 0; j < matrix.length; j++) {
	    if(visited.indexOf(j) == -1) {
		  if(matrix[candidates[i]][j] < minVal) {
		    minVal = matrix[candidates[i]][j];
			from = candidates[i];
			to = j;
			car = i;
		  }
		}
	  }
	}
	paths[car].push(to);
	visited.push(to);
  }
  
  return paths;
}