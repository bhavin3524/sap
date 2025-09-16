# CO2 Emission Calculator

This is a command-line tool written in Java (Spring Boot) to calculate CO2-equivalent emissions for travel between two cities using a specific transportation method.

## Prerequisites

- Java 17
- Maven
- [OpenRouteService API token](https://openrouteservice.org/dev/#/signup)

## Setup

1. Clone the repository.
2. Set the environment variable `ORS_TOKEN` with your API token.

### Linux/macOS

```sh
export ORS_TOKEN=your_token_here
```

### Windows

```cmd
set ORS_TOKEN=your_token_here
```

## Build

```sh
mvn clean package
```

## Run

```sh
java -jar target/co2-calculator-1.0.0.jar --start Hamburg --end Berlin --transportation-method diesel-car-medium
```

- You can use space or `=` between parameters.
- Cities with spaces should be quoted.

## Example

```sh
java -jar target/co2-calculator-1.0.0.jar --start "Los Angeles" --end "New York" --transportation-method=electric-car-large
```

## Test

```sh
mvn test
```

## Supported Transportation Methods

- diesel-car-small
- petrol-car-small
- plugin-hybrid-car-small
- electric-car-small
- diesel-car-medium
- petrol-car-medium
- plugin-hybrid-car-medium
- electric-car-medium
- diesel-car-large
- petrol-car-large
- plugin-hybrid-car-large
- electric-car-large
- bus-default
- train-default

## Error Handling

- Invalid or missing parameters
- Unknown transportation method
- API failures

## License

MIT