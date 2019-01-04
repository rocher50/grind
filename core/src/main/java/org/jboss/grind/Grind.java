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
public class Grind {

    private class Context implements ProcessContext {

        private final Map<Class<?>, Object> pushedOutcomes;


        private Context(Map<Class<?>, Object> pushedOutcomes) {
            this.pushedOutcomes = new HashMap<>(pushedOutcomes);
        }

        @Override
        public <O> void pushOutcome(Class<O> outcomeType, O outcome) throws GrindException {
            if(pushedOutcomes.put(outcomeType, outcome) != null) {
                // let's for now be strict about it
                throw new GrindException("Outcome of type " + outcomeType.getName() + " has already been provided");
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public <O> O resolveOutcome(Class<O> outcomeType) throws GrindException {
            final Object outcome = pushedOutcomes.get(outcomeType);
            if(outcome == null) {
                throw new GrindException("Failed to resolve outcome of type " + outcomeType);
            }
            return (O) outcome;
        }
    }

    private final Map<Class<?>, List<PhaseDescription>> inputProviders;
    private final Map<Class<?>, List<PhaseDescription>> outcomeProviders;
    private Map<Class<?>, Object> pushedOutcomes = Collections.emptyMap();

    protected Grind(GrindFactory factory) {
        inputProviders = Collections.unmodifiableMap(factory.inputProviders);
        outcomeProviders = Collections.unmodifiableMap(factory.outcomeProviders);
    }

    public <O> Grind pushOutcome(Class<O> type, O outcome) throws GrindException {
        if(pushedOutcomes.isEmpty()) {
            pushedOutcomes = new HashMap<>(pushedOutcomes);
        }
        if(pushedOutcomes.put(type, outcome) != null) {
            // let's for now be strict about it
            throw new GrindException("Outcome of type " + type.getName() + " has already been provided");
        }
        return this;
    }

    public <T> T resolve(Class<T> outcomeType) throws GrindException {
        final List<PhaseDescription> phaseChain = resolvePhaseChain(outcomeType);
        final ProcessContext ctx = new Context(pushedOutcomes);
        for(PhaseDescription phaseDescr : phaseChain) {
            phaseDescr.handler.process(ctx);
        }
        return ctx.resolveOutcome(outcomeType);
    }

    private <T> List<PhaseDescription> resolvePhaseChain(Class<T> outcomeType) throws GrindException {
        final List<PhaseDescription> providers = outcomeProviders.get(outcomeType);
        if(providers == null) {
            throw new GrindException("No providers found for outcome type " + outcomeType.getName());
        }
        List<PhaseDescription> chain = new ArrayList<>();
        for(PhaseDescription phaseDescr : providers) {
            resolvePhaseChain(chain, phaseDescr);
        }
        if(chain.isEmpty()) {
            throw new GrindException("Failed to resolve phase chain for the outcome type " + outcomeType.getName());
        }
        return chain;
    }

    private boolean resolvePhaseChain(List<PhaseDescription> chain, PhaseDescription phaseDescr) throws GrindException {
        if(!phaseDescr.setFlag(PhaseDescription.VISITED)) {
            return false;
        }
        if(phaseDescr.inputTypes.isEmpty()) {
            chain.add(phaseDescr);
            return true;
        }
        for(Class<?> inputType : phaseDescr.inputTypes) {
            if(pushedOutcomes.containsKey(inputType)) {
                continue;
            }
            final List<PhaseDescription> providers = outcomeProviders.get(inputType);
            if(providers == null) {
                throw new GrindException("No provider found for input type " + inputType.getName());
            }
            boolean provided = false;
            for(PhaseDescription provider : providers) {
                if(provided = provider.isFlagOn(PhaseDescription.IN_CHAIN)) {
                    break;
                }
            }
            if(provided) {
                continue;
            }
            final int originalChainLength = chain.size();
            for(PhaseDescription provider : providers) {
                if(provided = resolvePhaseChain(chain, provider)) {
                    break;
                }
                if(chain.size() > originalChainLength) {
                    for(int i = chain.size() - 1; i >= originalChainLength; --i) {
                        chain.remove(i).clearFlag(PhaseDescription.IN_CHAIN);
                    }
                }
            }
            if(provided) {
                continue;
            }
            return false;
        }
        chain.add(phaseDescr);
        phaseDescr.clearFlag(PhaseDescription.VISITED);
        phaseDescr.setFlag(PhaseDescription.IN_CHAIN);
        return true;
    }
}
