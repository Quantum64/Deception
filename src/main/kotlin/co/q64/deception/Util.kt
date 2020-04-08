package co.q64.deception

import discord4j.core.`object`.entity.Message
import reactor.core.publisher.Mono

val numbers = listOf("1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣")
const val commandPrefix = "!"

fun Message.reply(text: String): Mono<Message> =
        channel.flatMap { channel ->
            authorAsMember.flatMap { author -> channel.createMessage("${author.mention} $text") }
        }

fun <T> Mono<T>?.orEmpty(): Mono<T> = this ?: Mono.empty()