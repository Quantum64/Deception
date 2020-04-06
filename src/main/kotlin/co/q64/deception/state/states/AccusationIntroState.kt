package co.q64.deception.state.states

import co.q64.deception.Game
import co.q64.deception.state.BasicState
import co.q64.deception.state.GameState
import net.dv8tion.jda.api.EmbedBuilder

class AccusationIntroState(game: Game) : BasicState(game, 60) {
    override val state = GameState.ACCUSATION_INTRO

    override fun enter() {
        for (channel in game.players.mapNotNull { it.channel }) {
            channel.sendMessage(EmbedBuilder()
                    .setTitle(game.theme.operationsTitle)
                    .setDescription(game.theme.operationsDescription)
                    .build()).queue { addReaction(it) }
        }
    }

    override fun timeout() = game.enter(GameState.OPERATION_START)
}