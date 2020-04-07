package co.q64.deception.state.states

import co.q64.deception.Game
import co.q64.deception.state.BasicState
import co.q64.deception.state.GameState
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class AssignmentMessageState(game: Game) : BasicState(game, 30) {
    override val state get() = GameState.ASSIGNMENT_MESSAGE

    override fun enter(): Mono<Void> = game.mute().and(
            Flux.fromIterable(game.players)
                    .filter { it != game.selected }
                    .flatMap { player ->
                        game.theme.roleAssignmentWait(game.selected?.member ?: player.member).flatMap { embed ->
                            player.channel?.createEmbed { embed(it) }
                        }
                    }
                    .flatMap { add(it) }
                    .then()
    ).and(
            Flux.just(game.selected)
                    .filter { it != null }
                    .flatMap { player ->
                        player!!.team.assignmentCard(player).flatMap { embed ->
                            player.channel?.createEmbed { embed(it) }
                        }
                    }.flatMap { addReaction(it) }
    )

    override fun exit(): Mono<Void> = super.exit().and(game.unmute())
    override fun timeout() = game.enter(GameState.ASSIGNMENT_DISCUSS)
}