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
public class PhaseRouterFactory {

    public static PhaseRouterFactory getInstance() {
        return new PhaseRouterFactory();
    }

    private class Registration implements PhaseRegistration {

        private int phasesTotal;
        PhaseDescription phaseDescr;

        void register(PhaseHandler handler) throws GrindException {
            this.phaseDescr = new PhaseDescription(++phasesTotal, handler);
            handler.register(this);
        }

        @Override
        public void consumes(Class<?> type) throws GrindException {
            phaseDescr.addConsumedType(type);
        }

        @Override
        public void provides(Class<?> type) throws GrindException {
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

    private PhaseRouterFactory() {
    }

    public PhaseRouterFactory addPhase(PhaseHandler handler) throws GrindException {
        registration.register(handler);
        return this;
    }

    public PhaseRouter build() throws GrindException {
        return new PhaseRouter(this);
    }
}
