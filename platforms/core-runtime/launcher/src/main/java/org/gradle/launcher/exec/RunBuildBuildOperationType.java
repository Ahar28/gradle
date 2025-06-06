/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.launcher.exec;

import org.gradle.internal.operations.BuildOperationType;
import org.gradle.internal.problems.failure.Failure;
import org.jspecify.annotations.Nullable;

public final class RunBuildBuildOperationType implements BuildOperationType<RunBuildBuildOperationType.Details, RunBuildBuildOperationType.Result> {
    public interface Details {
    }

    public interface Result {
        /**
         * The build failure.
         * <p>
         * Used by the Tooling Api.
         */
        @Nullable
        Failure getFailure();
    }
}

