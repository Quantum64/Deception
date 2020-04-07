package co.q64.deception.state.states

import co.q64.deception.Game
import co.q64.deception.state.BasicState
import co.q64.deception.state.GameState
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class AssignmentStartState(game: Game) : BasicState(game, 15) {
    override val state get() = GameState.ASSIGNMENT_START

    override fun enter(): Mono<Void> {
        game.selected = game.players.first { !it.receivedAssignment }
        game.selected?.receivedAssignment = true
        return Flux.fromIterable(game.players)
                .flatMap { player ->
                    game.theme.roleAssignmentStart(game.selected?.member ?: player.member).flatMap { embed ->
                        player.channel?.createEmbed { embed(it) }
                    }
                }
                .flatMap { addReaction(it) }
                .then()
    }

    override fun timeout() = game.enter(GameState.ASSIGNMENT_MESSAGE)
}