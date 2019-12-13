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

package org.apache.ignite.internal.processors.query.calcite.rel;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelDistribution;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Exchange;

/**
 *
 */
public class IgniteExchange extends Exchange implements IgniteRel {
    public IgniteExchange(RelOptCluster cluster, RelTraitSet traitSet, RelNode input, RelDistribution distribution) {
        super(cluster, traitSet, input, distribution);
    }

    @Override public Exchange copy(RelTraitSet traitSet, RelNode newInput, RelDistribution newDistribution) {
        return new IgniteExchange(getCluster(), traitSet, newInput, newDistribution);
    }

    @Override public <T> T accept(IgniteRelVisitor<T> visitor) {
        return visitor.visit(this);
    }
}