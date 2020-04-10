package co.q64.deception

import reactor.core.publisher.Hooks
import reactor.kotlin.core.publisher.toMono
import java.io.File
import java.nio.file.Files

fun main() {
    Hooks.onOperatorDebug()
    File("token").run {
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
            ?.toMono() // TODO ?
            .orEmpty()
            .flatMap { Bot(it).start() }
            .block()
}