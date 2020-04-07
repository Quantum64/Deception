package co.q64.deception

import discord4j.core.`object`.entity.channel.Channel
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.spec.EmbedCreateSpec

fun test() {
    val c: TextChannel? = null
    val channel: TextChannel = c!!

    channel.createEmbed { EmbedCreateSpec().setTitle("Test").setDescription("test") }
            .flatMap { it.addReaction(ReactionEmoji.unicode("✅")) }
            .subscribe()

    channel.createEmbed { EmbedCreateSpec().setTitle("Test").setDescription("test") }
            .subscribe {
                it.addReaction(ReactionEmoji.unicode("✅")).subscribe()
            }
}