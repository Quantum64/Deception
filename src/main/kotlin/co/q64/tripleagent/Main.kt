package co.q64.tripleagent

import discord4j.core.DiscordClientBuilder
import discord4j.core.event.domain.message.MessageCreateEvent
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
    val builder = DiscordClientBuilder(token)
    val client = builder.build()
    GameHandler(client)
    client.login().block()
}