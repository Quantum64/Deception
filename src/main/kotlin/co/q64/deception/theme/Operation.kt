package co.q64.deception.theme

import co.q64.deception.Player
import co.q64.deception.state.BasicState
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.spec.EmbedCreateSpec
import reactor.core.publisher.Mono

interface Operation {
    val title: String
    val automatic: Boolean get() = true
    fun description(player: Player): String
    fun message(player: Player): Mono<(EmbedCreateSpec) -> Unit>
    fun canAssign(player: Player): Boolean = true
    fun handleMessage(player: Player, message: Message): Mono<Void> = Mono.empty()
    fun handleReaction(player: Player, message: Message, state: BasicState, reaction: ReactionEmoji): Mono<Void> = Mono.empty()
}

object NoOperation : Operation {
    override val title get() = "Unknown Operation"
    override fun description(player: Player) = "If you are seeing this, there is a bug in the game."
    override fun message(player: Player): Mono<(EmbedCreateSpec) -> Unit> = Mono.empty()
    override fun canAssign(player: Player) = true
}