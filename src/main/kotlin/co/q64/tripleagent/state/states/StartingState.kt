package co.q64.tripleagent.state.states

import co.q64.tripleagent.Game
import co.q64.tripleagent.state.BasicState
import co.q64.tripleagent.state.GameState
import net.dv8tion.jda.api.EmbedBuilder

class StartingState(game: Game) : BasicState(game, 30) {
    override val state = GameState.STARTING

    override fun enter() {
        for (player in game.players) {
            player.channel?.sendMessage(player.member.asMention)?.queue {
                player.channel?.sendMessage(EmbedBuilder()
                        .setTitle("Game Channel Created")
                        .setDescription("""
                                You will receive all game communication through this channel.
                                The game will begin shortly.
                                
                                React with ✅ when you have read this message.
                            """.trimIndent())
                        .build())?.queue { addReaction(it) }
            }
        }
    }

    override fun timeout() = game.enter(GameState.INTRO)
}