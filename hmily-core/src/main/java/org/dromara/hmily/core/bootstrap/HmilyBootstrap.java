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

package org.dromara.hmily.core.bootstrap;

import org.dromara.hmily.common.utils.LogUtil;
import org.dromara.hmily.config.HmilyConfig;
import org.dromara.hmily.core.disruptor.publisher.HmilyRepositoryEventPublisher;
import org.dromara.hmily.core.hook.HmilyShutdownHook;
import org.dromara.hmily.core.repository.HmilyRepositoryFacade;
import org.dromara.hmily.core.holder.SingletonHolder;
import org.dromara.hmily.core.logo.HmilyLogo;
import org.dromara.hmily.core.schedule.HmilyTransactionSelfRecoveryScheduled;
import org.dromara.hmily.repository.spi.HmilyRepository;
import org.dromara.hmily.serializer.spi.HmilySerializer;
import org.dromara.hmily.spi.ExtensionLoaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xiaoyu
 */
public final class HmilyBootstrap {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HmilyBootstrap.class);
    
    private static final HmilyBootstrap INSTANCE = new HmilyBootstrap();
    
    private HmilyBootstrap() {
    }
    
    public static HmilyBootstrap getInstance() {
        return INSTANCE;
    }
    
    /**
     * hmily initialization.
     *
     * @param hmilyConfig {@linkplain HmilyConfig}
     */
    public void start(final HmilyConfig hmilyConfig) {
        try {
            SingletonHolder.INST.register(HmilyConfig.class, hmilyConfig);
            loadHmilyRepository(hmilyConfig);
            HmilyShutdownHook.getInstance().registerAutoCloseable(new HmilyTransactionSelfRecoveryScheduled());
            HmilyShutdownHook.getInstance().registerAutoCloseable(HmilyRepositoryEventPublisher.getInstance());
        } catch (Exception ex) {
            LogUtil.error(LOGGER, " hmily init exception:{}", ex::getMessage);
            System.exit(1);
        }
        new HmilyLogo().logo();
    }
    
   
    private void loadHmilyRepository(final HmilyConfig hmilyConfig) {
        HmilySerializer hmilySerializer = ExtensionLoaderFactory.load(HmilySerializer.class, hmilyConfig.getSerializer());
        HmilyRepository hmilyRepository = ExtensionLoaderFactory.load(HmilyRepository.class, hmilyConfig.getRepository());
        hmilyRepository.setSerializer(hmilySerializer);
        hmilyRepository.init(hmilyConfig);
        HmilyRepositoryFacade.getInstance().setHmilyRepository(hmilyRepository);
    }
}
