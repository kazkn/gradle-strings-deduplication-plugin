package com.github.kazkn.gradle.deduplication


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.LineSeparator
import org.jdom2.output.XMLOutputter
import java.io.File
import java.io.FileWriter


class StringsDeduplicationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.task("deduplicationStrings").doLast {
            // find res Dirs
            val resDirs = project.rootProject.subprojects
                    .map { it.projectDir.path + "/src/main/res" }
                    .filter { File(it).exists() }

            val rootElement = Element("checkstyle")
            rootElement.setAttribute("version", "8.0")

            resDirs.flatMap { findStringsXmlPath(it) }
                    .forEach {
                        println("--- ${it.absolutePath.removePrefix(project.rootProject.projectDir.absolutePath)} ---")
                        val fileElement = Element("file").setAttribute("name", it.absolutePath)
                        rootElement.children.add(fileElement)

                        checkStringsXml(it, fileElement)
                    }

            writeReport(project.rootProject, Document(rootElement))
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
    private fun checkStringsXml(targetFile: File, reports: Element) {
        val doc = SAXBuilder().build(targetFile)

        val map = HashMap<String, MutableList<String>>()

        doc.rootElement.children.forEach { element ->
            val value = element.value

            if (map.containsKey(value)) {
                map[value]?.add(element.getAttributeValue("name"))
            } else {
                map[value] = mutableListOf(element.getAttributeValue("name"))
            }
        }

        val lineReports = HashMap<Int, Element>()

        map.entries
                .filter { it.value.size >= 2 }
                .forEach {
                    val message = "\"${it.key}\" is defined ${it.value.size} times (${it.value.joinToString(",")})"

                    println(message)

                    it.value.forEach { stringId ->
                        val line = getLineNumberForId(targetFile, stringId)

                        if (line != null) {
                            val element = Element("error")
                                    .setAttribute("line", line.toString())
                                    .setAttribute("severity", "error")
                                    .setAttribute("message", message)

                            lineReports[line] = element
                        }
                    }
                }

        reports.children.addAll(lineReports.keys.sorted().map { lineReports[it] })
    }

    private fun getLineNumberForId(targetFile: File, id: String): Int? {
        val lines = targetFile.readLines()
        val regex = Regex("^.*<string name=\"$id\">.*$")

        for (i in 0 until lines.size) {
            if (lines[i].matches(regex)) {
                return i + 1
            }
        }

        return null
    }

    // write report xml
    private fun writeReport(project: Project, xmlDocument: Document) {
        val file = File("${project.projectDir.absolutePath}/build/reports/strings-deduplication.xml")
        file.parentFile.mkdirs()

        XMLOutputter().apply {
            format = Format.getRawFormat()
            format.setLineSeparator(LineSeparator.SYSTEM)
            format.textMode = Format.TextMode.NORMALIZE
            format.indent = "\t"
            format.encoding = "utf-8"

            output(xmlDocument, FileWriter(file))
        }
    }
}
