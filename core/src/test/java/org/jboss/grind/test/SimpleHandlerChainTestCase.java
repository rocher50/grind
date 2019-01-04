/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.grind.test;

import static org.junit.Assert.assertEquals;

import org.jboss.grind.Grind;
import org.jboss.grind.GrindException;
import org.jboss.grind.GrindFactory;
import org.jboss.grind.PhaseHandler;
import org.jboss.grind.PhaseRegistration;
import org.jboss.grind.ProcessContext;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class SimpleHandlerChainTestCase {

    public static class Input1 {
        final String input1;

        public Input1(String input1) {
            this.input1 = input1;
        }
    }

    public static class Input2 {
        final String input2;

        public Input2(String input2) {
            this.input2 = input2;
        }
    }

    @Test
    public void mainTest() throws Exception {

        final Grind grind = GrindFactory.getInstance()
                .addPhase(new PhaseHandler() {
                    @Override
                    public void register(PhaseRegistration registration) throws GrindException {
                        registration.providesOutcome(TestResult.class);
                        registration.requiresInput(Input1.class);
                    }
                    @Override
                    public void process(ProcessContext ctx) throws GrindException {
                        final Input1 input1 = ctx.resolveOutcome(Input1.class);
                        ctx.pushOutcome(TestResult.class, new TestResult(input1.input1));
                    }
                    })
                .addPhase(new PhaseHandler() {
                    @Override
                    public void register(PhaseRegistration registration) throws GrindException {
                        registration.providesOutcome(Input1.class);
                        registration.requiresInput(Input2.class);
                    }

                    @Override
                    public void process(ProcessContext ctx) throws GrindException {
                        final Input2 input2 = ctx.resolveOutcome(Input2.class);
                        ctx.pushOutcome(Input1.class, new Input1(input2.input2));
                    }})
                .addPhase(new PhaseHandler() {
                    @Override
                    public void register(PhaseRegistration registration) throws GrindException {
                        registration.providesOutcome(Input2.class);
                    }

                    @Override
                    public void process(ProcessContext ctx) throws GrindException {
                        ctx.pushOutcome(Input2.class, new Input2("input2"));
                    }})
                .build();

        assertEquals(new TestResult("input2"), grind.resolve(TestResult.class));
    }
}
