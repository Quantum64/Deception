package co.q64.deception.theme

import co.q64.deception.Player
import discord4j.core.spec.EmbedCreateSpec
import reactor.core.publisher.Mono

interface Team {
    val name: String
    val genericName: String
    val other: Team
    fun assignmentCard(player: Player): Mono<(EmbedCreateSpec) -> Unit>
}