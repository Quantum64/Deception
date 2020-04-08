package co.q64.deception.state.states

import co.q64.deception.Game
import co.q64.deception.state.BasicState
import co.q64.deception.state.GameState
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

class AccusationIntroState(game: Game) : BasicState(game, 300) {
    override val state = GameState.ACCUSATION_INTRO

    override fun enter(): Mono<Void> = game.players.toFlux()
            .flatMap { player ->
                game.theme.accusationIntro(game).flatMap { embed ->
                    player.channel?.createEmbed { embed(it) }
                }
            }
            .flatMap { addReaction(it) }
            .then()

    override fun timeout() = game.enter(GameState.ACCUSATION_VOTE)
}