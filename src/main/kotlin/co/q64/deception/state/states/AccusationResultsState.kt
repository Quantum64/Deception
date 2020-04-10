package co.q64.deception.state.states

import co.q64.deception.Game
import co.q64.deception.Player
import co.q64.deception.orEmpty
import co.q64.deception.state.BasicState
import co.q64.deception.state.GameState
import co.q64.deception.state.WaitingState
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono

class AccusationResultsState(game: Game) : BasicState(game, 500) {
    override val state get() = GameState.ACCUSATION_RESULTS

    private val remaining = game.players.toMutableList()

    private var phase: ResultsPhase = ResultsPhase.REVEAL
    private var selected: Player? = null
    private var counter = 0;

    override fun enter(): Mono<Void> {
        selected = game.players.map { player -> game.theme.calculateVotes(player) }.max().let { max ->
            game.players.filter { game.theme.calculateVotes(it) == max }.let { players ->
                when (players.size) {
                    1 -> players.first()
                    else -> null
                }
            }
        }
        return Mono.empty()
    }

    override fun tick(): Mono<Void> = super.tick().doFirst {
        counter++
    }.then(counter.toMono().filter { it.rem(4) == 0 }
            .flatMap {
                when {
                    remaining.size > 0 -> {
                        val next = remaining.minBy { game.theme.calculateVotes(it) }!!
                        remaining.remove(next)
                        game.players.toFlux().flatMap { player ->
                            game.theme.resultsVotes(next, game.theme.calculateVotes(next), numbers[game.theme.calculateVotes(next)]).flatMap { embed ->
                                player?.channel?.createEmbed(embed).orEmpty()
                            }
                        }.then()
                    }
                    phase == ResultsPhase.REVEAL -> {
                        game.players.toFlux().flatMap { player ->
                            selected.let { target ->
                                when (target) { // TODO What team wins
                                    null -> game.theme.resultsNoSelection().flatMap { embed -> player.channel?.createEmbed(embed).orEmpty() }
                                    else -> game.theme.resultsSelected(target).flatMap { embed -> player.channel?.createEmbed(embed).orEmpty() }
                                }
                            }
                        }.doOnComplete { phase = ResultsPhase.LIST }.then()
                    }
                    phase == ResultsPhase.LIST -> {
                        game.players.toFlux().flatMap { player ->
                            player.channel?.createEmbed { embed ->
                                embed.setDescription("__WINNERS__\n" + game.players
                                        .filter { game.theme.winner(it, selected) }
                                        .joinToString("\n") { game.theme.resultsListEntry(it) } +
                                        "\n\n__LOSERS__\n" + game.players
                                        .filter { !game.theme.winner(it, selected) }
                                        .joinToString("\n") { game.theme.resultsListEntry(it) })
                            }.orEmpty()
                        }.doOnComplete { phase = ResultsPhase.END }.then()
                    }
                    phase == ResultsPhase.END -> {
                        game.players.toFlux().flatMap { player ->
                            player.channel?.createEmbed { embed ->
                                embed.setTitle("Game Over").setDescription("React with ✅ to end the game.")
                            }.orEmpty().flatMap { addReaction(it) }
                        }.doOnComplete { phase = ResultsPhase.STOP }.then()
                    }
                    else -> Mono.empty()
                }
            }
    ).then()

    override fun timeout(): Mono<Void> = game.toMono().doOnNext {
        it.players.clear()
        it.state = WaitingState
    }.and(game.undeafen()).and(game.deleteChannels())
}

private enum class ResultsPhase {
    REVEAL, LIST, END, STOP
}

private val numbers = listOf("0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣")