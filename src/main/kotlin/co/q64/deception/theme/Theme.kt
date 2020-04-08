package co.q64.deception.theme

import co.q64.deception.Game
import discord4j.core.`object`.entity.Member
import discord4j.core.spec.EmbedCreateSpec
import reactor.core.publisher.Mono

interface Theme {
    val channelPrefix: String get() = "game-"
    val minPlayers: Int
    val maxPlayers: Int

    val player: Team
    val traitor: Team
    fun traitorCount(players: Int): Int
    fun roleCount(players: Int): Int
    fun generateRoles(): List<Role>
    fun generateOperations(game: Game): List<Operation>

    fun intro(): Mono<(EmbedCreateSpec) -> Unit>
    fun roleAssignmentStart(member: Member): Mono<(EmbedCreateSpec) -> Unit>
    fun roleAssignmentWait(member: Member): Mono<(EmbedCreateSpec) -> Unit>
    fun roleAssignmentDiscuss(member: Member): Mono<(EmbedCreateSpec) -> Unit>
    fun operationIntro(): Mono<(EmbedCreateSpec) -> Unit>
    fun operationActionWait(member: Member): Mono<(EmbedCreateSpec) -> Unit>
    fun operationDiscuss(member: Member): Mono<(EmbedCreateSpec) -> Unit>
    fun accusationIntro(game: Game): Mono<(EmbedCreateSpec) -> Unit>
    fun accusationVote(reactions: String): Mono<(EmbedCreateSpec) -> Unit>
    fun accusationComplete(member: Member): Mono<(EmbedCreateSpec) -> Unit>
}