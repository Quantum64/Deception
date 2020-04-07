package co.q64.deception

import discord4j.core.DiscordClient
import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.ReactionAddEvent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

private val commands = mapOf<String, (Game, Message) -> Mono<Void>>(
        "join" to { g, m -> g.join(m) },
        "leave" to { g, m -> g.leave(m) },
        "start" to { g, m -> g.start(m) },
        "end" to { g, m -> g.end(m) }
)

class Bot(private val token: String) {
    private val games: MutableMap<Guild, Game> = mutableMapOf()
    private val client: DiscordClient = DiscordClientBuilder.create(token).build()


    init {
        Flux.interval(Duration.ofSeconds(1))
                .flatMapIterable { games.values }
                .flatMap {
                    it.tick()
                }
                .subscribe()

        client.withGateway { gateway ->
            gateway.eventDispatcher.on(MessageCreateEvent::class.java)
                    .filterWhen { it.guild.hasElement() }
                    .filter { it.message.content.startsWith(commandPrefix) }
                    .flatMap { event ->
                        event.guild.flatMap { guild ->
                            commands[event.message.content.removePrefix(commandPrefix).toLowerCase()]?.let { action ->
                                action(game(guild), event.message)
                            }
                        }
                    }
                    .subscribe()
            gateway.eventDispatcher.on(ReactionAddEvent::class.java)
                    .filterWhen { it.guild.hasElement() }
                    .filter { it.member.isPresent }
                    .flatMap { event ->
                        event.guild.flatMap { guild ->
                            event.message.flatMap { message ->
                                game(guild).handleReaction(event.member.get(), message, event.emoji)
                            }
                        }
                    }
                    .subscribe()
            gateway.onDisconnect()
        }.block()


    }

    private fun game(guild: Guild): Game =
            games.getOrPut(guild, { Game(guild) })
}