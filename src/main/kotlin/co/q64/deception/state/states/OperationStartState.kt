package co.q64.deception.state.states

import co.q64.deception.Game
import co.q64.deception.state.BasicState
import co.q64.deception.state.GameState
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class OperationStartState(game: Game) : BasicState(game, 45) {
    override val state get() = GameState.OPERATION_START

    override fun enter(): Mono<Void> {
        game.selected = game.players.first { !it.receivedOperation }
        game.selected?.receivedOperation = true

        return Flux
                .fromIterable(game.players)
                .map { it.channel }
                .filter { it != null }
                .flatMap {
                    it!!.createEmbed { embed ->
                        embed.setTitle(game.selected?.operation?.title ?: "Unknown Operation")
                                .setDescription("""
                                    ${game.selected?.let { it.operation.description(it) }}
                                    
                                    Voice chat will be disabled as ${game.selected?.member?.mention} completes their operation.
                                    React with ✅ when you have read this message.
                                """.trimIndent())
                    }
                }
                .flatMap { addReaction(it) }
                .then()
    }

    override fun timeout() = game.enter(GameState.OPERATION_ACTION)
}