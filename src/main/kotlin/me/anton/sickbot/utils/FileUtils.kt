package me.anton.sickbot.utils

import org.bson.Document
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class FileUtils {
    companion object {
        fun getFileAsDocument(name: String): Document {
            val stream = Companion::class.java.classLoader.getResourceAsStream("$name.json")
            val reader = InputStreamReader(stream, StandardCharsets.UTF_8)
            val bufferedReader = BufferedReader(reader)
            val stringBuilder = StringBuilder()
            for (line in bufferedReader.lines()) {
                stringBuilder.append(line).append("\n")
            }
            return Document.parse(stringBuilder.toString())
        }

        fun getValueOfResource(docName: String, key: String): Any{
            val doc = getFileAsDocument(docName)
            return doc.getValue(key)
        }
    }
}