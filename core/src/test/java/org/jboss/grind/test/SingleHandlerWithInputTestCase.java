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

import org.jboss.grind.PhaseRouter;
import org.jboss.grind.PhaseRouterException;
import org.jboss.grind.PhaseRouterFactory;
import org.jboss.grind.PhaseHandler;
import org.jboss.grind.PhaseRegistration;
import org.jboss.grind.PhaseProcessingContext;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class SingleHandlerWithInputTestCase {

    public static class Input1 {
        final String text;

        Input1(String text) {
            this.text = text;
        }
    }

    @Test
    public void mainTest() throws Exception {

        final PhaseRouter router = PhaseRouterFactory.getInstance()
                .addPhase(new PhaseHandler() {
                    @Override
                    public void register(PhaseRegistration registration) throws PhaseRouterException {
                        registration.consumes(Input1.class);
                        registration.provides(TestResult.class);
                    }
                    @Override
                    public void process(PhaseProcessingContext ctx) throws PhaseRouterException {
                        final Input1 input1 = ctx.consume(Input1.class);
                        ctx.provide(TestResult.class, new TestResult(input1.text));
                    }
                    })
                .build();

        router.provide(new Input1("input1"));
        assertEquals(new TestResult("input1"), router.consume(TestResult.class));
    }
}
