play-microservice
=================

Every application in the MDTP is conceptually considered to be a microservice. It may be a microservice with or without templates, i.e. a Frontend application or a REST Api.

This library is dependent on Play! 2.3.x or higher

### Features
* **Scheduling** A given function can be run periodically in the background by mixing in ```RunningOfScheduledJobs``` to the service's ```Global```
* **Test matchers** A number of ScalaTest have matchers that allow http response statuses to be checked
* **Admin endpoints** A number of useful controller methods (intended to be mapped under the ```/admin/``` path) that allow the service to be pinged, healthchecked and useful metadata to be queried.

### Add as a dependency

For scala 2.11.x versions +2.0.0
```scala
"uk.gov.hmrc" % "play-microservice" % "2.0.0"
```

For scala 2.10.x
```scala
"uk.gov.hmrc" % "play-microservice" % "1.22.0"
```

### Collaborators

Find a list of the collaborators [here](https://github.tools.tax.service.gov.uk/HMRC/play-microservice/blob/master/project/HmrcBuild.scala#L80)

