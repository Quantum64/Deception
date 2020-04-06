package co.q64.tripleagent.state.states

import co.q64.tripleagent.Game
import co.q64.tripleagent.state.BasicState
import co.q64.tripleagent.state.GameState
import net.dv8tion.jda.api.EmbedBuilder

class OperationStartState(game: Game) : BasicState(game, 45) {
    override val state get() = GameState.OPERATION_START

    override fun enter() {
        game.selected = game.players.first { !it.receivedOperation }
        game.selected?.receivedOperation = true
        for (player in game.players) {
            player.channel?.sendMessage(EmbedBuilder()
                    .setTitle(game.selected?.operation?.title ?: "Unknown Operation")
                    .setDescription("""
                            ${game.selected?.let { it.operation.description(it) }}
                            
                            Voice chat will be disabled as ${game.selected?.member?.asMention} completes their operation.
                            React with ✅ when you have read this message.
                        """.trimIndent())
                    .build())?.queue { addReaction(it) }
        }
    }

    override fun timeout() = game.enter(GameState.OPERATION_ACTION)
}