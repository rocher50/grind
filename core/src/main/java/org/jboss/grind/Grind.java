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

        private final Map<Class<?>, Object> provided;

        private Context(Map<Class<?>, Object> provided) {
            this.provided = new HashMap<>(provided);
        }

        @Override
        public <O> void provide(Class<O> type, O value) throws GrindException {
            if(provided.put(type, value) != null) {
                // let's for now be strict about it
                throw new GrindException("Outcome of type " + type.getName() + " has already been provided");
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public <O> O consume(Class<O> type) throws GrindException {
            final Object value = provided.get(type);
            if(value == null) {
                throw new GrindException("Failed to resolve outcome of type " + type);
            }
            return (O) value;
        }
    }

    private final Map<Class<?>, List<PhaseDescription>> providers;
    private Map<Class<?>, Object> provided = Collections.emptyMap();

    protected Grind(GrindFactory factory) {
        providers = Collections.unmodifiableMap(factory.outcomeProviders);
    }

    @SuppressWarnings("unchecked")
    public <T> Grind provide(T value) throws GrindException {
        return provide((Class<T>)value.getClass(), value);
    }

    public <T> Grind provide(Class<T> type, T value) throws GrindException {
        if(provided.isEmpty()) {
            provided = new HashMap<>(provided);
        }
        if(provided.put(type, value) != null) {
            // let's for now be strict about it
            throw new GrindException("Outcome of type " + type.getName() + " has already been provided");
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> type) throws GrindException {
        final Object value = this.provided.get(type);
        if(value != null) {
            return (T) value;
        }
        final List<PhaseDescription> phaseChain = resolvePhaseChain(type);
        final ProcessContext ctx = new Context(provided);
        for(PhaseDescription phaseDescr : phaseChain) {
            phaseDescr.handler.process(ctx);
        }
        return ctx.consume(type);
    }

    private <T> List<PhaseDescription> resolvePhaseChain(Class<T> type) throws GrindException {
        final List<PhaseDescription> phases = providers.get(type);
        if(phases == null) {
            throw new GrindException("No providers found for outcome type " + type.getName());
        }
        List<PhaseDescription> chain = new ArrayList<>();
        for(PhaseDescription phaseDescr : phases) {
            resolvePhaseChain(chain, phaseDescr);
        }
        if(chain.isEmpty()) {
            throw new GrindException("Failed to resolve phase flow for the outcome of type " + type.getName());
        }
        return chain;
    }

    private boolean resolvePhaseChain(List<PhaseDescription> chain, PhaseDescription phaseDescr) throws GrindException {
        if(!phaseDescr.setFlag(PhaseDescription.VISITED)) {
            return false;
        }
        try {
            if (!phaseDescr.consumedTypes.isEmpty()) {
                for (Class<?> consumedType : phaseDescr.consumedTypes) {
                    if (provided.containsKey(consumedType)) {
                        continue;
                    }
                    final List<PhaseDescription> phases = providers.get(consumedType);
                    if (phases == null) {
                        return false;
                    }
                    boolean provided = false;
                    for (PhaseDescription provider : phases) {
                        if (provided = provider.isFlagOn(PhaseDescription.IN_CHAIN)) {
                            break;
                        }
                    }
                    if (provided) {
                        continue;
                    }
                    final int originalChainLength = chain.size();
                    for (PhaseDescription provider : phases) {
                        if (provided = resolvePhaseChain(chain, provider)) {
                            break;
                        }
                        if (chain.size() > originalChainLength) {
                            for (int i = chain.size() - 1; i >= originalChainLength; --i) {
                                chain.remove(i).clearFlag(PhaseDescription.IN_CHAIN);
                            }
                        }
                    }
                    if (provided) {
                        continue;
                    }
                    return false;
                }
            }
        } finally {
            phaseDescr.clearFlag(PhaseDescription.VISITED);
        }
        chain.add(phaseDescr);
        phaseDescr.setFlag(PhaseDescription.IN_CHAIN);
        return true;
    }
}
