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

package org.jboss.grind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Alexey Loubyansky
 */
public class GrindFactory {

    public static GrindFactory getInstance() {
        return new GrindFactory();
    }

    private class Registration implements PhaseRegistration {

        private int phasesTotal;
        PhaseDescription phaseDescr;

        void register(PhaseHandler handler) throws GrindException {
            this.phaseDescr = new PhaseDescription(++phasesTotal, handler);
            handler.register(this);
        }

        @Override
        public void requiresInput(Class<?> inputType) throws GrindException {
            phaseDescr.addInputType(inputType);
            List<PhaseDescription> providers = inputProviders.get(inputType);
            if(providers == null) {
                inputProviders.put(inputType, Collections.singletonList(phaseDescr));
                return;
            }
            if(providers.size() == 1) {
                final List<PhaseDescription> tmp = new ArrayList<>(2);
                tmp.add(providers.get(0));
                inputProviders.put(inputType, tmp);
                providers = tmp;
            }
            providers.add(phaseDescr);
        }

        @Override
        public void providesOutcome(Class<?> outcomeType) throws GrindException {
            phaseDescr.addOutcomeType(outcomeType);
            List<PhaseDescription> providers = outcomeProviders.get(outcomeType);
            if(providers == null) {
                outcomeProviders.put(outcomeType, Collections.singletonList(phaseDescr));
                return;
            }
            if(providers.size() == 1) {
                final List<PhaseDescription> tmp = new ArrayList<>(2);
                tmp.add(providers.get(0));
                outcomeProviders.put(outcomeType, tmp);
                providers = tmp;
            }
            providers.add(phaseDescr);
        }
    }

    private Registration registration = new Registration();
    Map<Class<?>, List<PhaseDescription>> inputProviders = new HashMap<>();
    Map<Class<?>, List<PhaseDescription>> outcomeProviders = new HashMap<>();

    private GrindFactory() {
    }

    public GrindFactory addPhase(PhaseHandler handler) throws GrindException {
        registration.register(handler);
        return this;
    }

    public Grind build() throws GrindException {
        return new Grind(this);
    }
}
