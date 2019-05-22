/**
 *
 *    Copyright 2018-2019 Optimizely and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab.config.parser;

import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.DatafileProjectConfigTestUtils;
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
        jsonV2 = DatafileProjectConfigTestUtils.validConfigJsonV2();
        jsonV3 = DatafileProjectConfigTestUtils.validConfigJsonV3();
        jsonV4 = DatafileProjectConfigTestUtils.validConfigJsonV4();
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
