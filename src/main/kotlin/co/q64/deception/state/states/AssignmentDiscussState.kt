package co.q64.deception.state.states

import co.q64.deception.Game
import co.q64.deception.state.BasicState
import co.q64.deception.state.GameState
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class AssignmentDiscussState(game: Game) : BasicState(game, 60) {
    override val state get() = GameState.ASSIGNMENT_DISCUSS

    override fun enter(): Mono<Void> = Flux.fromIterable(game.players)
            .flatMap { player ->
                game.theme.roleAssignmentDiscuss(game.selected?.member ?: player.member).flatMap { embed ->
                    player.channel?.createEmbed { embed(it) }
                }
            }
            .flatMap { addReaction(it) }
            .then()

    override fun timeout(): Mono<Void> {
        if (game.players.any { !it.receivedAssignment }) {
            return game.enter(GameState.ASSIGNMENT_START)
        }
        return game.enter(GameState.OPERATION_INTRO)
    }
}