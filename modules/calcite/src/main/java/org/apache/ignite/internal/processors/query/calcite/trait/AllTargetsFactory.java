/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.query.calcite.trait;

import java.io.ObjectStreamException;
import java.util.List;
import java.util.UUID;
import org.apache.calcite.util.ImmutableIntList;
import org.apache.ignite.internal.processors.query.calcite.metadata.NodesMapping;
import org.apache.ignite.internal.processors.query.calcite.prepare.PlannerContext;

/**
 *
 */
public final class AllTargetsFactory extends AbstractDestinationFunctionFactory {
    public static final DestinationFunctionFactory INSTANCE = new AllTargetsFactory();

    @Override public DestinationFunction create(PlannerContext ctx, NodesMapping m, ImmutableIntList k) {
        List<UUID> nodes = m.nodes();

        return r -> nodes;
    }

    @Override public Object key() {
        return "AllTargetsFactory";
    }

    private Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }
}