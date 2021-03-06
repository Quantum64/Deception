package co.q64.deception

import discord4j.core.DiscordClient
import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.ReactionAddEvent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.logging.Level
import java.util.logging.Logger

private val commands = mapOf<String, (Game, Message) -> Mono<Void>>(
        "help" to { _, m -> m.reply(help).then() },
        "join" to { g, m -> g.join(m) },
        "leave" to { g, m -> g.leave(m) },
        "start" to { g, m -> g.start(m) },
        "end" to { g, m -> g.end(m) },
        "roles" to { g, m -> g.roles(m) },
        "operations" to { g, m -> g.operations(m) }
)

private val help = """
    These are the commands that I know
    **+join**: Join a waiting game
    **+leave**: Leave a waiting game
    **+start**: Start the game
    **+end**: End the game
    **+roles**: List the currently enabled roles
    **+operations**: List the currently enabled operations
""".trimIndent()

class Bot(token: String) {
    private val logger = Logger.getLogger("game")
    private val games: MutableMap<Guild, Game> = mutableMapOf()
    private val client: DiscordClient = DiscordClientBuilder.create(token).build()

    fun start(): Mono<Void> = Mono.`when`(
            Flux.interval(Duration.ofSeconds(1), Schedulers.newParallel("timers"))
                    .flatMapIterable { games.values }
                    .flatMap {
                        it.tick().onErrorResume { error ->
                            Mono.just(error)
                                    .doOnNext { exception -> logger.log(Level.SEVERE, "Exception in timer tick", exception) }
                                    .then()
                        }
                    },
            client.withGateway { gateway ->
                Mono.`when`(
                        gateway.eventDispatcher.on(MessageCreateEvent::class.java)
                                .filterWhen { it.guild.hasElement() }
                                .filter { it.message.content.startsWith(commandPrefix) }
                                .flatMap { event ->
                                    event.guild.flatMap { guild ->
                                        commands[event.message.content.removePrefix(commandPrefix).toLowerCase()]?.let { action ->
                                            action(game(guild), event.message)
                                        }.orEmpty()
                                    }.onErrorResume { error ->
                                        Mono.just(error)
                                                .doOnNext { exception -> logger.log(Level.SEVERE, "Exception in command processor", exception) }
                                                .then()
                                    }
                                }
                                .then(),
                        gateway.eventDispatcher.on(ReactionAddEvent::class.java)
                                .filterWhen { it.guild.hasElement() }
                                .filter { it.member.isPresent }
                                .flatMap { event ->
                                    event.guild.flatMap { guild ->
                                        event.message.flatMap { message ->
                                            game(guild).handleReaction(event.member.get(), message, event.emoji)
                                        }
                                    }.onErrorResume { error ->
                                        Mono.just(error)
                                                .doOnNext { exception -> logger.log(Level.SEVERE, "Exception in reaction processor", exception) }
                                                .then()
                                    }
                                }
                                .then(),
                        gateway.onDisconnect()
                )
            })

    private fun game(guild: Guild): Game =
            games.getOrPut(guild, { Game(guild) })
}