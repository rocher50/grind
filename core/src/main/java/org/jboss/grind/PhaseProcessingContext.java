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

/**
 * Phase processing context which allows a handle to consume
 * outcomes of previously processed phases and/or provide
 * new outcomes.
 *
 * @author Alexey Loubyansky
 */
public interface PhaseProcessingContext {

    /**
     * Provides an outcome for consumption by other handlers
     *
     * @param value  outcome
     * @throws PhaseRouterException  in case of a failure
     */
    @SuppressWarnings("unchecked")
    default void provide(Object value) throws PhaseRouterException {
        provide((Class<Object>)value.getClass(), value);
    }

    /**
     * Provides an outcome of the specified type for consumption
     * by other phase handlers.
     *
     * @param type  outcome type
     * @param value  outcome value
     * @throws PhaseRouterException  in case of a failure
     */
    <T> void provide(Class<T> type, T value) throws PhaseRouterException;

    /**
     * Consumes an outcome of a previously processed phase.
     *
     * @param type  outcome type
     * @return  outcome value
     * @throws PhaseRouterException  in case of a failure
     */
    <T> T consume(Class<T> type) throws PhaseRouterException;

    /**
     * Checks whether an outcome of specific type is available.
     *
     * @param type  outcome type
     * @return  true if the outcome is available, false if not
     */
    boolean isAvailable(Class<?> type);
}
