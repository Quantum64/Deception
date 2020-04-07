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
import discord4j.core.spec.GuildMemberEditSpec
import discord4j.core.spec.TextChannelCreateSpec
import discord4j.rest.util.Permission
import discord4j.rest.util.PermissionSet
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


class Game(private val guild: Guild) {
    val players: MutableList<Player> = mutableListOf()
    val members: Flux<Member> get() = Flux.fromIterable(players.map { it.member })
    var state: State = WaitingState
    var theme = AgentTheme
    var selected: Player? = null

    fun start(message: Message): Mono<Void> = Mono.just(state)
            .filter { it is WaitingState }
            .flatMap {
                deleteChannels().then(Flux.fromIterable(players).flatMap { player ->
                    guild.everyoneRole.flatMap { everyone ->
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
                                                    PermissionSet.none())
                                    ))
                        }.map { player.channel = it }
                    }
                }.then(Mono.just(0).doOnEach {
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
                }.then(enter(GameState.STARTING))))
            }
            .switchIfEmpty(message.reply("Cannot start game. Game is already running!").then(Mono.empty()))
            .then()
    // TODO Check voice chat

    fun end(message: Message): Mono<Void> = Mono.just(state)
            .filter { it !is WaitingState }
            .flatMap {
                players.clear()
                state = WaitingState
                deleteChannels().then(unmute()).then(message.reply("Game ended!"))
            }
            .then()
            .switchIfEmpty(message.reply("You can not end the game because the game has not started!").then())

    fun join(message: Message): Mono<Void> = Mono.just(state)
            .filter { it is WaitingState }
            .flatMap {
                message.authorAsMember
                        .flatMap { inGame(it) }
                        .filter { it == false }
                        .flatMap {
                            message.authorAsMember.map { member -> players.add(Player(this, member)) }
                                    .then(message.reply("joined"))
                        }
                        .switchIfEmpty(message.reply("You can not join a game that you have already joined!"))
            }
            .switchIfEmpty(message.reply("You can not join the game since it has already started!"))
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

    fun tick() = state.tick()
    fun handleReaction(member: Member, message: Message, reaction: ReactionEmoji): Mono<Void> =
            state.handleReaction(member, message, reaction)

    fun enter(new: GameState): Mono<Void> {
        val enter = new.generator(this)
        val action = state.exit().then(enter.enter())
        state = enter
        return action
    }

    fun mute(): Mono<Void> = Flux.fromIterable(players.map { it.member })
            .flatMap { member -> member.edit { it.setMute(true) } }
            .then()

    fun unmute(): Mono<Void> = Flux.fromIterable(players.map { it.member })
            .flatMap { member -> member.edit { it.setMute(false) } }
            .then()

    private fun inGame(member: Member?): Mono<Boolean> =
            Mono.just(players.map { it.member }.any { it == member })

    private fun deleteChannels(): Mono<Void> = guild.channels
            .filter { it.name.startsWith(theme.channelPrefix) }
            .flatMap { it.delete() }
            .then()
}
