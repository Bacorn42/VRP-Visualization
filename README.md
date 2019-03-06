# VRP visualizer
Web application for visualizing the Vehicle Routing Problem (Multiple TSP variant) solutions.
It uses a Greedy Algorithm or a Simulated Annealing algorithm that takes the distance matrix and the amount of vehicles and gives the paths each vehicle takes. 
Next, it queries the server to obtain the route points for those paths. 

Finally, Leaflet is used to draw the routes in various colors to distinguish the different vehicles.  
The points are randomly generated over the area of Warsaw between the coordinates [52.16, 20.9] and [52.3, 21.1]. 
The vehicles' starting point is located roughly in the middle of Warsaw.

## Quick Start
1. Enter the number of locations, click "Randomize Points" to calculate given number of random points.
2. Enter the number of cars possible for utilization.
3. Click the "Calculate!" button to calculate routes.
4. Calculated routes should appear on the map after calculations, which take longer depending on given number of points. 

## Credits
Kacper Leszczyński
Youssef Ibrahim
Igor Sałuch
