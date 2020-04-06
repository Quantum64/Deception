package co.q64.deception.theme.agent

import co.q64.deception.Player
import co.q64.deception.state.BasicState
import co.q64.deception.theme.Operation
import co.q64.deception.theme.Team
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageReaction

// Templates
abstract class AgendaOperation : Operation {
    override val title get() = "Hidden Agenda"
    override fun description(player: Player) = "${player.member.asMention} gets new orders from up top. " +
            "${player.member.asMention} could flip sides, get a new win condition, or simply gain information about another agent."
}

abstract class SelectOperation : Operation {
    override val automatic get() = false

    abstract fun prompt(player: Player): String
    abstract fun select(state: BasicState, player: Player, selected: Player)

    override fun message(player: Player, message: EmbedBuilder) {
        message.setDescription(prompt(player) + "\n\n" + player.game.players
                .filter { it != player }
                .mapIndexed { index, target -> numbers[index] + " - " + target.member.asMention }
                .joinToString("\n"))
    }

    override fun handleMessage(player: Player, message: Message) {
        for (number in player.game.players.filter { it != player }.indices) {
            message.addReaction(numbers[number]).queue()
        }
    }

    override fun handleReaction(player: Player, state: BasicState, reaction: MessageReaction) {
        if (reaction.reactionEmote.isEmoji && reaction.reactionEmote.emoji in numbers) {
            val index = numbers.indexOf(reaction.reactionEmote.emoji)
            if (index < player.game.players.filter { it != player }.size) {
                select(state, player, player.game.players.filter { it != player }[index])
                state.timer = 30
            }
        }
    }
}

// Standard Operations

object SpyTransfer : SelectOperation() {
    override val title get() = "Spy Transfer"
    override fun description(player: Player) = "${player.member.asMention} must choose another agent and secretly swap agencies with them. " +
            "They each now work for the agency the other used to work for."

    override fun canAssign(player: Player) = player.role != VirusLoyalistRole && player.role != ServiceLoyalistRole

    override fun prompt(player: Player) = "Select an agent to swap agencies with."

    override fun select(state: BasicState, player: Player, selected: Player) {
        player.team = player.team.other
        selected.team = selected.team.other
        player.channel?.sendMessage(EmbedBuilder().setTitle("Agency Swap").setDescription("""
            You have swapped agencies with ${selected.member.asMention}
            You are now with **${player.team.name}**.
            
            React with ✅ when you have read this message.
        """.trimIndent()).build())?.queue { state.addReaction(it) }
        // TODO
    }
}

object Confession : SelectOperation() { // TODO do we use actual or apparent role for this?
    override val title = "Confession"
    override fun description(player: Player) = "${player.member.asMention} must divulge their agency to one other agent."

    override fun prompt(player: Player) = "You must divulge their agency to one other agent. Select which agent you would like to tell."

    override fun select(state: BasicState, player: Player, selected: Player) {
        player.channel?.sendMessage(EmbedBuilder().setTitle("Transmitting Message").setDescription(selected.member.asMention +
                " will receive your confession shortly.").build())?.queue { state.add(it) }
        selected.channel?.sendMessage(EmbedBuilder().setTitle("Confession").setDescription("${player.member.asMention} has divulged that they work for **${player.team.name}**. " +
                "Only you have received this information.").build())?.queue { state.addReaction(it) }
    }
}

// TODO Secret Intel (multi select)

object AnonymousTipOperation : Operation {
    override val title get() = "Anonymous Tip"
    override fun description(player: Player) = "${player.member.asMention}'s source knows the agency of one other agent and reveals it to them."
    override fun message(player: Player, message: EmbedBuilder) {
        val target = player.game.players.shuffled().first { it != player }
        message.setDescription("""
            Your source reveals that ${target.member.asMention} is with **${apparentTeam(player).name}**
        """.trimIndent())
    }
}

object DanishIntelligenceOperation : Operation {
    override val title get() = "Danish Intelligence"
    override fun description(player: Player) = "${player.member.asMention} intercepts a transmission with two names. One is a virus agent and one is not."

    override fun message(player: Player, message: EmbedBuilder) {
        // TODO Should this use apparent or actual team?
        val targets = listOf(
                player.game.players.shuffled().first { it != player && apparentTeam(it) == VirusTeam },
                player.game.players.shuffled().first { it != player && apparentTeam(it) == ServiceTeam }
        ).shuffled()
        message.setDescription("""
            The transmission reveals that one of the following two players is a VIRUS agent, and one is not.
            ${targets[0].member.asMention}
            ${targets[1].member.asMention}
        """.trimIndent())
    }
}

object OldPhotographsOperation : Operation {
    override val title get() = "Old Photographs"
    override fun description(player: Player) = "${player.member.asMention} found evidence that shows them the names of two agents that were working for the same agency at the start."

