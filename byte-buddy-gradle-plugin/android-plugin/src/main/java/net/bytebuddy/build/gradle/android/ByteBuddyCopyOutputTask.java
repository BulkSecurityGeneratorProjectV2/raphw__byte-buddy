/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.build.gradle.android;

import com.android.build.api.artifact.MultipleArtifact;
import com.android.build.api.variant.Variant;
import net.bytebuddy.utility.FileSystem;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileType;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.FileChange;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;

import java.io.File;
import java.io.IOException;

/**
 * Collects locally compiled classes (from both the Java and Kotlin compilers) and places them in a
 * single output folder that will be later used as input for Byte Buddy.
 */
public abstract class ByteBuddyCopyOutputTask extends DefaultTask {

    /**
     * Returns the local class path.
     *
     * @return The local class path.
     */
    @Incremental
    @Classpath
    public abstract ConfigurableFileCollection getLocalClasspath();

    /**
     * Returns the output directory.
     *
     * @return The output directory.
     */
    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    /**
     * The action to execute.
     *
     * @param inputChanges The incremental changes of the task.
     */
    @TaskAction
    public void execute(InputChanges inputChanges) {
        for (FileChange fileChange : inputChanges.getFileChanges(getLocalClasspath())) {
            if (fileChange.getFileType() == FileType.DIRECTORY) {
                return;
            }
            File target = getOutputDir().file(fileChange.getNormalizedPath()).get().getAsFile();
            switch (fileChange.getChangeType()) {
                case REMOVED:
                    if (target.delete()) {
                        getLogger().debug("Deleted file {}", target);
                    }
                    break;
                case ADDED:
                case MODIFIED:
                    try {
                        if (!target.getParentFile().isDirectory() && !target.getParentFile().mkdirs()) {
                            throw new IllegalStateException("Failed to create target directory: " + target.getParentFile());
                        }
                        FileSystem.getInstance().copy(fileChange.getFile(), target);
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to copy " + fileChange.getFile() + " to " + target, e);
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    /**
     * A configuration action for a {@link ByteBuddyCopyOutputTask}.
     */
    protected static class ConfigurationAction implements Action<ByteBuddyCopyOutputTask> {

        /**
         * The current Gradle project.
         */
        private final Project project;

        /**
         * The current variant.
         */
        private final Variant variant;

        /**
         * Creates a new configuration action for a {@link ByteBuddyCopyOutputTask}.
         *
         * @param project The current Gradle project.
         * @param variant The current variant.
         */
        protected ConfigurationAction(Project project, Variant variant) {
            this.project = project;
            this.variant = variant;
        }

        /**
         * {@inheritDoc}
         */
        public void execute(ByteBuddyCopyOutputTask task) {
            task.getLocalClasspath().from(variant.getArtifacts().getAll(MultipleArtifact.ALL_CLASSES_DIRS.INSTANCE));
            task.getOutputDir().set(project.getLayout().getBuildDirectory().dir("intermediates/incremental/" + task.getName()));
        }
    }
}
