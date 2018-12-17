var map;
const url = "http://localhost:8989";

// Gets bounds of map and initializes it
document.addEventListener("DOMContentLoaded", function(e) {
	var bounds;
	fetch(url + "/info")
	.then(function(response) {
	  return response.json();
	})
	.then(function(responseJSON) {
	  bounds = responseJSON.bbox;
	  initMap(bounds);
	})
});

// Creates a map in the div and draws the bounding rectangle
function initMap(bounds) {
  var layer = L.tileLayer('https://maps.omniscale.net/v2/mapsgraph-bf48cc0b/style.default' + (L.Browser.retina ? '/hq.true' : '') + '/{z}/{x}/{y}.png', {
    layers: 'osm',
    attribution: '&copy; <a href="http://www.openstreetmap.org/copyright" target="_blank">OpenStreetMap</a> contributors, &copy; <a href="https://maps.omniscale.com/">Omniscale</a>'
  });
  map = L.map('map', {
    layers: [layer]
  });

  map.fitBounds(new L.LatLngBounds(new L.LatLng(bounds[1], bounds[0]), new L.LatLng(bounds[3], bounds[2])));
  
  var myStyle = {
        color: 'black',
        weight: 2,
        opacity: 0.3
    };
  var geoJson = {
    type: "Feature",
    geometry: {
      type: "LineString",
      coordinates: [
        [bounds[0], bounds[1]],
        [bounds[2], bounds[1]],
        [bounds[2], bounds[3]],
        [bounds[0], bounds[3]],
        [bounds[0], bounds[1]]
      ]
    }
  };
  L.geoJson(geoJson, {
    style: myStyle
  }).addTo(map);
}

// Generates a number of random points between coordinates [52.16, 20.9] and [52.3, 21.1]
// with the first (starting) point being roughly in the center of Warsaw
document.getElementById('randBut').addEventListener('click', function() {
  var points = parseInt(document.getElementById('points').value);
  var textarea = document.getElementById('text');
  textarea.value = "52.23,21\n";
  for(let i = 0; i < points - 1; i++) {
    let lat = Math.round((Math.random() * 0.14 + 52.16) * 10000) / 10000;
	let lon = Math.round((Math.random() * 0.2 + 20.9) * 10000) / 10000;
	textarea.value += lat + "," + lon + "\n";
  }
});

// TODO: Convert this code from client-side to server-side
//
// First makes n(n - 1) AJAX calls to find distances between all points.
// Once all dinstances are aquired, call the VRP algorithm on the distance matrix.
// Finally, from the solution aquire the routes and draw them.
document.getElementById('searchButtonX').addEventListener('click', function() {
  console.log("Getting data.");
  var inputs = document.getElementById('text').value.trim().split('\n');
  var matrix;
  var coords = [];
  for(let i = 0; i < inputs.length - 1; i++) {
    coords.push(inputs[i]);
  }
  var promises = [];
  matrix = Array(inputs.length - 1);
  for(let i = 0; i < matrix.length; i++)
    matrix[i] = Array(inputs.length - 1);
  for(let i = 0; i < matrix.length; i++)
    matrix[i][i] = 0;
  console.log("Finding distance matrix.");
  for(let i = 0; i < matrix.length; i++)
    for(let j = 0; j < matrix.length; j++) {
	  if(i != j) {
	    promises.push(fetch("http://localhost:8989/route/?point=" + coords[i] + "&point=" + coords[j])
		.then(function (response) {
		  return response.json();
		})
		.then(function (responseJSON) {
		  matrix[i][j] = responseJSON.paths[0].distance;
		}));
	  }
	}
  Promise.all(promises)
  .then(function (values) {
    console.log("FInding VRP solution.");
	var cars = parseInt(document.getElementById('cars').value);
    var paths = vrp(matrix, cars);
	console.log("Getting routes.");
	getRoutes(paths, coords)
	.then(function (r) {
	  console.log("Drawing routes.");
	  drawRoutes(r);
	  console.log("Finished successfully.");
	});
  });
});

// TODO: Convert this code from client-side to server-side
//
// Converts the vehicle paths from graph form to actual map routes.
function getRoutes(paths, coords) {
  return new Promise( function(resolve, reject) {
	  routes = Array(paths.length);
	  var promises = [];
	  for(let i = 0; i < paths.length; i++)
		routes[i] = Array(paths[i].length - 1);
	  for(let i = 0; i < paths.length; i++)
		for(let j = 0; j < paths[i].length - 1; j++)
		  promises.push(fetch("http://localhost:8989/route/?point=" + coords[paths[i][j]] + "&point=" + coords[paths[i][j+1]] + "&points_encoded=false")
		  .then(function (response) {
			return response.json();
		  })
		  .then(function (responseJSON) {
			routes[i][j] = responseJSON.paths[0].points.coordinates;
		  }));
	  Promise.all(promises)
	  .then(function (values) {
		 resolve(routes);
	  });
  });
}

// TODO: Add more colors.
//
// Draws the routes with a different color for each vehicle.
// Index description:
// i - route for vehicle (made of many points)
// j - route between 2 path points (made of many lines)
// k - coordinate of line point
function drawRoutes(routes) {
  const colors = ['#882288', '#887711', '#117711', '#113377', '#666666', '#119999'];
  for(let i = 0; i < routes.length; i++) {
    for(let j = 0; j < routes[i].length; j++) {
	  let points = [];
	  for(let k = 0; k < routes[i][j].length; k++)
	    points.push(new L.LatLng(routes[i][j][k][1], routes[i][j][k][0]));
		
	  var polyline = new L.Polyline(points, {
	    color: colors[i],
		weight: 5,
		opacity: 1,
		smoothFactor: 1,
		fill: false
	  });
	  polyline.addTo(map);
	}
  }
}