/*
 * Copyright 2015 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.netty.buffer;

import java.util.List;

/**
 * Expose metrics for an arena.公开竞技场的指标。
 */
public interface PoolArenaMetric {

    /**
     * Returns the number of thread caches backed by this arena.返回此竞技场支持的线程缓存的数量。
     */
    int numThreadCaches();

    /**
     * Returns the number of tiny sub-pages for the arena.返回竞技场的小子页面数。
     */
    int numTinySubpages();

    /**
     * Returns the number of small sub-pages for the arena.返回竞技场的小子页面数。
     */
    int numSmallSubpages();

    /**
     * Returns the number of chunk lists for the arena.返回竞技场的块列表数量。
     */
    int numChunkLists();

    /**
     * Returns an unmodifiable {@link List} which holds {@link PoolSubpageMetric}s for tiny sub-pages.返回一个不可修改的列表，其中包含用于小子页面的PoolSubpageMetrics。
     */
    List<PoolSubpageMetric> tinySubpages();

    /**
     * Returns an unmodifiable {@link List} which holds {@link PoolSubpageMetric}s for small sub-pages.返回一个不可修改的列表，其中包含用于小子页面的PoolSubpageMetrics。
     */
    List<PoolSubpageMetric> smallSubpages();

    /**
     * Returns an unmodifiable {@link List} which holds {@link PoolChunkListMetric}s.返回通过竞技场完成的少量分配。
     */
    List<PoolChunkListMetric> chunkLists();

    /**
     * Return the number of allocations done via the arena. This includes all sizes.返回通过竞技场完成的分配数量。这包括所有大小。
     */
    long numAllocations();

    /**
     * Return the number of tiny allocations done via the arena.返回通过竞技场完成的微小分配的数量。
     */
    long numTinyAllocations();

    /**
     * Return the number of small allocations done via the arena.返回通过竞技场完成的少量分配。
     */
    long numSmallAllocations();

    /**
     * Return the number of normal allocations done via the arena.返回通过竞技场完成的正常分配的数量。
     */
    long numNormalAllocations();

    /**
     * Return the number of huge allocations done via the arena.返回通过竞技场完成的巨额分配的数量。
     */
    long numHugeAllocations();

    /**
     * Return the number of deallocations done via the arena. This includes all sizes.返回通过竞技场完成的交易数量。这包括所有大小。
     */
    long numDeallocations();

    /**
     * Return the number of tiny deallocations done via the arena.返回通过竞技场完成的微小交易数量。
     */
    long numTinyDeallocations();

    /**
     * Return the number of small deallocations done via the arena.返回通过竞技场完成的小交易数量。
     */
    long numSmallDeallocations();

    /**
     * Return the number of normal deallocations done via the arena.返回通过竞技场完成的正常交易数量。
     */
    long numNormalDeallocations();

    /**
     * Return the number of huge deallocations done via the arena.返回通过竞技场完成的巨大交易数量。
     */
    long numHugeDeallocations();

    /**
     * Return the number of currently active allocations.返回当前活动分配的数量。
     */
    long numActiveAllocations();

    /**
     * Return the number of currently active tiny allocations.返回当前活动的微小分配的数量。
     */
    long numActiveTinyAllocations();

    /**
     * Return the number of currently active small allocations.返回当前活动的小规模分配的数量。
     */
    long numActiveSmallAllocations();

    /**
     * Return the number of currently active normal allocations.返回当前活动的正常分配的数量。
     */
    long numActiveNormalAllocations();

    /**
     * Return the number of currently active huge allocations.返回当前活跃的巨额分配的数量。
     */
    long numActiveHugeAllocations();

    /**
     * Return the number of active bytes that are currently allocated by the arena.返回竞技场当前分配的活动字节数。
     */
    long numActiveBytes();
}
