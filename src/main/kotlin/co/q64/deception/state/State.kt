package co.q64.deception.state

import co.q64.deception.Game
import co.q64.deception.orEmpty
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.reaction.ReactionEmoji
import org.atteo.evo.inflector.English
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono

interface State {
    val state: GameState
    fun enter(): Mono<Void> = Mono.empty()
    fun exit(): Mono<Void> = Mono.empty()
    fun tick(): Mono<Void> = Mono.empty()
    fun handleReaction(member: Member, message: Message, reaction: ReactionEmoji): Mono<Void> = Mono.empty()
}

abstract class BasicState(val game: Game, var timer: Int = -1, var required: Int = 0) : State {
    var messages: MutableList<Message> = mutableListOf()
    var ready: MutableSet<Member> = mutableSetOf()

    abstract fun timeout(): Mono<Void>
    override fun enter(): Mono<Void> = Mono.just(true).then()
    override fun exit(): Mono<Void> = messages
            .filterNotNull()
            .toFlux().flatMap {
        it
                .delete()
                .orEmpty()
                .retry(1)
                .onErrorResume { Mono.empty() }
    }.then()

    override fun tick(): Mono<Void> = timer
            .toMono()
            .map { it - 1 }
            .map { if (ready.size >= neededToContinue && timer > 0) 0 else it }
            .doOnNext { timer = it }
            .flatMap { _ ->
                when {
                    timer > 0 -> when {
                        timer % 2 == 0 -> messages.filterNotNull().toFlux().flatMap { updateMessage(it) }.then()
                        else -> Mono.empty()
                    }
                    timer == 0 -> timeout()
                    else -> Mono.empty()
                }
            }
            .then()

    override fun handleReaction(member: Member, message: Message, reaction: ReactionEmoji): Mono<Void> {
        if (reaction.asUnicodeEmoji().isPresent && reaction.asUnicodeEmoji().get().raw == "✅" && member in game.players.map { it.member }) {
            ready.add(member)
        }
        return Mono.empty()
    }

    fun add(message: Message?): Mono<Void> =
            message?.let { msg ->
                updateMessage(msg).doOnEach {
                    messages.add(msg)
                }.then()
            }.orEmpty()

    fun addReaction(message: Message?): Mono<Void> =
            message?.addReaction(ReactionEmoji.unicode("✅"))?.doFirst {
                if (required >= 0 && required < game.players.size) required++
            }?.and(add(message)).orEmpty()

    private fun updateMessage(message: Message): Mono<Void> =
            message.embeds.getOrNull(0)?.let { embed ->
                message.edit { message ->
                    message.setEmbed { new ->
                        embed.title.ifPresent { new.setTitle(it) }
                        embed.description.ifPresent { new.setDescription(it) }
                        embed.color.ifPresent { new.setColor(it) }
                        embed.thumbnail.ifPresent { new.setThumbnail(it.url) }
                        new.setFooter("The game will continue in $timer ${English.plural("second", timer)}. " +
                                "${ready.size}/$neededToContinue players ready.", null)
                    }
                }.then()
            }.orEmpty()

    private val neededToContinue get() = if (required > 0) required else game.players.size
}

object WaitingState : State {
    override val state = GameState.WAITING
}