# VRP visualizer
Visualizing the Vehicle Routing Problem solutions.

## Introduction
Web application that provides and displays solutions to the Vehicle Routing Problem 
(finding the shortest path for a fleet of vehicles to go through a set of points). 
There will be one starting point for the vehicles 
(the depots for delivery companies for example) and any number (up to, say 1000) 
of points to cover.  
It can be thought of as a simulation of a delivery company calculating a route for the day.  

## Usage
All user interaction is meant to be happening in a sidebar on the left side of the web app
page or the map on the remaining part.

To calculate the solution:

1. Select the start point using a map or the search box.
2. Enter the number of vehicles.
3. Enter destinations using a map or the search box.
4. Select an algorithm used to calculate routes.
5. Click "Route!" button to perform the calculation.

Calculated routes should appear on the map after calculations which take longer depending 
on number of points. 
Distances should appear on the sidebar.

## Technical details

### Server 
#### Graphhopper
The application runs on a local [Graphhopper](https://github.com/graphhopper/graphhopper)
server which stores road data of the Mazowieckie Voivodeship 
(it can be easily changed though, see
[the documentation of the Graphhopper server](https://github.com/graphhopper/graphhopper/blob/master/docs/core/quickstart-from-source.md)
). 
That allows for easy calculations with real road data in mind.
All computation is done on the server side.

The server has few modifications in order to enable ability to perform calculations
on matrices and implement algorithms used to calculate solutions.
The server modifications (algorithms and ways of performing calculations) 
are contained in the file `../graphhopper/VRPResource.java`, which is meant to be in
`web-bundle/src/main/java/com/graphhopper/resources`
**of** [**the Graphhopper source**](https://github.com/graphhopper/graphhopper)
and enabled in the file
`web-bundle/src/main/java/com/graphhopper/http/GraphHopperBundle.java`,
also **in** [**the Graphhopper source**](https://github.com/graphhopper/graphhopper).

#### Algorithms
There are a few algorithms for the user to choose from to find the most suitable solution,
ranging from the fast, but rather inefficient greedy algorithm, 
to more sophisticated algorithms that will take more time to compute, 
but provide better solutions, for example, _simulated annealing_ to reduce total distance.

### Client 
#### Web app
The web app is built using technologies such as HTML5, CSS, JavaScript and Leaflet.
It consists of a simple sidebar with controls used to input data for the calculations
and a Leaflet map widget which covers the most of the web app area.
That part is used to help in providing input data and showing the output.

All web app JavaScript code is containted in the file `../js/main.js` and the main HTML file
is in `../index.html`. Web app CSS data is in the file `../css/styles.css`,

#### Leaflet
A local [Leaflet](https://leafletjs.com) instance is used to show interactive map, 
destination and starting points, and the results as a colored lines to distinguish 
paths for different vehicles.
The local approach allows for easy modifications, resistance to changes that could break
the functionality, better performance, since nothing needs to be downloaded from 
the remote servers, but on the other hand, needs constant updating in order to keep up
with the updates.
The Leaflet instance did not need any modifications for this implementation.
[OpenStreetMap](https://www.openstreetmap.org) is used as a map layer provider.
