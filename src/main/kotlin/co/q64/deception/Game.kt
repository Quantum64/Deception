package co.q64.deception

import co.q64.deception.state.GameState
import co.q64.deception.state.State
import co.q64.deception.state.WaitingState
import co.q64.deception.theme.NoOperation
import co.q64.deception.theme.agent.AgentTheme
import discord4j.core.`object`.PermissionOverwrite
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.rest.util.Permission
import discord4j.rest.util.PermissionSet
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.util.logging.Logger


class Game(private val guild: Guild) {
    private val logger = Logger.getLogger("game")
    val players: MutableList<Player> = mutableListOf()
    val members: List<Member> get() = players.map { it.member }
    var state: State = WaitingState
    var theme = AgentTheme
    var selected: Player? = null

    fun start(message: Message): Mono<Void> =
            state.toMono().filterWhen { checkPermissions(message) }.flatMap { state ->
                state.toMono()
                        .filter { it is WaitingState }
                        .switchIfEmpty(message.reply("Cannot start game. Game is already running!").thenEmpty())
                        .flatMap {
                            players.size.toMono()
                                    .filter { it >= theme.minPlayers }
                                    .switchIfEmpty(message.reply("Cannot start game. The minimum number of players is " +
                                            "${theme.minPlayers}, but you only have ${players.size} so far.").thenEmpty())
                                    .flatMap {
                                        players.toFlux().flatMap { player ->
                                            player.member.voiceState
                                                    .flatMap {
                                                        when {
                                                            //it.isSelfMuted -> "${player.member.mention} is self muted in voice chat".toMono()
                                                            it.isSelfDeaf -> "${player.member.mention} is self deafened in void chat".toMono()
                                                            !it.channelId.isPresent -> "${player.member.mention} is not connected to voice chat".toMono()
                                                            else -> "".toMono() // TODO this is hacky
                                                        }
                                                    }
                                                    .switchIfEmpty("${player.member.mention} is not connected to voice chat".toMono())
                                        }
                                                .filter { !it.isBlank() }
                                                .reduce { a, b -> "$a\n$b" }
                                                .switchIfEmpty(initializeGame().thenEmpty())
                                                .flatMap { problems ->
                                                    message.channel.flatMap { channel ->
                                                        channel.createMessage(("Voice chat is critical for gameplay, " +
                                                                "and I've noticed issues that need to be resolved before starting\n" +
                                                                problems))
                                                    }
                                                }
                                    }
                        }
            }.then()

    private fun initializeGame(): Mono<Void> = deleteChannels().then(players.toFlux().flatMap { player ->
        guild.everyoneRole.flatMap { everyone ->
            guild.members.filterWhen { member -> guild.client.selfId.map { it == member.id } }.toMono().flatMap { bot ->
                guild.createTextChannel {
                    it
                            .setName(theme.channelPrefix + player.member.id.asLong())
                            .setPermissionOverwrites(setOf<PermissionOverwrite>(
                                    PermissionOverwrite.forRole(everyone.guildId,
                                            PermissionSet.none(),
                                            PermissionSet.of(
                                                    Permission.VIEW_CHANNEL,
                                                    Permission.READ_MESSAGE_HISTORY,
                                                    Permission.ADD_REACTIONS
                                            )),
                                    PermissionOverwrite.forMember(player.member.id,
                                            PermissionSet.of(
                                                    Permission.READ_MESSAGE_HISTORY,
                                                    Permission.VIEW_CHANNEL),
                                            PermissionSet.none()),
                                    PermissionOverwrite.forMember(bot.id,
                                            PermissionSet.all(),
                                            PermissionSet.none())
                            ))
                }.map { player.channel = it }
            }
        }
    }.doOnComplete {
        for (count in 1..theme.traitorCount(players.size)) {
            players.shuffled().firstOrNull { it.team == theme.player }?.let {
                it.team = theme.traitor
                it.startingTeam = theme.traitor
            }
        }
        val roles = theme.generateRoles()
        for (count in 1..theme.roleCount(players.size)) {
            players.shuffle()
            roles.shuffled().firstOrNull { role -> role !in players.map { it.role } }?.assign(this)
        }
        val operations = theme.generateOperations(this)
        for (player in players) {
            player.operation = operations.shuffled()
                    .firstOrNull { operation -> operation !in players.map { it.operation } && operation.canAssign(player) }
                    ?: NoOperation
        }
        players.shuffle()
    }.then(enter(GameState.STARTING))).then()

    fun end(message: Message): Mono<Void> = Mono.just(state)
            .filter { it !is WaitingState }
            .switchIfEmpty(message.reply("You can not end the game because the game has not started!").thenEmpty())
            .flatMap {
                players.clear()
                state = WaitingState
                undeafen().and(message.reply("Game ended!")).then(deleteChannels())
            }
            .then()


    fun join(message: Message): Mono<Void> = Mono.just(state)
            .filter { it is WaitingState }
            .switchIfEmpty(message.reply("You can not join the game since it has already started!").thenEmpty())
            .flatMap {
                message.authorAsMember
                        .flatMap { inGame(it) }
                        .filter { it == false }
                        .switchIfEmpty(message.reply("You can not join a game that you have already joined!").thenEmpty())
                        .flatMap {
                            players.size.toMono()
                                    .filter { it < theme.maxPlayers }
                                    .switchIfEmpty(message.reply("This game is already full! (${players.size}/${theme.maxPlayers})").thenEmpty())
                                    .flatMap {
                                        message.authorAsMember.map { member -> players.add(Player(this, member)) }.flatMap {
                                            message.reply("You have joined the game (${players.size}/${theme.maxPlayers})")
                                        }
                                    }
                        }
            }
            .then()

