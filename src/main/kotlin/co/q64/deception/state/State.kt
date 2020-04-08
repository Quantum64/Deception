package co.q64.deception.state

import co.q64.deception.Game
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.reaction.ReactionEmoji
import org.atteo.evo.inflector.English
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface State {
    val state: GameState
    fun enter(): Mono<Void> = Mono.empty()
    fun exit(): Mono<Void> = Mono.empty()
    fun tick(): Mono<Void> = Mono.empty()
    fun handleReaction(member: Member, message: Message, reaction: ReactionEmoji): Mono<Void> = Mono.empty()
}

abstract class BasicState(val game: Game, var timer: Int = -1) : State {
    var messages: MutableList<Message> = mutableListOf()
    var ready: MutableSet<Member> = mutableSetOf()
    var required = 0;

    abstract fun timeout(): Mono<Void>
    override fun enter(): Mono<Void> = Mono.just(true).then()
    override fun exit(): Mono<Void> = Flux.fromIterable(messages).flatMap { it.delete() }.then()

    override fun tick(): Mono<Void> {
        timer--
        if (ready.size >= neededToContinue) {
            timer = 0
        }
        if (timer > 0) {
            if (timer % 2 == 0) {
                return Flux.fromIterable(messages.filterNotNull()).flatMap { updateMessage(it) }.then()
            }
            return Mono.empty()
        }
        if (timer == 0) {
            return timeout()
        }
        return Mono.empty()
    }

    override fun handleReaction(member: Member, message: Message, reaction: ReactionEmoji): Mono<Void> {
        if (reaction.asUnicodeEmoji().isPresent && reaction.asUnicodeEmoji().get().raw == "✅" && member in game.players.map { it.member }) {
            ready.add(member)
        }
        return Mono.empty()
    }

    fun add(message: Message): Mono<Void> =
            updateMessage(message).doOnEach {
                messages.add(message)
            }.then()

    fun addReaction(message: Message): Mono<Void> =
            message.addReaction(ReactionEmoji.unicode("✅")).doFirst {
                required++
            }.and(add(message))

    private fun updateMessage(message: Message): Mono<Void> =
            message.embeds.getOrNull(0)?.let { embed ->
                message.edit { message ->
                    message.setEmbed { new ->
                        embed.title.ifPresent { new.setTitle(it) }
                        embed.description.ifPresent { new.setDescription(it) }
                        embed.color.ifPresent { new.setColor(it) }
                        new.setFooter("The game will continue in $timer ${English.plural("second", timer)}. " +
                                "${ready.size}/$neededToContinue players ready.", null)
                    }
                }.then()
            } ?: Mono.just(true).then()

    private val neededToContinue get() = if (required > 0) required else game.players.size
}

class NoOpState(private val mock: GameState) : State {
    override val state get() = mock
    override fun enter() = Mono.just(true).then()
    override fun exit() = Mono.just(true).then()
    override fun tick() = Mono.just(true).then()
}

object WaitingState : State {
    override val state = GameState.WAITING
}