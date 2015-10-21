# Jephyr benchmark

## Running the benchmark

Clone jephyr-benchmark repository

```
git clone https://github.com/yngui/jephyr-benchmark.git
```

Build the project

```
cd jephyr-benchmark
mvn install
```

Run the benchmark

```
cd jephyr-benchmark
java -jar target/jephyr-benchmark.jar \
     -jvmArgsAppend "-Xbootclasspath/p:jsr166e.jar -DworkerCount=503 -DringSize=1000000 -Dparallelism=4 -javaagent:quasar-core-0.7.3.jar" \
     -wi 5 -i 10 -bm avgt -tu ms -f 5 .*RingBenchmark
```
