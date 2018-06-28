package com.github.kazkn.gradle.deduplication


import org.gradle.api.Plugin
import org.gradle.api.Project

class StringsDeduplicationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        println("Hello" + project.name + "!")
    }
}
