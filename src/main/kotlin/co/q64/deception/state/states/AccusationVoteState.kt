package co.q64.deception.state.states

import co.q64.deception.Game
import co.q64.deception.numbers
import co.q64.deception.orEmpty
import co.q64.deception.state.BasicState
import co.q64.deception.state.GameState
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.reaction.ReactionEmoji
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono

class AccusationVoteState(game: Game) : BasicState(game, 60) {
    override val state = GameState.ACCUSATION_INTRO

    override fun enter(): Mono<Void> = game.deafen().and(game.players.toFlux()
            .flatMap { player ->
                game.theme.accusationVote(
                        game.players
                                .filter { it != player }
                                .map { it.member }
                                .mapIndexed { index, member -> "${numbers[index]} - ${member.mention}" }
                                .joinToString("\n")
                ).flatMap { embed ->
                    player.channel?.createEmbed { embed(it) }
                }.flatMap { message ->
                    (game.players.filter { it != player }.indices).toFlux()
                            .flatMap { message.addReaction(ReactionEmoji.unicode(numbers[it])) }
                            .then(add(message))
                }
            })
            .then()


    override fun handleReaction(member: Member, message: Message, reaction: ReactionEmoji): Mono<Void> =
            super.handleReaction(member, message, reaction).then(
                    member.toMono().flatMap {
                        game.players.firstOrNull { it.member == member }?.toMono().orEmpty()
                    }.flatMap { player ->
                        player.toMono().flatMap {
                            reaction.toMono()
                                    .filter { it.asUnicodeEmoji().isPresent }
                                    .map { it.asUnicodeEmoji().get() }
                                    .map { numbers.indexOf(it.raw) }
                                    .filter { index -> index >= 0 && index < player.game.players.filter { it != player }.size }
                                    .map { index -> player.game.players.filter { it != player }[index] }
                                    .flatMap { target ->
                                        target.votes++
                                        player.voteCast = true
                                        message.removeAllReactions().then(message.toMono()
                                                .flatMap { message.channel }
                                                .flatMap { channel ->
                                                    game.theme.accusationComplete(target.member).flatMap { embed ->
                                                        channel.createEmbed { embed(it) }
                                                    }
                                                }.flatMap { addReaction(it) })
                                    }
                                    .then()
                        }
                    }.then())

    override fun exit(): Mono<Void> = super.exit().and(game.undeafen())
    override fun timeout(): Mono<Void> =
            game.players.toFlux().filter { !it.voteCast }.doOnNext { player ->
                game.players.shuffled().first { it != player }.votes++
                player.voteCast = true
            }.then(game.enter(GameState.ACCUSATION_RESULTS))
}