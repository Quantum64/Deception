package co.q64.deception.state

import co.q64.deception.Game
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageReaction
import org.atteo.evo.inflector.English

interface State {
    val state: GameState
    fun enter() = Unit
    fun exit() = Unit
    fun tick() = Unit
    fun handleReaction(member: Member, reaction: MessageReaction) = Unit
}

abstract class BasicState(val game: Game, var timer: Int = -1) : State {
    var messages: MutableList<Message> = mutableListOf()
    var ready: MutableSet<Member> = mutableSetOf()
    var required = 0;

    abstract fun timeout()
    override fun enter() = Unit
    override fun exit() = messages.forEach { it.delete().queue() }

    override fun tick() {
        timer--
        if (ready.size >= neededToContinue) {
            timer = 0
        }
        if (timer > 0) {
            if (timer % 4 == 0) {
                messages.forEach { updateMessage(it) }
            }
            return
        }
        if (timer == 0) {
            timeout()
        }
    }

    override fun handleReaction(member: Member, reaction: MessageReaction) {
        if (reaction.reactionEmote.isEmoji && reaction.reactionEmote.emoji == "✅" && member in game.members) {
            ready.add(member)
        }
    }

    fun add(message: Message) {
        messages.add(message)
        updateMessage(message)
    }

    fun addReaction(message: Message) {
        message.addReaction("✅").queue()
        required++
        add(message)
    }

    private fun updateMessage(message: Message) {
        message.embeds.getOrNull(0)?.let { embed ->
            message.editMessage(EmbedBuilder(embed)
                    .setFooter("The game will continue in $timer ${English.plural("second", timer)}. " +
                            "${ready.size}/$neededToContinue players ready.")
                    .build()).queue()
        }
    }

    private val neededToContinue get() = if (required > 0) required else game.players.size
}

class NoOpState(private val mock: GameState) : State {
    override val state get() = mock
    override fun enter() = Unit
    override fun exit() = Unit
    override fun tick() = Unit
}

object WaitingState : State {
    override val state = GameState.WAITING
}