package co.q64.deception.state.states

import co.q64.deception.Game
import co.q64.deception.state.BasicState
import co.q64.deception.state.GameState
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageReaction

class OperationActionState(game: Game) : BasicState(game, 60) {
    override val state get() = GameState.OPERATION_ACTION

    override fun enter() {
        game.mute()
        for (player in game.players) {
            if (player == game.selected) continue
            player.channel?.sendMessage(EmbedBuilder()
                    .setTitle(game.theme.operationActionWaitTitle)
                    .setDescription("""
                            ${game.theme.operationActionWaitDescription(game.selected?.member?.asMention ?: "Unknown")}
                        """.trimIndent())
                    .build())?.queue { add(it) }
        }
        game.selected?.let { player ->
            val embed = EmbedBuilder().setTitle(player.operation.title)
            player.operation.message(player, embed)
            player.channel?.sendMessage(embed.build())?.queue {
                if (player.operation.automatic) addReaction(it) else add(it)
                player.operation.handleMessage(player, it)
            }
        }
    }

    override fun handleReaction(member: Member, reaction: MessageReaction) {
        super.handleReaction(member, reaction)
        if (member in game.members) {
            game.selected?.operation?.handleReaction(game.players.first { it.member == member }, this, reaction)
        }
    }

    override fun exit() {
        super.exit()
        game.unmute()
    }

    override fun timeout() = game.enter(GameState.OPERATION_DISCUSS)
}