var map;
var markersLayer = new L.LayerGroup();
var routesLayer = new L.LayerGroup();
const url = "http://localhost:8989";

// Gets bounds of map and initializes it
document.addEventListener("DOMContentLoaded", function(e) {
  fetch(url + "/info")
  .then(response => response.json())
  .then(responseJSON => initMap(responseJSON.bbox));
});

// Creates a map in the div and draws the bounding rectangle
function initMap(bounds) {
  var layer = L.tileLayer('https://maps.omniscale.net/v2/mapsgraph-bf48cc0b/style.default' + (L.Browser.retina ? '/hq.true' : '') + '/{z}/{x}/{y}.png', {
    layers: 'osm',
    attribution: '&copy; <a href="http://www.openstreetmap.org/copyright" target="_blank">OpenStreetMap</a> contributors, &copy; <a href="https://maps.omniscale.com/">Omniscale</a>'
  });
  map = L.map('map', {
    layers: [layer, markersLayer, routesLayer]
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
// with the first (starting) point being roughly in the center of Warsaw at [52.23, 21]
document.getElementById('randBut').addEventListener('click', function() {
  var points = parseInt(document.getElementById('points').value);
  var textarea = document.getElementById('text');
  textarea.value = "52.23,21\n";
  for(let i = 0; i < points - 1; i++) {
    let lat = Math.round((Math.random() * 0.4 + 52.03) * 10000) / 10000;
    let lon = Math.round((Math.random() * 0.5 + 20.75) * 10000) / 10000;
    textarea.value += lat + "," + lon + "\n";
  }
});

// Clears the previous markers and routes.
// Calls the custom VRP API with all the points and numbr of cars.
// The calculations are all done on the server.
document.getElementById('searchButtonX').addEventListener('click', function() {
  var inputs = document.getElementById('text').value.trim().split('\n');
  var cars = parseInt(document.getElementById('cars').value);
  
  markersLayer.clearLayers();
  routesLayer.clearLayers();
  
  drawMarkers(inputs);
  
  var requestUrl = url + "/vrp/?";
  for(let i = 0; i < inputs.length; i++)
    requestUrl += "p=" + inputs[i] + "&";
  requestUrl += "c=" + cars;
  
  fetch(requestUrl)
  .then(response => response.json())
  .then(pathsObj => getRoutes(pathsObj, cars));
});

// Draws a marker on each location a vehicle has to go through.
function drawMarkers(inputs) {
  for(let coord of inputs) {
    let coords = coord.split(',');
    let marker = L.marker([parseFloat(coords[0]), parseFloat(coords[1])]);
	marker.setOpacity(0.5);
	markersLayer.addLayer(marker);
  }
}

// Extracts arrays of coordinates from the JSON object and draws the routes.
function getRoutes(pathsObj, cars) {
  var routes = [];
  for(let i = 0; i < cars; i++)
    routes.push(pathsObj.paths[i].points.coordinates);
  drawRoutes(routes);
}

// TODO: Add more colors.
//
// Draws the routes with a different color for each vehicle.
// Index description:
// i - route for vehicle (made of many points)
// j - coordinate of line point
function drawRoutes(routes) {
  const colors = ['#882288', '#887711', '#229922', '#113377', '#999999', '#119999', '#555555', '#000000', '#005500', '#654321'];
  for(let i = 0; i < routes.length; i++) {
	let points = [];
	for(let j = 0; j < routes[i].length; j++)
      points.push(new L.LatLng(routes[i][j][1], routes[i][j][0]));
		
    let polyline = new L.Polyline(points, {
      color: colors[i],
      weight: 5,
      opacity: 1,
      smoothFactor: 1,
      fill: false
	});
    polyline.addTo(routesLayer);
  }
}