package co.q64.tripleagent

import net.dv8tion.jda.api.entities.Message

const val commandPrefix = "!"

fun Message.reply(text: String) {
    channel.sendMessage(author.asMention + " " + text).queue()
}