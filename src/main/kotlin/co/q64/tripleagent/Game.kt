package co.q64.tripleagent

import discord4j.core.`object`.entity.Channel
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import reactor.core.publisher.Flux
import reactor.core.publisher.toFlux
import java.time.Duration

class Game(private val guild: Guild) {
    var started = false;

    fun start(message: Message): Flux<*> {
        if (started) {
            return reply(message, "Can't start game. Game is already running!")
        }
        started = true
        var countdown: Flux<Message> = message.channel
                .flatMap { it.createMessage("The game is about to start!") }.toFlux()
        for (value in 5 downTo 1) {
            countdown = countdown
                    .delayElements(Duration.ofSeconds(1))
                    .flatMap { it.channel }.flatMap { it.createMessage("$value...") }
        }
        val result = countdown.flatMap { players() }.flatMap { member ->
            guild.createTextChannel { spec ->
                spec.run {
                    setName(channelPrefix + member.id)
                }
            }.flatMap { created -> created.createMessage(member.mention + " This is your channel. You should watch this channel for the entire game.") }
        }.delayElements(Duration.ofSeconds(5))
        return result
    }

    fun join(message: Message): Flux<*> {
        if (started) {
            return reply(message, "You can't join a game that has already started!")
        }
        return reply(message, "Joined game!").flatMap { agentRole(guild).flatMap { role -> message.authorAsMember.flatMap { author -> author.addRole(role.id) } } }
    }

    private fun players(): Flux<Member> =
            agentRole(guild).toFlux()
                    .flatMap { role -> guild.members.filterWhen { member -> member.roles.any { role == it } } }

    private fun channel(member: Member): Flux<Channel> =
            member.guild.toFlux().flatMap { guild -> guild.channels.filter { channel -> channel.name == "$channelPrefix${member.id}" } }
}