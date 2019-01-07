# VRP visualizer
Web application for visualizing the Vehicle Routing Problem solutions.
**The project is under heavy development.**
Currently it uses a Greedy Algorithm that takes the distance matrix and the amount of vehicles and gives the paths each vehicle takes. 
Next, it queries the server to obtain the route points for those paths. 

Finally, Leaflet is used to draw the routes in various colors to distinguish the different vehicles.  
The points are randomly generated over the area of Warsaw between the coordinates [52.16, 20.9] and [52.3, 21.1]. 
The vehicles starting point is located roughly in the middle of Warsaw.

Plans include improved VRP algorithms in order to reduce total distance (for example, simulated annealing). 

## Quick Start
1. Enter the number of locations on the sidebar, click a button to calculate given number of random points.
2. Click "Route!" button to calculate routes.
3. Calculated route should appear on the map after calculations which take longer depending on given number of points. 
