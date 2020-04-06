package co.q64.tripleagent.state.states

import co.q64.tripleagent.Game
import co.q64.tripleagent.state.BasicState
import co.q64.tripleagent.state.GameState
import net.dv8tion.jda.api.EmbedBuilder

class AssignmentStartState(game: Game) : BasicState(game, 15) {
    override val state get() = GameState.ASSIGNMENT_START

    override fun enter() {
        game.selected = game.players.first { !it.receivedAssignment }
        game.selected?.receivedAssignment = true
        for (player in game.players) {
            player.channel?.sendMessage(EmbedBuilder()
                    .setTitle(game.theme.roleAssignmentStartTitle)
                    .setDescription("""
                            ${game.theme.roleAssignmentStartDescription(game.selected?.member?.asMention ?: "Unknown")}
                            
                            React with ✅ when you have read this message.
                        """.trimIndent())
                    .build())?.queue { addReaction(it) }
        }
    }

    override fun timeout() = game.enter(GameState.ASSIGNMENT_MESSAGE)
}