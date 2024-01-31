/*
 * Copyright 2023 Google LLC
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

package com.google.gradle.plugins

import com.google.gradle.tasks.CopyFileTask
import com.google.gradle.util.android
import com.google.gradle.util.outputFile
import com.google.gradle.util.release
import com.google.gradle.util.tempFile
import java.io.File
import kotlinx.validation.KotlinApiBuildTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

typealias BuildApiTask = KotlinApiBuildTask

/**
 * A Gradle plugin for creating `.api` files; representing the public API of the project.
 *
 * Registers two tasks:
 * - `buildApi` -> creates a `.api` file containing the *current* public API of the project.
 * - `exportApi` -> exports the file generated by `buildApi` to a `public.api` file at the project
 *   directory
 *
 * @see ApiPluginExtension
 * @see ChangelogPluginExtension
 */
abstract class ApiPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    with(project) {
      extensions.create<ApiPluginExtension>("api").apply { commonConfiguration() }

      val buildApi = registerBuildApiTask()

      tasks.register<CopyFileTask>("exportApi") {
        source.set(buildApi.outputFile)
        dest.set(project.file("public.api"))
      }
    }
  }

  private fun Project.registerBuildApiTask() =
    tasks.register<BuildApiTask>("buildApi") {
      val classes = provider { android.release.output.classesDirs }

      inputClassesDirs = files(classes)
      inputDependencies = files(classes)
      outputApiDir = tempFile("api").get()
    }

  context(Project)
  private fun ApiPluginExtension.commonConfiguration() {
    val latestApiFile = rootProject.file("api/${project.version}.api")

    apiFile.convention(latestApiFile)
  }
}

/**
 * Extension properties for the [ApiPlugin].
 *
 * @property apiFile The file to reference to for the publicly released api.
 */
abstract class ApiPluginExtension {
  @get:Optional abstract val apiFile: Property<File>
}
