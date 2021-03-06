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
import java.util.List;

/**
 *
 * @author Alexey Loubyansky
 */
public class PhaseDescription {

    static final int IN_LINE = 0b000001;
    static final int VISITED = 0b000010;

    protected final int id;
    protected final PhaseHandler handler;
    protected List<Class<?>> consumedTypes = Collections.emptyList();
    protected List<Class<?>> providedTypes = Collections.emptyList();
    private int flags;

    protected PhaseDescription(int id, PhaseHandler handler) {
        this.id = id;
        this.handler = handler;
    }

    protected void addConsumedType(Class<?> consumedType) {
        if(consumedTypes.isEmpty()) {
            consumedTypes = new ArrayList<>(1);
        }
        consumedTypes.add(consumedType);
    }

    protected void addProvidedType(Class<?> providedType) {
        if(providedTypes.isEmpty()) {
            providedTypes = new ArrayList<>(1);
        }
        providedTypes.add(providedType);
    }

    boolean isFlagOn(int flag) {
        return (flags & flag) > 0;
    }

    boolean setFlag(int flag) {
        if((flags & flag) > 0) {
            return false;
        }
        flags ^= flag;
        return true;
    }

    void clearFlag(int flag) {
        if((flags & flag) > 0) {
            flags ^= flag;
        }
    }
}
