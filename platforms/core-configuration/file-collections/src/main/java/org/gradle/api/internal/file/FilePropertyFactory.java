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

package org.gradle.api.internal.file;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.internal.file.PathToFileResolver;
import org.gradle.internal.service.scopes.Scope;
import org.gradle.internal.service.scopes.ServiceScope;

@ServiceScope({Scope.Global.class, Scope.Project.class})
public interface FilePropertyFactory {
    DirectoryProperty newDirectoryProperty();

    RegularFileProperty newFileProperty();

    /**
     * Returns a new FilePropertyFactory configured with the given file resolver and file collection resolver.
     */
    FilePropertyFactory withResolvers(FileResolver fileResolver, PathToFileResolver fileCollectionResolver);

    /**
     * Returns a new FilePropertyFactory configured with the given file resolver.
     */
    FilePropertyFactory withResolver(FileResolver fileResolver);
}
