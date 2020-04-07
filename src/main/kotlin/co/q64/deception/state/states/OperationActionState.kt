package co.q64.deception.state.states

import co.q64.deception.Game
import co.q64.deception.state.BasicState
import co.q64.deception.state.GameState
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.reaction.ReactionEmoji
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class OperationActionState(game: Game) : BasicState(game, 60) {
    override val state get() = GameState.OPERATION_ACTION

    override fun enter(): Mono<Void> =
            game.mute().and(
                    Flux.fromIterable(game.players)
                            .filter { it != game.selected }
                            .flatMap { player ->
                                game.theme.operationActionWait(game.selected?.member
                                        ?: player.member).flatMap { embed ->
                                    player.channel?.createEmbed { embed(it) }
                                }
                            }.flatMap { add(it) }
            ).and((game.selected?.let { player ->
                player.operation.message(player).flatMap { embed ->
                    player.channel?.createEmbed {
                        it.setTitle(player.operation.title)
                        embed(it)
                    }
                }
            } ?: Mono.empty())
                    .flatMap {
                        game.selected?.let { player ->
                            (if (player.operation.automatic) addReaction(it) else add(it))
                                    .then(player.operation.handleMessage(player, it))
                        } ?: Mono.empty()
                    })

    override fun handleReaction(member: Member, message: Message, reaction: ReactionEmoji): Mono<Void> =
            super.handleReaction(member, message, reaction).and(if (member in game.players.map { it.member }) {
                game.selected?.operation?.handleReaction(game.players.first { it.member == member }, message, this, reaction)
                        ?: Mono.empty()
            } else Mono.empty())


    override fun exit(): Mono<Void> = super.exit().and(game.unmute())
    override fun timeout() = game.enter(GameState.OPERATION_DISCUSS)
}