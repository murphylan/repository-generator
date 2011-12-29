Repository Generator
====================

Generates a fake repository of jars and poms.
Jars are empty (they contain one empty `/test` directory.
poms are valid but minimal.

No metadata or hashes are generated.

How to run
----------

1.) mvn exec:java
This uses `src/main/resources/repository-generator.properties` 

2.) Self executing jar:
`java -jar repository-generator-<version>.jar [path to repository-generator.properties]`

See `src/main/resources/repository-generator.properties` for more info.
