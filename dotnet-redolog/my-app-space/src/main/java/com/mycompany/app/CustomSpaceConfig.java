/*
 * Copyright (c) 2008-2016, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mycompany.app;

import java.util.Properties;


import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.core.cluster.ClusterInfoContext;
import org.openspaces.core.config.annotation.EmbeddedSpaceBeansConfig;
import org.openspaces.core.space.EmbeddedSpaceFactoryBean;

public class CustomSpaceConfig extends EmbeddedSpaceBeansConfig {

	@ClusterInfoContext
	private ClusterInfo clusterInfo;

    @Override
    protected void configure(EmbeddedSpaceFactoryBean factoryBean) {
		super.configure(factoryBean);
		factoryBean.setMirrored(true);
		Properties properties = new Properties();
        properties.setProperty("space-config.engine.cache_policy", "1"); // 1 == ALL IN CACHE
		properties.setProperty("cluster-config.groups.group.repl-policy.swap-redo-log.storage-type", "sqlite");
		properties.setProperty("cluster-config.groups.group.repl-policy.redo-log-memory-capacity", "0");
		properties.setProperty(	"cluster-config.groups.group.repl-policy.redo-log-capacity", "10000");
        factoryBean.setProperties(properties);
	}


	

}
