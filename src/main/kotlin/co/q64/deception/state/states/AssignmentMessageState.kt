package co.q64.deception.state.states

import co.q64.deception.Game
import co.q64.deception.state.BasicState
import co.q64.deception.state.GameState
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

class AssignmentMessageState(game: Game) : BasicState(game, 30) {
    override val state get() = GameState.ASSIGNMENT_MESSAGE

    override fun enter(): Mono<Void> = game.deafen().and(
            game.players.toFlux()
                    .filter { it != game.selected }
                    .flatMap { player ->
                        game.theme.roleAssignmentWait(game.selected?.member ?: player.member).flatMap { embed ->
                            player.channel?.createEmbed { embed(it) }
                        }
                    }
                    .flatMap { add(it) }
                    .then()
    ).and(
            Mono.justOrEmpty(game.selected)
                    .flatMap { player ->
                        player!!.team.assignmentCard(player).flatMap { embed ->
                            player.channel?.createEmbed { embed(it) }
                        }
                    }.flatMap { addReaction(it) }
    )

    override fun exit(): Mono<Void> = super.exit().and(game.undeafen())
    override fun timeout() = game.enter(GameState.ASSIGNMENT_DISCUSS)
}