package com.github.kazkn.gradle.deduplication


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jdom2.input.SAXBuilder
import java.io.File


class StringsDeduplicationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.task("deduplicationStrings").doLast {
            // find res Dirs
            val resDirs = project.rootProject.subprojects
                    .map { it.projectDir.path + "/src/main/res" }
                    .filter { File(it).exists() }

            resDirs.flatMap { findStringsXmlPath(it) }
                    .forEach {
                        println("--- $it ---")
                        checkStringsXml(it)
                    }
        }
    }

    // find values*/strings.xml
    private fun findStringsXmlPath(resDirPath: String) = File(resDirPath).listFiles().flatMap {
        if (it.isDirectory) {
            it.listFiles().asIterable()
        } else {
            listOf(it)
        }
    }.filter {
        it.path.matches(Regex("^.*values.*/strings.xml$"))
    }

    // find duplicate string
    private fun checkStringsXml(xmlPath: File) {
        val doc = SAXBuilder().build(xmlPath)

        val map = HashMap<String, MutableList<String>>()

        doc.rootElement.children.forEach { element ->
            val value = element.value

            if (map.containsKey(value)) {
                map[value]?.add(element.getAttributeValue("name"))
            } else {
                map[value] = mutableListOf(element.getAttributeValue("name"))
            }
        }

        map.entries
                .filter { it.value.size >= 2 }
                .forEach {
                    println("\"${it.key}\" is defined ${it.value.size} times (${it.value.joinToString(",")})")
                }
    }
}
