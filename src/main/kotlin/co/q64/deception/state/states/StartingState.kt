package co.q64.deception.state.states

import co.q64.deception.Game
import co.q64.deception.state.BasicState
import co.q64.deception.state.GameState
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class StartingState(game: Game) : BasicState(game, 30) {
    override val state = GameState.STARTING

    override fun enter(): Mono<Void> = Flux.fromIterable(game.players)
            .filter { it.channel != null }
            .flatMap { player ->
                (player.channel?.createMessage(player.member.mention) ?: Mono.empty()).then(
                        (player.channel?.createEmbed { embed ->
                            embed.setTitle("Game Channel Created")
                                    .setDescription("""
                                            You will receive all game communication through this channel.
                                            The game will begin shortly.
                                            
                                            React with ✅ when you have read this message.
                                        """.trimIndent())
                        } ?: Mono.empty()).flatMap { addReaction(it) }
                )
            }.then()

    override fun timeout() = game.enter(GameState.INTRO)
}