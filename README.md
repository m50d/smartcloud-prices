# Smartcloud instance prices API

## Assumptions

 - Since this is a realtime API, we should make an upstream call for every call rather than e.g. caching responses
    - In particular, in the case of an error we should propagate an error to our users rather than e.g. fall back to old data
 - Errors and status from the upstream API are safe to expose to users
 - The example call with a `kind` parameter is the only style we need to support 

## Design decisions
 
 - Library choice was guided by those in the existing project (e.g. ember client to match ember server, munit for testing).
 I updated to approximately current versions where this was necessary due to incompatibility with my JVM or other upgrades.  
 - I switched the existing "dummy" implementation of `InstanceKindService#getAll` for one that calls the actual upstream
 API; I'm not sure whether this was intended in the requirements or not, but I felt it could cause confusion if the
 `instance-kinds` endpoint returned a different list of instance kinds from those that are accepted by the `prices`
 endpoint.
 - I implemented a combined `SmartcloudService` rather than a specific `SmartcloudPriceService`, as I wanted to share
 code between the implementation of the price call and the instance kind call. I kept the `InstanceKindService` and 
 `InstanceDetailsService` interfaces separate so this is easy to change in the future if need be.
 - I followed the existing code structure arranged by "vertical" layer (`data`/`routes`/`services`). IMO it would be
 better to arrange the code by business/feature area.
 - I moved `Exception` into a common place and made an `ErrorHandling` "middleware" that recognises it. IME maintaining
 specific exception types for each service is usually an excessive amount of overhead for a small benefit, and having a
 shared concept of known error types across the application strikes a better balance.
    - I've tried to sketch out the idea of having separate handlers for "known" errors (subtypes of 
    `prices.services.Exception`, which we match on and handle specific cases individually)
    and unknown service exceptions (for which we log the error and show a generic error, as the detailed error might
    contain sensitive information). As written this middleware likely duplicates functionality that http4s itself
    provides, but in a real application this would provide a place to integrate e.g. custom alerting.
 - I implemented the transformation between the upstream data format and the downstream response format in a standalone
 `PricesService`, to show the "correct" place for it. In practice a standalone class for this one-liner is overkill,
 and in a "real" project I would likely include that logic in `PricesRoutes` until it grew more substantial and became
 worth factoring out.
 - In `PricesRoutesSuite` I've used the "real" implementation of `PricesService` and `ErrorHandling`, so this is
 something of an integration test. IMO it is good to do this where possible, as it makes for more realistic tests that
 can detect real errors, and in this case the overhead is not prohibitive.
    - There is some duplication of the dependency "wiring up" between `PricesRoutesSuite` and the real `Server` impl.
    Ideally I would look to avoid this, perhaps by using a lightweight cake style where the "wiring" code lives in
    traits that can be composed in both test and real, and/or by using an automatic wiring library such as macwire.
    - I haven't added any more specific tests of other services or "unit-style" tests of `PricesRoutes` with a stub
    `PricesService`. IMO there is currently ample test coverage given the low complexity of the code we're testing;
    more complex logic would warrant more tests to achieve a reasonable level of confidence in its correctness.
    For a "glue" service like this, misunderstanding the interface with the upstream service is a more likely source
    of errors, but unit testing cannot catch those errors as any stub impl will repeat those same misunderstandings.
    In a real deployment I would ideally want automated end-to-end tests that confirmed that this service interoperated
    correctly with the real upstream API, especially if the cloud provider offers a test endpoint that we can use.
    This would also serve to e.g. alert us if the upstream API changed in an incompatible way.
 - I followed the "final tagless" structure of existing classes parameterised by `F[_]`; I was hoping to get some value
 out of this by being able to use a simpler type like `SyncIO` or `Id` in the test class, but in fact 
 `Client.fromHttpApp` (http4s' recommended approach to testing) requires `Async` so `F` is always `IO`. So IMO while
 "business" services like `PricesService` may see some benefit from this style (it would be possible to implement e.g.
 a unit test of `PricesService` using `Id`), the `*Routes` classes will always use `IO` and it would probably be better
 to write them concretely using `IO`.

## How to run this code

(Tested with OpenJDK 21.0.2, SBT 1.10.0, and docker 26.1.4)

1. Follow the instruction at [smartcloud](https://hub.docker.com/r/smartpayco/smartcloud) to run the Docker container on your machine.
1. Run `sbt run`
1. The API should be running on your port 8080
````
$ curl http://localhost:8080/prices?kind=sc2-medium
{"kind":"sc2-medium","amount":0.452}
````