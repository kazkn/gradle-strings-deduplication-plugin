package com.github.kazkn.gradle.deduplication;


import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class StringsDeduplicationPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        System.out.print("Hello" + project.getName() + "!");
    }
}
