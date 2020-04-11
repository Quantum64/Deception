package co.q64.deception

import discord4j.core.`object`.entity.Message
import reactor.core.publisher.Mono

val numbers = listOf("1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣")
const val commandPrefix = "+"

fun Message.reply(text: String): Mono<Message> =
        channel.flatMap { channel ->
            authorAsMember.flatMap { author -> channel.createMessage("${author.mention} $text") }
        }

fun <T> Mono<T>?.orEmpty(): Mono<T> = this ?: Mono.empty()

fun <T> Mono<*>?.thenEmpty(): Mono<T> = this?.then(Mono.empty()) ?: Mono.empty()

fun String.titleCase(): String = split(" ").joinToString(" ") { it.capitalize() }

fun color(r: Int, b: Int, g: Int) = ((r and 0x0ff) shl 16) or ((g and 0x0ff) shl 8) or (b and 0x0ff);