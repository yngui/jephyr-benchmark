# Zephyr benchmark

## Running the benchmark

Clone zephyr-benchmark repository

```
git clone https://github.com/yngui/zephyr-benchmark.git
```

Build the project

```
cd zephyr-benchmark
mvn install
```

Run the benchmark

```
cd zephyr-benchmark
java -jar target/zephyr-benchmark.jar \
     -jvmArgsAppend "-Xbootclasspath/p:jsr166e.jar -DworkerCount=503 -DringSize=1000000 -Dparallelism=4 -javaagent:quasar-core-0.7.3.jar" \
     -wi 5 -i 10 -bm avgt -tu ms -f 5 .*RingBenchmark
```
