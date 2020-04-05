package co.q64.tripleagent

import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.Role
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono

const val channelPrefix = "game-"

fun agentRole(guild: Guild): Mono<Role> =
        guild.roles.filter { it.name == "Secret Agent" }.toMono()

fun reply(message: Message, text: String): Flux<*> =
        message.authorAsMember.flatMap { member -> message.channel.flatMap { channel -> channel.createMessage("${member.mention} $text") } }.toFlux()