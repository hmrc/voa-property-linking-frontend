voa-property-linking-frontend 
=============
A frontend service for the CCA (Business Rates) project at VOA. It communicates with voa-property-linking, and business-rates-authorisation microservices.
 
# Installation
Running the service is sensitive to installed versions of node/npm

Recommended versions:
* node: v8.5.0
  
* npm: v5.3.0 

  
Use 'nvm' to manage multiple versions of node/npm.

see:
* https://stackoverflow.com/questions/7718313/how-to-change-to-an-older-version-of-node-js
### Cloning:

SSH
```
git@github.com:hmrc/voa-property-linking-frontend.git
```
HTTPS
```
https://github.com/hmrc/voa-property-linking-frontend.git
```
### Running the application

Ensure that you have the latest versions of the required services and that they are running. This can be done via service manager using the BUSINESS_RATES_ALL profile
```
sm --start BUSINESS_RATES_ALL -v
sm --stop VOA_PROPERTY_LINKING_FRONTEND
```
* `cd` to the root of the project.
* `sbt run`
* In your browser navigate to [localhost:9523/business-rates-property-linking](http://localhost:9523/business-rates-property-linking)


### Found a bug?

* Please raise an issue by selecting Issues near the top right hand side of this page.
* Add comments, logs and screenshots where possible.
