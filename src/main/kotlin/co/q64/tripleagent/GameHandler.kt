package co.q64.tripleagent

import discord4j.core.DiscordClient
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.util.function.Tuples
import java.util.concurrent.ConcurrentHashMap

class GameHandler(private val client: DiscordClient) {
    private val games = mutableMapOf<Guild, Game>()

    init {
        with(client.eventDispatcher) {
            // Remove the agent role from all users
            on(GuildCreateEvent::class.java)
                    .map { it.guild }
                    .flatMap { agentRole(it) }
                    .subscribe { role ->
                        role.guild.toFlux()
                                .flatMap { it.members }
                                .filterWhen { member -> member.roles.any { it == role } }
                                .flatMap { it.removeRole(role.id) }
                                .subscribe()
                    }

            // Delete channels from previous games
            on(GuildCreateEvent::class.java)
                    .map { it.guild }
                    .flatMap { it.channels }
                    .filter { it.name.toLowerCase().startsWith(channelPrefix) }
                    .flatMap { it.delete() }
                    .subscribe()
        }

        gameCommand("!start") { game, message -> game.start(message) }
        gameCommand("!join") { game, message -> game.join(message) }
    }

    private fun command(name: String, action: (message: Flux<Message>) -> Flux<*>) {
        client.eventDispatcher.on(MessageCreateEvent::class.java)
                .map { it.message }
                .filter { it.content.orElse("").toLowerCase().startsWith(name.toLowerCase()) }
                .run { action(this) }
                .subscribe()
    }

    private fun game(guild: Guild): Game =
            games.getOrPut(guild) { Game(guild) }

    private fun gameCommand(name: String, action: (game: Game, message: Message) -> Flux<*>) {
        command(name) { message ->
            message.flatMap { it.guild.zipWith(Mono.just(it)) }.flatMap { action(game(it.t1), it.t2) }
        }
    }
}

