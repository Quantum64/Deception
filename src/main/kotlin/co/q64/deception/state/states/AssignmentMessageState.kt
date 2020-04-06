package co.q64.deception.state.states

import co.q64.deception.Game
import co.q64.deception.state.BasicState
import co.q64.deception.state.GameState
import net.dv8tion.jda.api.EmbedBuilder

class AssignmentMessageState(game: Game) : BasicState(game, 30) {
    override val state get() = GameState.ASSIGNMENT_MESSAGE

    override fun enter() {
        game.mute()
        for (player in game.players) {
            if (player == game.selected) continue
            player.channel?.sendMessage(EmbedBuilder()
                    .setTitle(game.theme.roleAssignmentMessageWaitTitle)
                    .setDescription("""
                            ${game.theme.roleAssignmentMessageWaitDescription(game.selected?.member?.asMention ?: "Unknown")}
                        """.trimIndent())
                    .build())?.queue { add(it) }
        }
        game.selected?.channel?.sendMessage(EmbedBuilder()
                .setTitle("Mission Briefing")
                .setDescription(game.selected?.let { it.team.assignmentCard(it) })
                .build())?.queue { addReaction(it) }
    }

    override fun exit() {
        super.exit()
        game.unmute()
    }

    override fun timeout() = game.enter(GameState.ASSIGNMENT_DISCUSS)
}