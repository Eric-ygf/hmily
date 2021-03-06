/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dromara.hmily.core.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.Weigher;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.dromara.hmily.common.utils.CollectionUtils;
import org.dromara.hmily.common.utils.StringUtils;
import org.dromara.hmily.core.repository.HmilyRepositoryFacade;
import org.dromara.hmily.repository.spi.entity.HmilyParticipant;
import org.dromara.hmily.repository.spi.entity.HmilyTransaction;

/**
 * use google guava cache.
 *
 * @author xiaoyu
 */
public final class HmilyParticipantCacheManager {
    
    private static final int MAX_COUNT = 1000000;
    
    private final LoadingCache<String, List<HmilyParticipant>> LOADING_CACHE =
            CacheBuilder.newBuilder().maximumWeight(MAX_COUNT)
                    .weigher((Weigher<String, List<HmilyParticipant>>) (string, hmilyParticipantList) -> getSize())
                    .build(new CacheLoader<String, List<HmilyParticipant>>() {
                        @Override
                        public List<HmilyParticipant> load(final String key) {
                            return cacheHmilyParticipant(key);
                        }
                    });
    
    private static final HmilyParticipantCacheManager INSTANCE = new HmilyParticipantCacheManager();
    
    private HmilyParticipantCacheManager() {
    }
    
    /**
     * HmilyTransactionCacheManager.
     *
     * @return HmilyTransactionCacheManager
     */
    public static HmilyParticipantCacheManager getInstance() {
        return INSTANCE;
    }
    
    public void cacheHmilyParticipant(final HmilyParticipant hmilyParticipant) {
        String participantId = hmilyParticipant.getParticipantId();
        cacheHmilyParticipant(participantId, hmilyParticipant);
    }
    
    public void cacheHmilyParticipant(final String participantId, final HmilyParticipant hmilyParticipant) {
        List<HmilyParticipant> existHmilyParticipantList = get(participantId);
        if (CollectionUtils.isEmpty(existHmilyParticipantList)) {
            LOADING_CACHE.put(participantId, Lists.newArrayList(hmilyParticipant));
        } else {
            existHmilyParticipantList.add(hmilyParticipant);
            LOADING_CACHE.put(participantId, existHmilyParticipantList);
        }
    }
    
    /**
     * acquire hmilyTransaction.
     *
     * @param participantId this guava key.
     * @return {@linkplain HmilyTransaction}
     */
    public List<HmilyParticipant> get(final String participantId) {
        try {
            return LOADING_CACHE.get(participantId);
        } catch (ExecutionException e) {
            return Collections.emptyList();
        }
    }
    
    /**
     * remove guava cache by key.
     *
     * @param participantId guava cache key.
     */
    public void removeByKey(final String participantId) {
        if (StringUtils.isNoneBlank(participantId)) {
            LOADING_CACHE.invalidate(participantId);
        }
    }
    
    private int getSize() {
        return (int) LOADING_CACHE.size();
    }
    
    private List<HmilyParticipant> cacheHmilyParticipant(final String key) {
        return Optional.ofNullable(HmilyRepositoryFacade.getInstance().findHmilyParticipant(key)).orElse(Collections.emptyList());
    }
}
