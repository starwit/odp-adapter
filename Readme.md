# Open Data Platform Adapter

This software implements an adapter to Wolfsburg's open data platform. It will update available parking spots for configured parking space

## Functions
Adapter implements the following functions:
* Get OffStreetParking status
* Set OffStreetParking status

## Configuration
App can be configured via application.properties file. Just on parking area is supported and it's configuration is also done via application properties. Next to all Spring Boot config the following keys can be used:
```
# user name for ODP login
odp.auth.username=meckauer 
# password for ODP login
odp.auth.password=secret 
# URL to get auth token
odp.auth.url=https://auth.staging.wolfsburg.digital/auth/realms/default/protocol/openid-connect/token

# URL to read & update parking space data
odp.parking.url=https://api.staging.wolfsburg.digital/context/v2/entities/
# ID for the one currently supported parking space
odp.parking.meckauer.id=OffStreetParking-Pkpd-787878
```

## Build & Run
```bash
    mvn clean package
    java -jar run target/odp-adapter-1.0-SNAPSHOT.jar
```

## License & Contribution
This software is published under the AGPLv3 and the license agreement can be found [here](/LICENSE). Pull requests are very much appreciated and you contributed code will be licensed as AGPLv3 as well.