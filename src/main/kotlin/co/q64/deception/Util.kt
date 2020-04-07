package co.q64.deception

import discord4j.core.`object`.entity.Message
import reactor.core.publisher.Mono

const val commandPrefix = "!"

fun Message.reply(text: String): Mono<Message> =
        channel.flatMap { channel ->
            authorAsMember.flatMap { author -> channel.createMessage("${author.mention} $text") }
        }