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
 * Includes a set of phase handlers, allows to provide additional input
 * and consume outcomes of specific types by resolving the phases and the
 * order in which they have to be processed to produce the requested outcome.
 *
 * @author Alexey Loubyansky
 */
public class PhaseRouter {

    private class Context implements PhaseProcessingContext {

        private final Map<Class<?>, Object> provided;

        private Context(Map<Class<?>, Object> provided) {
            this.provided = new HashMap<>(provided);
        }

        @Override
        public <O> void provide(Class<O> type, O value) throws PhaseRouterException {
            if(provided.put(type, value) != null) {
                // let's for now be strict about it
                throw new PhaseRouterException("Outcome of type " + type.getName() + " has already been provided");
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public <O> O consume(Class<O> type) throws PhaseRouterException {
            final Object value = provided.get(type);
            if(value == null) {
                throw new PhaseRouterException("Failed to resolve outcome of type " + type);
            }
            return (O) value;
        }

        @Override
        public boolean isAvailable(Class<?> type) {
            return provided.containsKey(type);
        }
    }

    private final Map<Class<?>, List<PhaseDescription>> providers;
    private Map<Class<?>, Object> provided = Collections.emptyMap();
    private boolean checkHandlerOutcome;

    protected PhaseRouter(PhaseRouterFactory factory) {
        providers = Collections.unmodifiableMap(factory.providers);
        checkHandlerOutcome = factory.checkHandlerOutcome;
    }

    /**
     * Whether to check that handlers actually provide the outcomes
     * they declared during registration
     *
     * @param checkHandlerOutcome  whether to check handler outcome
     */
    public void setCheckHandlerOutcome(boolean checkHandlerOutcome) {
        this.checkHandlerOutcome = checkHandlerOutcome;
    }

    /**
     * Provides an outcome for consumption by other handlers
     *
     * @param value  outcome
     * @throws PhaseRouterException  in case of a failure
     */
    @SuppressWarnings("unchecked")
    public void provide(Object value) throws PhaseRouterException {
        provide((Class<Object>)value.getClass(), value);
    }

    /**
     * Provides a value that can be consumed by phase handlers.
     */
    public <T> void provide(Class<T> type, T value) throws PhaseRouterException {
        if(provided.isEmpty()) {
            provided = new HashMap<>(provided);
        }
        if(provided.put(type, value) != null) {
            // let's for now be strict about it
            throw new PhaseRouterException("Outcome of type " + type.getName() + " has already been provided");
        }
    }

    /**
     * Consumes a value of the specified type by processing the necessary phases
     */
    @SuppressWarnings("unchecked")
    public <T> T consume(Class<T> type) throws PhaseRouterException {
        final Object value = this.provided.get(type);
        if(value != null) {
            return (T) value;
        }
        final List<PhaseDescription> phaseChain = resolvePhaseChain(type);
        final Context ctx = new Context(provided);
        for(PhaseDescription phaseDescr : phaseChain) {
            phaseDescr.handler.process(ctx);
            if(checkHandlerOutcome && !phaseDescr.providedTypes.isEmpty()) {
                List<Class<?>> missingTypes = null;
                for(Class<?> providedType : phaseDescr.providedTypes) {
                    if(!ctx.isAvailable(providedType)) {
                        if(missingTypes == null) {
                            missingTypes = new ArrayList<>(1);
                        }
                        missingTypes.add(providedType);
                    }
                }
                if(missingTypes != null) {
                    throw new PhaseRouterException(Errors.handlerNotProvidedOutcomes(phaseDescr.handler, missingTypes));
                }
            }
        }
        return ctx.consume(type);
    }

    /**
     * Provides certain values and consumes a value of the specified type
     * by processing the necessary phases
     *
     * @param type  type of the consumed outcome
     * @param provided  provided values
     * @return  outcome
     * @throws PhaseRouterException  in case of a failure
     */
    public <T> T consume(Class<T> type, Object... provided) throws PhaseRouterException {
        if(provided.length > 0) {
            for(Object o : provided) {
                provide(o);
            }
        }
        return consume(type);
    }

    public boolean isAvailable(Class<?> type) {
        return provided.containsKey(type);
    }

    private <T> List<PhaseDescription> resolvePhaseChain(Class<T> type) throws PhaseRouterException {
        final List<PhaseDescription> phases = providers.get(type);
        if(phases == null) {
            throw new PhaseRouterException("No providers found for outcome type " + type.getName());
        }
        List<PhaseDescription> chain = new ArrayList<>();
        for(PhaseDescription phaseDescr : phases) {
            resolvePhaseChain(chain, phaseDescr);
        }
        if(chain.isEmpty()) {
            throw new PhaseRouterException("Failed to resolve phase flow for the outcome of type " + type.getName());
        }
        return chain;
    }

    private boolean resolvePhaseChain(List<PhaseDescription> chain, PhaseDescription phaseDescr) throws PhaseRouterException {
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
                        if (provided = provider.isFlagOn(PhaseDescription.IN_LINE)) {
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
                                chain.remove(i).clearFlag(PhaseDescription.IN_LINE);
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
        phaseDescr.setFlag(PhaseDescription.IN_LINE);
        return true;
    }
}