    override fun message(player: Player, message: EmbedBuilder) {
        val first = player.game.players.shuffled().firstOrNull { it != player && (player.team == ServiceTeam || it.team == ServiceTeam) }
        val second = player.game.players.shuffled().firstOrNull { it != first && it != player && it.startingTeam == first?.startingTeam }
        if (first == null || second == null) {
            message.setDescription("There are not enough agents to provide you with any useful information.") //TODO is this possible?
            return
        }
        message.setDescription("Evidence reveals that ${first.member.asMention} and ${second.member.asMention} started on the same team.")
    }
}

object DeepUndercover : SelectOperation() { // TODO again, actual or apparent team?
    override val title get() = "Deep Undercover"
    override fun description(player: Player) = "${player.member.asMention} picks one agent to discover their true agency. " +
            "If they turn out to be a virus agent, the ${player.member.asMention} will join their cause."

    override fun prompt(player: Player) = "Select a player to discover their true agency."
    override fun select(state: BasicState, player: Player, selected: Player) {
        val text = "Intelligence reveals that ${selected.member.asMention} is with **${selected.team.name}**." +
                if (selected.team == VirusTeam) {
                    if (player.role == ServiceLoyalistRole) {
                        "\n\nYou would have switched to **${selected.team.name}**, but you are a __${player.role.name}__, so your agency has not changed."
                    } else {
                        "\n\nYou were already with **${player.team.name}**, so your agency has not changed."
                    }
                } else "" + "\nReact with ✅ when you have read this message."
        player.channel?.sendMessage(EmbedBuilder().setTitle("Incoming Transmission").setDescription(text).build())?.queue { state.addReaction(it) }
    }
}

object UnfortunateEncounter : SelectOperation() {
    override val title get() = "Unfortunate Encounter"
    override fun description(player: Player) = "${player.member.asMention} picks one agent. They will both see whether both or either of them works for VIRUS."

    override fun prompt(player: Player) = "Select an agent. You will both see if both of you or either of you works for VIRUS."

    override fun select(state: BasicState, player: Player, selected: Player) {
        val players = listOf(player, selected).shuffled().map { it.member.asMention }
        val embed = EmbedBuilder().setTitle("Incoming Transmission").setDescription((
                if (apparentTeam(player) == VirusTeam && apparentTeam(selected) == VirusTeam)
                    "New intelligence reveals that both ${players[0]} and ${players[1]} work for VIRUS."
                else if (apparentTeam(player) == VirusTeam || apparentTeam(selected) == VirusTeam)
                    "New intelligence reveals that either ${players[0]} or ${players[1]} work for VIRUS."
                else
                    "New intelligence reveals that neither ${players[0]} nor ${players[1]} work for VIRUS."
                ) + "\n\nReact with ✅ when you have read this message.").build()
        player.channel?.sendMessage(embed)?.queue { state.addReaction(it) }
        selected.channel?.sendMessage(embed)?.queue { state.addReaction(it) }
    }
}

// TODO Incriminating Evidence

// TODO Defector (custom select)

// Hidden Agendas
object ScapegoatOperation : AgendaOperation() {
    override fun message(player: Player, message: EmbedBuilder) {
        message.setTitle("Scapegoat").setDescription("""
            You now win if and only if you yourself are imprisoned. Try to trick the other agents into voting for you in the accusation phase.
            If you succeed The Service and VIRUS agents lose. Your agency is still the same.
            
            Tip: Try telling everyone you were VIRUS but are now part of The Service.
        """.trimIndent())
    }
}

class GrudgeOperation(private val target: Player) : AgendaOperation() {
    override fun message(player: Player, message: EmbedBuilder) {
        message.setTitle("Grudge").setDescription("""
            You have a grudge against ${target.member.asMention}
        """.trimIndent())
    }

    override fun canAssign(player: Player) = player != target
}

class InfatuationOperation(private val target: Player) : AgendaOperation() {
    override fun message(player: Player, message: EmbedBuilder) {
        message.setTitle("Infatuation").setDescription("""
            You are love with ${target.member.asMention}
        """.trimIndent())
    }

    override fun canAssign(player: Player) = player != target
}

object SleeperAgentOperation : AgendaOperation() {
    override fun message(player: Player, message: EmbedBuilder) {
        message.setTitle("Sleeper Agent")
        if (canSwitch(player)) {
            message.setDescription("""
                You switch sides. You were with **${player.team.name}** but now you are with **${player.team.other.name}**
            """.trimIndent())
            player.team = player.team.other
        } else {
            message.setDescription("""
                You would have switches sides, but you are a __${player.role.name}__ so you are still with **${player.team.name}**.
            """.trimIndent())
        }
    }
}

object SecretTipOperation : AgendaOperation() {
    override fun message(player: Player, message: EmbedBuilder) {
        val target = player.game.players.shuffled().first { it != player }
        message.setTitle("Sleeper Agent").setDescription("""
            You have found that ${target.member.asMention} is with **${target.team.name}**
        """.trimIndent())
    }
}

private val numbers = listOf("1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣")

private fun canSwitch(player: Player): Boolean = player.role != ServiceLoyalistRole && player.role != VirusLoyalistRole

private fun apparentTeam(player: Player): Team = when (player.role) {
    is DeepCoverAgentRole -> ServiceTeam
    is SuspiciousAgentRole -> VirusTeam
    else -> player.team
}