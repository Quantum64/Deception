package co.q64.tripleagent.theme

import co.q64.tripleagent.Player
import co.q64.tripleagent.state.BasicState
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageReaction

interface Operation {
    val title: String
    val automatic: Boolean get() = true
    fun description(player: Player): String
    fun message(player: Player, message: EmbedBuilder)
    fun canAssign(player: Player): Boolean = true
    fun handleMessage(player: Player, message: Message) = Unit
    fun handleReaction(player: Player, state: BasicState, reaction: MessageReaction) = Unit
}

object NoOperation : Operation {
    override val title get() = "Unknown Operation"
    override fun description(player: Player) = "If you are seeing this, there is a bug in the game."
    override fun message(player: Player, message: EmbedBuilder) = Unit
    override fun canAssign(player: Player) = true
}