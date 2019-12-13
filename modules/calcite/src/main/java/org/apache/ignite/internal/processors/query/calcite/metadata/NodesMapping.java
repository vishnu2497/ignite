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

package org.apache.ignite.internal.processors.query.calcite.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.ignite.internal.processors.query.calcite.util.Commons;
import org.apache.ignite.internal.util.GridIntList;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.U;

/**
 *
 */
public class NodesMapping implements Serializable {
    public static final byte HAS_MOVING_PARTITIONS = 1;
    public static final byte HAS_REPLICATED_CACHES = 1 << 1;
    public static final byte HAS_PARTITIONED_CACHES = 1 << 2;
    public static final byte PARTIALLY_REPLICATED = 1 << 3;
    public static final byte DEDUPLICATED = 1 << 4;

    private final List<UUID> nodes;
    private final List<List<UUID>> assignments;
    private final byte flags;

    public NodesMapping(List<UUID> nodes, List<List<UUID>> assignments, byte flags) {
        this.nodes = nodes;
        this.assignments = assignments;
        this.flags = flags;
    }

    public List<UUID> nodes() {
        return nodes;
    }

    public List<List<UUID>> assignments() {
        return assignments;
    }

    public NodesMapping mergeWith(NodesMapping other) throws LocationMappingException {
        byte flags = (byte) (this.flags | other.flags);

        if ((flags & PARTIALLY_REPLICATED) == 0)
            return new NodesMapping(U.firstNotNull(nodes, other.nodes), mergeAssignments(other, null), flags);

        List<UUID> nodes;

        if (this.nodes == null)
            nodes = other.nodes;
        else if (other.nodes == null)
            nodes = this.nodes;
        else
            nodes = Commons.intersect(this.nodes, other.nodes);

        if (nodes != null && nodes.isEmpty())
            throw new LocationMappingException("Failed to map fragment to location.");

        return new NodesMapping(nodes, mergeAssignments(other, nodes), flags);
    }

    public NodesMapping deduplicate() {
        if (!excessive())
            return this;

        if (assignments == null) {
            UUID node = nodes.get(ThreadLocalRandom.current().nextInt(nodes.size()));

            return new NodesMapping(Collections.singletonList(node), null, (byte)(flags | DEDUPLICATED));
        }

        HashSet<UUID> nodes0 = new HashSet<>();
        List<List<UUID>> assignments0 = new ArrayList<>(assignments.size());

        for (List<UUID> partNodes : assignments) {
            UUID node = F.first(partNodes);

            if (node == null)
                assignments0.add(Collections.emptyList());
            else {
                assignments0.add(Collections.singletonList(node));

                nodes0.add(node);
            }
        }

        return new NodesMapping(new ArrayList<>(nodes0), assignments0, (byte)(flags | DEDUPLICATED));
    }

    public int[] partitions(UUID node) {
        if (assignments == null)
            return null;

        GridIntList parts = new GridIntList(assignments.size());

        for (int i = 0; i < assignments.size(); i++) {
            List<UUID> assignment = assignments.get(i);
            if (Objects.equals(node, F.first(assignment)))
                parts.add(i);
        }

        return parts.array();
    }

    public boolean excessive() {
        return (flags & DEDUPLICATED) == 0;
    }

    public boolean hasMovingPartitions() {
        return (flags & HAS_MOVING_PARTITIONS) == HAS_MOVING_PARTITIONS;
    }

    public boolean hasReplicatedCaches() {
        return (flags & HAS_REPLICATED_CACHES) == HAS_REPLICATED_CACHES;
    }

    public boolean hasPartitionedCaches() {
        return (flags & HAS_PARTITIONED_CACHES) == HAS_PARTITIONED_CACHES;
    }

    public boolean partiallyReplicated() {
        return (flags & PARTIALLY_REPLICATED) == PARTIALLY_REPLICATED;
    }

    private List<List<UUID>> mergeAssignments(NodesMapping other, List<UUID> nodes) throws LocationMappingException {
        byte flags = (byte) (this.flags | other.flags); List<List<UUID>> left = assignments, right = other.assignments;

        if (left == null && right == null)
            return null; // nothing to intersect;

        if (left == null || right == null || (flags & HAS_MOVING_PARTITIONS) == 0) {
            List<List<UUID>> assignments = U.firstNotNull(left, right);

            if (nodes == null || (flags & PARTIALLY_REPLICATED) == 0)
                return assignments;

            List<List<UUID>> assignments0 = new ArrayList<>(assignments.size());
            HashSet<UUID> nodesSet = new HashSet<>(nodes);

            for (List<UUID> partNodes : assignments) {
                List<UUID> partNodes0 = new ArrayList<>(partNodes.size());

                for (UUID partNode : partNodes) {
                    if (nodesSet.contains(partNode))
                        partNodes0.add(partNode);
                }

                if (partNodes0.isEmpty())
                    throw new LocationMappingException("Failed to map fragment to location.");

                assignments0.add(partNodes0);
            }

            return assignments0;
        }

        List<List<UUID>> assignments = new ArrayList<>(left.size());
        HashSet<UUID> nodesSet = nodes != null ? new HashSet<>(nodes) : null;

        for (int i = 0; i < left.size(); i++) {
            List<UUID> leftNodes = left.get(i), partNodes = new ArrayList<>(leftNodes.size());
            HashSet<UUID> rightNodesSet = new HashSet<>(right.get(i));

            for (UUID partNode : leftNodes) {
                if (rightNodesSet.contains(partNode) && (nodesSet == null || nodesSet.contains(partNode)))
                    partNodes.add(partNode);
            }

            if (partNodes.isEmpty())
                throw new LocationMappingException("Failed to map fragment to location.");

            assignments.add(partNodes);
        }

        return assignments;
    }
}