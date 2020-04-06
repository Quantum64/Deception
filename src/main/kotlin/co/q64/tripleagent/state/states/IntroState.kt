package co.q64.tripleagent.state.states

import co.q64.tripleagent.Game
import co.q64.tripleagent.state.BasicState
import co.q64.tripleagent.state.GameState
import net.dv8tion.jda.api.EmbedBuilder

class IntroState(game: Game) : BasicState(game, 60) {
    override val state = GameState.INTRO

    override fun enter() {
        for (channel in game.players.mapNotNull { it.channel }) {
            channel.sendMessage(EmbedBuilder()
                    .setTitle(game.theme.introTitle)
                    .setDescription(game.theme.introDescription)
                    .build()).queue { addReaction(it) }
        }
    }

    override fun timeout() = game.enter(GameState.ASSIGNMENT_START)
}