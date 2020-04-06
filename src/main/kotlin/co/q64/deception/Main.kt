package co.q64.deception

import java.io.File
import java.nio.file.Files

fun main() {
    val token = File("token").run {
        if (!exists()) {
            println("File 'token' not found!")
            return
        }
        Files.readAllLines(toPath()).firstOrNull().let {
            if (it == null) {
                println("Could not read token from file")
                return
            }
            it
        }
    }
    Bot(token!!)
}