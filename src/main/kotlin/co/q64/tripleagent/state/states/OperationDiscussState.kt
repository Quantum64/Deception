package co.q64.tripleagent.state.states

import co.q64.tripleagent.Game
import co.q64.tripleagent.state.BasicState
import co.q64.tripleagent.state.GameState
import net.dv8tion.jda.api.EmbedBuilder

class OperationDiscussState(game: Game) : BasicState(game, 120) {
    override val state get() = GameState.ASSIGNMENT_DISCUSS

    override fun enter() {
        for (player in game.players) {
            player.channel?.sendMessage(EmbedBuilder()
                    .setTitle(game.theme.operationDiscussTitle)
                    .setDescription("""
                            ${game.theme.operationDiscussDescription(game.selected?.member?.asMention ?: "Unknown")}
                            
                            React with ✅ when you have completed the discussion.
                        """.trimIndent())
                    .build())?.queue { addReaction(it) }
        }
    }

    override fun timeout() {
        if (game.players.any { !it.receivedOperation }) {
            game.enter(GameState.OPERATION_START)
            return
        }
        game.enter(GameState.ACCUSATION_INTRO)
    }
}