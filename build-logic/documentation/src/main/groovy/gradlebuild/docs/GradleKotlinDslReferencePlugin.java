/*
 * Copyright 2023 the original author or authors.
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

package gradlebuild.docs;

import gradlebuild.basics.BuildEnvironmentKt;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.dokka.gradle.DokkaExtension;
import org.jetbrains.dokka.gradle.DokkaPlugin;
import org.jetbrains.dokka.gradle.engine.parameters.DokkaSourceLinkSpec;
import org.jetbrains.dokka.gradle.engine.parameters.DokkaSourceSetSpec;
import org.jetbrains.dokka.gradle.engine.plugins.DokkaHtmlPluginParameters;
import org.jetbrains.dokka.gradle.tasks.DokkaGeneratePublicationTask;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public class GradleKotlinDslReferencePlugin implements Plugin<Project> {

    private static final String TASK_NAME = "dokkaGeneratePublicationHtml";

    @Override
    public void apply(Project project) {
        GradleDocumentationExtension documentationExtension = project.getExtensions().getByType(GradleDocumentationExtension.class);
        applyPlugin(project);
        updateDocumentationExtension(project, documentationExtension);
        configurePlugin(project, documentationExtension);
    }

    private static void applyPlugin(Project project) {
        project.getPlugins().apply(DokkaPlugin.class);
    }

    private static void updateDocumentationExtension(Project project, GradleDocumentationExtension extension) {
        TaskProvider<Task> generateTask = project.getTasks().named(TASK_NAME);
        Provider<? extends Directory> outputDirectory = generateTask.flatMap(t -> ((DokkaGeneratePublicationTask) t).getOutputDirectory());
        extension.getKotlinDslReference().getRenderedDocumentation().set(outputDirectory);
    }

    private static void configurePlugin(Project project, GradleDocumentationExtension extension) {
        renameModule(project);
        wireInArtificialSourceSet(project, extension);
        setStyling(project, extension);
        overrideDokkaVersion(project, extension);
    }

    private static void setStyling(Project project, GradleDocumentationExtension extension) {
        getDokkaExtension(project).getPluginsConfiguration().named("html", DokkaHtmlPluginParameters.class, config -> {
            config.getCustomStyleSheets().from(extension.getSourceRoot().file("kotlin/styles/gradle.css"));
            config.getCustomAssets().from(extension.getSourceRoot().file("kotlin/images/logo-icon.svg"));
            config.getFooterMessage().set("Gradle Kotlin DSL Reference");
        });
    }

    private static void overrideDokkaVersion(Project project, GradleDocumentationExtension extension) {
        Property<String> dokkaVersionOverride = extension.getKotlinDslReference().getDokkaVersionOverride();
        Property<String> defaultDokkaVersion = getDokkaExtension(project).getDokkaEngineVersion();
        defaultDokkaVersion.set(dokkaVersionOverride.convention(defaultDokkaVersion.get()));
    }

    /**
     * The name of the module is part of the URI for deep links, changing it will break existing links.
     * The name of the module must match the first header of {@code kotlin/Module.md} file.
     */
    private static void renameModule(Project project) {
        getDokkaExtension(project).getModuleName().set("gradle");
    }

    private static void wireInArtificialSourceSet(Project project, GradleDocumentationExtension extension) {
        TaskProvider<GradleKotlinDslRuntimeGeneratedSources> runtimeExtensions = project.getTasks()
            .register("gradleKotlinDslRuntimeGeneratedSources", GradleKotlinDslRuntimeGeneratedSources.class, task -> {
                task.getGeneratedSources().set(project.getLayout().getBuildDirectory().dir("gradle-kotlin-dsl-extensions/sources"));
                task.getGeneratedClasses().set(project.getLayout().getBuildDirectory().dir("gradle-kotlin-dsl-extensions/classes"));
            });

        NamedDomainObjectContainer<DokkaSourceSetSpec> kotlinSourceSet = getDokkaExtension(project).getDokkaSourceSets();
        kotlinSourceSet.register("kotlin_dsl", spec -> {
            spec.getDisplayName().set("DSL");
            spec.getSourceRoots().from(extension.getKotlinDslSource());
            spec.getSourceRoots().from(runtimeExtensions.flatMap(GradleKotlinDslRuntimeGeneratedSources::getGeneratedSources));
            spec.getClasspath().from(extension.getClasspath());
            spec.getClasspath().from(runtimeExtensions.flatMap(GradleKotlinDslRuntimeGeneratedSources::getGeneratedClasses));
            spec.getIncludes().from(extension.getSourceRoot().file("kotlin/Module.md"));
            configureSourceLinks(project, extension, spec);
        });

        NamedDomainObjectContainer<DokkaSourceSetSpec> javaSourceSet = getDokkaExtension(project).getDokkaSourceSets();
        javaSourceSet.register("java_api", spec -> {
            spec.getDisplayName().set("API");
            spec.getSourceRoots().from(extension.getDocumentedSource());
            spec.getClasspath().from(extension.getClasspath());
            spec.getIncludes().from(extension.getSourceRoot().file("kotlin/Module.md"));
            configureSourceLinks(project, extension, spec);
        });
    }

    private static void configureSourceLinks(Project project, GradleDocumentationExtension extension, DokkaSourceSetSpec spec) {
        String commitId = BuildEnvironmentKt.getBuildEnvironmentExtension(project).getGitCommitId().get();
        if (commitId.isBlank() || commitId.toLowerCase(Locale.ROOT).contains("unknown")) {
            // we can't figure out the commit ID (probably this is a source distribution build), let's skip adding source links
            return;
        }

        extension.getSourceRoots().getFiles()
            .forEach(
                file -> {
                    DokkaSourceLinkSpec sourceLinkSpec = project.getObjects().newInstance(DokkaSourceLinkSpec.class);
                    sourceLinkSpec.getLocalDirectory().set(file);
                    URI uri = toUri(project.getRootDir(), file, commitId);
                    sourceLinkSpec.getRemoteUrl().set(uri);
                    sourceLinkSpec.getRemoteLineSuffix().set("#L");
                    spec.getSourceLinks().add(sourceLinkSpec);
                }
            );
    }

    private static URI toUri(File projectRootDir, File file, String commitId) {
        try {
            URI relativeLocation = projectRootDir.toURI().relativize(file.toURI());
            return new URI("https://github.com/gradle/gradle/blob/" + commitId + "/" + relativeLocation);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static DokkaExtension getDokkaExtension(Project project) {
        return project.getExtensions().getByType(DokkaExtension.class);
    }

}
