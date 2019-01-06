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
public class HandlerNotProvidingPromisedOutcomeCheckOutcomeFalseTestCase {

    public static class OtherOutcome extends TestResult {

    }

    @Test
    public void mainTest() throws Exception {

        final PhaseRouter router = PhaseRouterFactory.getInstance()
                .addPhase(new PhaseHandler() {
                    @Override
                    public void register(PhaseRegistration registration) throws PhaseRouterException {
                        registration.provides(TestResult.class);
                        registration.provides(OtherOutcome.class);
                    }
                    @Override
                    public void process(PhaseProcessingContext ctx) throws PhaseRouterException {
                        ctx.provide(TestResult.class, new TestResult("success"));
                    }
                    })
                .build();

        assertEquals(new TestResult("success"), router.consume(TestResult.class));
    }
}
