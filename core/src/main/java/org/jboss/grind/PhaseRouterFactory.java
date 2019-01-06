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
 * Phase router factory.
 *
 * @author Alexey Loubyansky
 */
public class PhaseRouterFactory {

    /**
     * Factory instance
     *
     * @return  factory instance
     */
    public static PhaseRouterFactory getInstance() {
        return new PhaseRouterFactory();
    }

    private class Registration implements PhaseRegistration {

        private int phasesTotal;
        PhaseDescription phaseDescr;

        void register(PhaseHandler handler) throws PhaseRouterException {
            this.phaseDescr = new PhaseDescription(++phasesTotal, handler);
            handler.register(this);
        }

        @Override
        public void consumes(Class<?> type) throws PhaseRouterException {
            phaseDescr.addConsumedType(type);
        }

        @Override
        public void provides(Class<?> type) throws PhaseRouterException {
            phaseDescr.addProvidedType(type);
            List<PhaseDescription> typeProviders = providers.get(type);
            if(typeProviders == null) {
                providers.put(type, Collections.singletonList(phaseDescr));
                return;
            }
            if(typeProviders.size() == 1) {
                final List<PhaseDescription> tmp = new ArrayList<>(2);
                tmp.add(typeProviders.get(0));
                providers.put(type, tmp);
                typeProviders = tmp;
            }
            typeProviders.add(phaseDescr);
        }
    }

    private Registration registration = new Registration();
    Map<Class<?>, List<PhaseDescription>> providers = new HashMap<>();
    boolean checkHandlerOutcome = false;

    private PhaseRouterFactory() {
    }

    /**
     * Whether to check that handlers actually provide the outcomes
     * they declared during registration
     *
     * @param checkHandlerOutcome whether to check handler outcomes
     * @return  this factory instance
     */
    public PhaseRouterFactory setCheckHandlerOutcome(boolean checkHandlerOutcome) {
        this.checkHandlerOutcome = checkHandlerOutcome;
        return this;
    }

    /**
     * Adds a phase handler
     *
     * @param handler  phase handler
     * @return  this factory instance
     * @throws PhaseRouterException  in case of a failure
     */
    public PhaseRouterFactory addPhase(PhaseHandler handler) throws PhaseRouterException {
        registration.register(handler);
        return this;
    }

    /**
     * Creates a new instance of PhaseRouter
     *
     * @return  PhaseRouter instance
     * @throws PhaseRouterException  in case of a failure
     */
    public PhaseRouter build() throws PhaseRouterException {
        return new PhaseRouter(this);
    }
}
