
CO₂ Emission Calculator

A command-line application built with Java 17 and Spring Boot that calculates CO₂-equivalent emissions for trips between two cities, based on the chosen transportation method.
It integrates with the OpenRouteService API for distance calculation.

⸻

Prerequisites
•	Java 17
•	Maven 3.8+
•	OpenRouteService API Token

⸻

Setup
1.	Clone the repository.
2.	Configure your OpenRouteService API Token in both configuration files:
•	src/main/resources/application.properties
•	src/test/resources/application-test.properties

such as 

ORS_TOKEN=your_token_here


⸻

Build

mvn clean clean
mvn clean package
mvn clean package -DskipTests


⸻


java -jar target/sap-0.0.1-SNAPSHOT.jar --start "Hamburg" --end "Frankfurt" --transportation-method diesel-car-medium

### Notes
- Parameters can be passed using either spaces or `=`.
- Cities with spaces must be quoted.
- And no route found between the two cities getting 0 output.

⸻

Example Usage

Run the application with your desired cities and transportation method:

java -jar target/sap-0.0.1-SNAPSHOT.jar --start "Hamburg" --end "Frankfurt" --transportation-method diesel-car-medium

Sample Output

Your trip caused 320.4kg of CO2-equivalent.
 [Watch Demo Video](https://drive.google.com/file/d/1Wl_b1XfYc3NkGLejqu23lAkybjzb2xyN/view)

⸻

Supported Transportation Methods

The allowed transportation methods are defined in **TransportMethod.java** (per requirement specification).
If you want to add a new method, update this file.

Currently Supported Transportation Methods
•	diesel-car-small
•	petrol-car-small
•	plugin-hybrid-car-small
•	electric-car-small
•	diesel-car-medium
•	petrol-car-medium
•	plugin-hybrid-car-medium
•	electric-car-medium
•	diesel-car-large
•	petrol-car-large
•	plugin-hybrid-car-large
•	electric-car-large
•	bus-default

⸻



⸻

Error Handling

The application validates inputs and provides clear error messages for:
•	Missing parameters

Usage: --start <City> --end <City> --transportation-method <method>


	•	Blank city names

City name must not be blank.


	•	Unsupported transportation methods

Unknown transportation method: xyz-method


	•	API failures (403, 5xx, network issues)

Detailed error messages with city names.



⸻


mvn test



