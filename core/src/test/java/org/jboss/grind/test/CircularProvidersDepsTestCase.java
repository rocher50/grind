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
import static org.junit.Assert.fail;

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
public class CircularProvidersDepsTestCase {

    public static class Type1 {
        final String text;

        Type1(String text) {
            this.text = text;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((text == null) ? 0 : text.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Type1 other = (Type1) obj;
            if (text == null) {
                if (other.text != null)
                    return false;
            } else if (!text.equals(other.text))
                return false;
            return true;
        }
    }

    public static class Type2 {
        final String text;

        Type2(String text) {
            this.text = text;
        }
    }

    public static class Type3 {
        final String text;

        Type3(String text) {
            this.text = text;
        }
    }

    @Test
    public void mainTest() throws Exception {

        final Grind grind = GrindFactory.getInstance()
                .addPhase(new PhaseHandler() {
                    @Override
                    public void register(PhaseRegistration registration) throws GrindException {
                        registration.provides(Type1.class);
                        registration.consumes(Type2.class);
                    }
                    @Override
                    public void process(ProcessContext ctx) throws GrindException {
                        ctx.provide(Type1.class, new Type1(ctx.consume(Type2.class).text));
                    }})
                .addPhase(new PhaseHandler() {
                    @Override
                    public void register(PhaseRegistration registration) throws GrindException {
                        registration.provides(Type2.class);
                        registration.consumes(Type3.class);
                    }
                    @Override
                    public void process(ProcessContext ctx) throws GrindException {
                        ctx.provide(Type2.class, new Type2(ctx.consume(Type3.class).text));
                    }})
                .addPhase(new PhaseHandler() {
                    @Override
                    public void register(PhaseRegistration registration) throws GrindException {
                        registration.consumes(Type1.class);
                        registration.provides(Type3.class);
                    }
                    @Override
                    public void process(ProcessContext ctx) throws GrindException {
                        ctx.provide(Type3.class, new Type3(ctx.consume(Type1.class).text));
                    }})
                .build();

        try {
            grind.resolve(Type1.class);
            fail();
        } catch(GrindException e) {
            assertEquals("Failed to resolve phase flow for the outcome of type " + Type1.class.getName(), e.getMessage());
        }
    }
}