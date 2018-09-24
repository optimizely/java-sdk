package com.optimizely.ab.config.parser;

import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.ProjectConfigTestUtils;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
@Warmup(iterations = 10)
@Measurement(iterations = 20)
@State(Scope.Benchmark)
public class JacksonConfigParserBenchmark {
    JacksonConfigParser parser;
    String jsonV2;
    String jsonV3;
    String jsonV4;

    @Setup
    public void setUp() throws IOException {
        parser = new JacksonConfigParser();
        jsonV2 = ProjectConfigTestUtils.validConfigJsonV2();
        jsonV3 = ProjectConfigTestUtils.validConfigJsonV3();
        jsonV4 = ProjectConfigTestUtils.validConfigJsonV4();
    }

    @Benchmark
    public ProjectConfig parseV2() throws ConfigParseException {
        return parser.parseProjectConfig(jsonV2);
    }

    @Benchmark
    public ProjectConfig parseV3() throws ConfigParseException {
        return parser.parseProjectConfig(jsonV3);
    }

    @Benchmark
    public ProjectConfig parseV4() throws ConfigParseException {
        return parser.parseProjectConfig(jsonV4);
    }
}
