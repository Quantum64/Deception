package co.q64.deception.state.states

import co.q64.deception.Game
import co.q64.deception.state.BasicState
import co.q64.deception.state.GameState
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class IntroState(game: Game) : BasicState(game, 60) {
    override val state = GameState.INTRO

    override fun enter(): Mono<Void> = Flux.fromIterable(game.players).flatMap { player ->
        game.theme.intro().flatMap { embed ->
            player.channel?.createEmbed { embed(it) }
        }.flatMap { addReaction(it) }
    }.then()

    override fun timeout() = game.enter(GameState.ASSIGNMENT_START)
}