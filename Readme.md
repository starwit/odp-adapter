# Open Data Platform Adapter

This software implements an adapter to Wolfsburg's open data platform. It will update available parking spots for configured parking space.

## Functions
Adapter implements the following functions:
* Get OffStreetParking status
* Set OffStreetParking status

## Configuration
App can be configured via application.properties file. Just on parking area is supported and it's configuration is also done via application properties. Next to all Spring Boot config the following keys can be used:
```
# base path for api
rest.base-path=api
# if true update starts immediately
config.autostart=true

# user name for ODP login
odp.auth.username=meckauer 
# password for ODP login
odp.auth.password=secret 
# URL to get auth token
odp.auth.url=https://auth.staging.wolfsburg.digital/auth/realms/default/protocol/openid-connect/token

# URL to read & update parking space data
odp.parking.url=https://api.staging.wolfsburg.digital/context/v2/entities/

# Configure mapping to parking area
odp.parkingareaid=OffStreetParking-Pkpd-787878
analytics.observation_area=12
```

## Build & Run
Adapter is build as a Spring Boot application, that runs an update job with a fixed schedule. Building and running can be done like so:
```bash
    mvn clean package
    java -jar run target/odp-adapter-1.0-SNAPSHOT.jar
```

### Background Wolfsburg's Open Data Platform

Wolfsburg's open data platform follows the Fiware standard and here is some background doc, how this platform can be used.

Get access token:
```bash
curl -H application/x-www-form-urlencoded -d "realm=default" -d "client_id=api" -d "scope=entity:read entity:write" -d "username=username" -d "password=PASSWORD" -d "grant_type=password" "https://auth.staging.wolfsburg.digital/auth/realms/default/protocol/openid-connect/token"
```

Get latest OffStreetParking data
```bash
curl --location 'https://api.staging.wolfsburg.digital/context/v2/entities/OffStreetParking-Pkpd-787878/' -H 'fiware-ServicePath: /ParkingManagement' -H 'fiware-service: Wolfsburg' -H 'Authorization: Bearer TOKEN'
```

Update OffStreetParking data
```bash
curl --location --request PATCH 'https://api.staging.wolfsburg.digital/context/v2/entities/OffStreetParking-Pkpd-787878/attrs/' \
--header 'fiware-ServicePath: /ParkingManagement' \
--header 'fiware-service: Wolfsburg' \
--header 'content-type: application/json' \
 -H 'Authorization: Bearer Token ' \
--data '{
    "availableSpotNumber": {
        "type": "Integer",
        "value": 55
    }
}'
```

Query ParkingSpots
```bash
curl --location 'https://api.staging.wolfsburg.digital/context/v2/entities?type=ParkingSpot' -H 'fiware-ServicePath: /ParkingManagement/Meckauer' -H 'fiware-service: Wolfsburg' -H 'Authorization: Bearer '
```

## License & Contribution
This software is published under the AGPLv3 and the license agreement can be found [here](/LICENSE). Pull requests are very much appreciated and you contributed code will be licensed as AGPLv3 as well.