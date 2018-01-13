# imagemagick

Execute `sbt showCoverage` to execute the tests and see the coverage report in the browser.

(`showCoverage` is an alias for multiple sbt commands, check the `build.sbt` file for more info)

## A few notes

- For readability's sake I did not use any logging library, but if necessary I would have used slf4j + logback
- Certain values could be externalized, like the default origin and tmp folders, the S3 location and the parallelism settings,
for that purpose I would use typesafe config with pureconfig to load it into a case class
- I did not write an integration test as it does not make sense since the assumption is that a crucial part of the application is mocked out
- There is no fancy error handling like retrying failed steps or propagating/aggregating the errors
- The errors in the returned disjunctions (`GeneralError Either T`) are simple strings, in a real application I would define dedicated error types
- The used stream approach is in my opinion perfectly fine for a simple flow (no feedbacks, retries, sophisticated zipping/merging/broadcasting),
for anything more complex I would use the Graph DSL to define more sophisticated data flows
- Certain parts of the code contain explanations of the used approach
- The ExecutionContext in which the IO operations run is supplied as an implicit parameter to the ImageProcessor on class level, for a more fine grained control of the IO processes each ImageFileService method could be written to expect an ExecutionContext as a parameter aswell
