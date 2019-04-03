/**
 *    Copyright 2019, Optimizely Inc. and contributors
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
package com.optimizely.ab.processor;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

public class PipelineTest {
    private final Stage<Integer, Integer> incrementStage = Stage.mapping(x -> x + 1);
    private final Stage<Integer, Integer> squareStage = Stage.mapping(x -> x * x);
    private final Stage<Object, String> toStringStage = Stage.mapping(Object::toString);
    private final Stage<Object, String> emphasisStage = Stage.mapping(o -> o.toString() + "!");

    @Test
    public void testSingleStage() {
        Pipeline<Integer, String> pipeline = Pipeline.Pipe.<Integer, String>of(toStringStage).toPipeline();

        assertThat(pipeline.getStages(), hasSize(1));
        assertThat(pipeline.getStages(), contains(toStringStage));

        List<String> output = new ArrayList<>();
        pipeline.configure(output::add);

        pipeline.process(1);
        assertThat(output, contains("1"));

        pipeline.process(2);
        assertThat(output, contains("1", "2"));
    }

    @Test
    public void testMultiStage() {
        Pipeline<Integer, String> pipeline = Pipeline.Pipe
            .of(incrementStage)
            .to(squareStage)
            .to(toStringStage)
            .to(emphasisStage)
            .toPipeline();

        assertThat(pipeline.getStages(), contains(
            incrementStage,
            squareStage,
            toStringStage,
            emphasisStage
        ));

        List<String> output = new ArrayList<>();
        pipeline.configure(output::add);

        pipeline.process(0);
        assertThat(output, contains("1!"));

        pipeline.process(1);
        assertThat(output, contains("1!", "4!"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void detectsCycle() {
        Pipeline.Pipe.of(squareStage)
            .to(incrementStage)
            .to(squareStage)
            .toPipeline();
    }
}