/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.ivyservice.moduleconverter;

import org.gradle.internal.component.local.model.LocalComponentGraphResolveState;

/**
 * Provides component instances intended to sit at the root of a dependency graph.
 */
public interface RootComponentProvider {

    /**
     * Create the root component that is intended to hold the root variant of a dependency graph.
     *
     * @param detached if the root variant is detached from the owner, and therefore should live
     *      within an adhoc root component.
     */
    LocalComponentGraphResolveState getRootComponent(boolean detached);

}
