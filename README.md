## To run the test

Just run the single test case. You'll get a failure saying the search result is outside of the `within()` parameter
in 5 minutes or earlier.

~~~
$ mvn test
Tests run: 2, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 181.127 sec <<< FAILURE!
putPeriodicallyAndCheckResults(com.example.BrokenWithinTest)  Time elapsed: 180.04 sec  <<< FAILURE!
java.lang.AssertionError: [taxi-10,1.343311,103.661969] is 20.98496260319283 distant, more than 2.5 (km)
        at org.junit.Assert.fail(Assert.java:88)
        at com.example.BrokenWithinTest.putPeriodicallyAndCheckResults(BrokenWithinTest.java:109)
        ...
~~~

You may want to add JVM options like `-DargLine="-Xmx1g -verbose:gc"`.


## To test on another version

Edit `<version.org.infinispan>` property in the `pom.xml`.
On JDG 7, you need to comment out `.onDefaultCoordinates()` method in the test case as well.


### To try the patch on JDG 7.1.0

Firstly get the artifact, `infinispan-embedded-query-8.4.0.Final-redhat-2-jdg-1020.jar`, from Red Hat Support.
Then install it into your local Maven repository.

~~~
$ mvn install:install-file \
    -Dfile=/path/to/infinispan-embedded-query-8.4.0.Final-redhat-2-jdg-1020.jar \
    -DgroupId=org.infinispan -DartifactId=infinispan-embedded-query \
    -Dversion=8.4.0.Final-redhat-2-jdg-1020 -Dpackaging=jar
~~~