    fun leave(message: Message): Mono<Void> = Mono.just(state)
            .filter { it is WaitingState }
            .flatMap {
                message.authorAsMember.flatMap { member ->
                    inGame(member)
                            .filter { it }
                            .flatMap {
                                players.removeIf { it.member == member }
                                message.reply("Left game! (${players.size}/${theme.maxPlayers} players)")
                            }
                            .switchIfEmpty(message.reply("You cannot leave a game that you have not joined!"))
                }
            }
            .switchIfEmpty(message.reply("You can not leave a game that has already started!"))
            .then()

    fun roles(message: Message): Mono<Void> = theme
            .generateRoles()
            .toFlux()
            .map { "__${it.name}__: ${it.description}" }
            .collectList()
            .flatMap { message.reply("These are the currently enabled roles:\n${it.joinToString("\n")}") }
            .then()

    fun operations(message: Message): Mono<Void> = theme
            .generateOperations(this)
            .toFlux()
            .flatMap { operations ->
                message.authorAsMember.map { sender ->
                    "**${operations.helpTitle}**: ${operations.helpDescription(SyntheticPlayer(this, sender))}"
                }
            }
            .collectList()
            .flatMap { message.reply("These are the currently enabled operations:\n${it.joinToString("\n")}") }
            .then()

    /*
    fun kick(message: Message): Mono<Void> = Mono.just(state)
            .filter { it is WaitingState }
            .flatMap {
                message.authorAsMember.flatMap { member ->
                    inGame(member)
                            .filter { it }
                            .flatMap {
                                players.removeIf { it.member == member }
                                message.reply("Left game! (${players.size}/${theme.maxPlayers} players)")
                            }
                            .switchIfEmpty(message.reply("You cannot leave a game that you have not joined!"))
                }
            }
            .switchIfEmpty(message.reply("You can not kick a player from a have that has already started!"))
            .then()
     */

    fun tick() = state.tick()
    fun handleReaction(member: Member, message: Message, reaction: ReactionEmoji): Mono<Void> =
            state.handleReaction(member, message, reaction)

    fun enter(new: GameState): Mono<Void> = state
            .toMono()
            .flatMap { it.exit() }
            .then(new.generator(this@Game).toMono()
                    .doOnNext { logger.info("Entering state ${new.name}") }
                    .doOnNext { state = it }
                    .flatMap { it.enter() }
            )


    fun deafen(): Mono<Void> = Flux.fromIterable(players.map { it.member })
            .flatMap { member ->
                member.voiceState.filter { it.channelId.isPresent }.flatMap { _ ->
                    member.edit {
                        it.setDeafen(true)
                    }
                }
            }
            .then()

    fun undeafen(): Mono<Void> = Flux.fromIterable(players.map { it.member })
            .flatMap { member ->
                member.voiceState.filter { it.channelId.isPresent }.flatMap { _ ->
                    member.edit {
                        it.setDeafen(false)
                    }
                }
            }
            .then()

    fun deleteChannels(): Mono<Void> = guild.channels
            .filter { it.name.startsWith(theme.channelPrefix) }
            .flatMap { it.delete() }
            .then()

    private fun inGame(member: Member?): Mono<Boolean> =
            players.map { it.member }.any { it == member }.toMono()

    private fun checkPermissions(message: Message): Mono<Boolean> =
            guild.members.filterWhen { member -> guild.client.selfId.map { it == member.id } }.toMono()
                    .switchIfEmpty(message.reply("There was a problem checking the permissions of the bot. Please try again later.").thenEmpty())
                    .flatMap { member ->
                        member.basePermissions.flatMap { permissions ->
                            if (permissions.containsAll(required.keys)) true.toMono()
                            else message.reply("I am missing the following permissions that I need to make the game work correctly\n" + required
                                    .filterKeys { it !in permissions }
                                    .toList()
                                    .joinToString("\n") {
                                        "**${it.first.name.replace("_", " ").toLowerCase().titleCase()}**: ${it.second}"
                                    }).then(false.toMono())
                        }
                    }
}

private val required: Map<Permission, String> = mapOf(
        Permission.READ_MESSAGE_HISTORY to "I need this to see what's going on in the game channels",
        Permission.ADD_REACTIONS to "I need this to add interactive reactions to messages I create",
        Permission.VIEW_CHANNEL to "I need this so I can find channels on the server",
        Permission.MANAGE_CHANNELS to "I need this to create and delete game channels and to set up " +
                "permissions correctly, and I will never touch any channel that I did not create",
        Permission.MANAGE_MESSAGES to "I need full control over messages in game channels, and I " +
                "will never touch a message in a channel I did not create",
        Permission.DEAFEN_MEMBERS to "I need this to temporarily deafen members in voice channels to enforce " +
                "game rules, and I will always undeafen anyone that I deafen",
        Permission.SEND_MESSAGES to "I need this to send messages..."
)
