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

import com.optimizely.ab.processor.Pipeline.Pipe;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class PipeTest {
    private final Stage<Integer, Integer> incrementStage = Stage.mapping(x -> x + 1);
    private final Stage<Integer, Integer> squareStage = Stage.mapping(x -> x * x);
    private final Stage<Integer, Integer> cubeStage = Stage.mapping(x -> x * x * x);

    @Test
    public void nil() {
        assertThat(Pipe.nil(), sameInstance(Pipe.nil()));
        assertThat(Pipe.nil().isEmpty(), is(true));
    }

    @Test
    public void start() {
        Pipe<Integer, Integer, Integer> pipe = Pipe.<Integer, Integer>nil().to(incrementStage);
        assertThat(pipe.get(), sameInstance(incrementStage));
        assertThat(pipe.stages(), contains(incrementStage));
        assertThat(pipe.prev(), equalTo(Pipe.nil()));
    }

    @Test
    public void into() {
        Pipe<Integer, Integer, Integer> parent = Pipe.of(incrementStage);
        Pipe<Integer, Integer, Integer> pipe = parent.to(squareStage);
        assertThat(pipe.get(), sameInstance(squareStage));
        assertThat(pipe.stages(), contains(incrementStage, squareStage));

    }

    @Test
    public void immutable() {
        Pipe<Integer, Integer, Integer> parent = Pipe.of(incrementStage);
        Pipe<Integer, Integer, Integer> child1 = parent.to(squareStage);
        Pipe<Integer, Integer, Integer> child2 = parent.to(cubeStage);

        assertThat(parent.stages(), contains(incrementStage));
        assertThat(child1.stages(), contains(incrementStage, squareStage));
        assertThat(child2.stages(), contains(incrementStage, cubeStage));
    }
}