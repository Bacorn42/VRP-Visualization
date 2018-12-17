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
	for(let i = 0; i < paths.length; i++)
	  candidates.push(paths[i][paths[i].length - 1]);
	  
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