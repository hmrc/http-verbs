http-verbs
==========

[![Build Status](https://travis-ci.org/hmrc/http-verbs.svg)](https://travis-ci.org/hmrc/http-verbs) [ ![Download](https://api.bintray.com/packages/hmrc/releases/http-verbs/images/download.svg) ](https://bintray.com/hmrc/releases/http-verbs/_latestVersion)

http-verbs is a Scala library providing an interface to make asynchronous HTTP calls. 

This library implements parts of [hmrc/http-core](https://github.com/hmrc/http-core) that are concerned with business logic side of calling other HTTP services on the HMRC Tax Platform, including:

  * executing hooks
  * mapping errors
  
  

## Adding to your build

In your SBT build add:

```scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies += "uk.gov.hmrc" %% "http-verbs" % "x.x.x"
```

## Usage

All examples are available here:[hmrc/http-verbs-example](https://github.com/hmrc/http-verbs-example)

## License ##
 
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
