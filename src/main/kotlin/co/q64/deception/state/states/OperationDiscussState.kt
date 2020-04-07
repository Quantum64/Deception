package co.q64.deception.state.states

import co.q64.deception.Game
import co.q64.deception.state.BasicState
import co.q64.deception.state.GameState
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class OperationDiscussState(game: Game) : BasicState(game, 120) {
    override val state get() = GameState.ASSIGNMENT_DISCUSS

    override fun enter(): Mono<Void> = Flux.fromIterable(game.players)
            .flatMap { player ->
                game.theme.operationDiscuss(game.selected?.member ?: player.member).flatMap { embed ->
                    player.channel?.createEmbed {
                        embed(it)
                    }
                }

            }
            .flatMap { addReaction(it) }
            .then()

    override fun timeout(): Mono<Void> {
        if (game.players.any { !it.receivedOperation }) {
            return game.enter(GameState.OPERATION_START)
        }
        return game.enter(GameState.ACCUSATION_INTRO)
    }
}