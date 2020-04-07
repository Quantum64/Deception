package co.q64.deception.theme.agent

import co.q64.deception.Player
import co.q64.deception.orEmpty
import co.q64.deception.state.BasicState
import co.q64.deception.theme.Operation
import co.q64.deception.theme.Team
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.spec.EmbedCreateSpec
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono

// Templates
interface AgendaOperation : Operation {
    override val title get() = "Hidden Agenda"
    override fun description(player: Player) = "${player.member.mention} gets new orders from up top. " +
            "${player.member.mention} could flip sides, get a new win condition, or simply gain information about another agent."
}

interface SelectOperation : Operation {
    override val automatic get() = false

    fun prompt(player: Player): String
    fun select(state: BasicState, player: Player, selected: Player): Mono<Void>

    override fun message(player: Player): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setDescription(prompt(player) + "\n\n" + player.game.players
                .filter { it != player }
                .mapIndexed { index, target -> numbers[index] + " - " + target.member.mention }
                .joinToString("\n"))
    }

    override fun handleMessage(player: Player, message: Message): Mono<Void> =
            Flux.fromIterable(player.game.players.filter { it != player }.indices)
                    .flatMap { message.addReaction(ReactionEmoji.unicode(numbers[it])) }
                    .then()

    override fun handleReaction(player: Player, message: Message, state: BasicState, reaction: ReactionEmoji): Mono<Void> =
            reaction.toMono()
                    .filter { it.asUnicodeEmoji().isPresent }
                    .map { it.asUnicodeEmoji().get() }
                    .map { numbers.indexOf(it.raw) }
                    .filter { index -> index >= 0 && index < player.game.players.filter { it != player }.size }
                    .flatMap { index -> select(state, player, player.game.players.filter { it != player }[index]) }
                    .doOnEach { state.timer = 30 }
                    .then()
}

// Standard Operations

object SpyTransferOperation : SelectOperation {
    override val title get() = "Spy Transfer"
    override fun description(player: Player) = "${player.member.mention} must choose another agent and secretly swap agencies with them. " +
            "They each now work for the agency the other used to work for."

    override fun canAssign(player: Player) = player.role != VirusLoyalistRole && player.role != ServiceLoyalistRole

    override fun prompt(player: Player) = "Choose an agent. You will now work for the agency they used to work for."

    override fun select(state: BasicState, player: Player, selected: Player): Mono<Void> = Mono
            .just(true)
            .doOnEach {
                player.team = player.team.other
                selected.team = selected.team.other
            }
            .flatMap {
                player.channel?.createEmbed {
                    it.setTitle("Agency Swap").setDescription("""
                            You have swapped agencies with ${selected.member.mention}
                            
                            React with ✅ when you have read this message.
                        """.trimIndent())
                }.orEmpty()
            }.flatMap { state.addReaction(it) }
}

object ConfessionOperation : SelectOperation { // TODO do we use actual or apparent role for this?
    override val title = "Confession"
    override fun description(player: Player) = "${player.member.mention} must divulge their agency to one other agent."

    override fun prompt(player: Player) = "You must divulge their agency to one other agent. Select which agent you would like to tell."

    override fun select(state: BasicState, player: Player, selected: Player): Mono<Void> =
            player.channel?.createEmbed {
                it.setTitle("Transmitting Message").setDescription(selected.member.mention + " will receive your confession shortly.")
            }.orEmpty()
                    .flatMap { state.addReaction(it) }
                    .zipWith(selected.channel?.createEmbed {
                        it.setTitle("Confession").setDescription("${player.member.mention} has divulged that they work for **${player.team.name}**. " +
                                "Only you have received this information.")
                    }.orEmpty().flatMap { state.addReaction(it) })
                    .then()
}

// TODO Secret Intel (multi select)

object AnonymousTipOperation : Operation {
    override val title get() = "Anonymous Tip"
    override fun description(player: Player) = "${player.member.mention}'s source knows the agency of one other agent and reveals it to them."
    override fun message(player: Player): Mono<(EmbedCreateSpec) -> Unit> =
            player.game.players.shuffled().firstOrNull { it != player }?.toMono().orEmpty()
                    .map { target ->
                        { embed: EmbedCreateSpec ->
                            embed.setDescription("""
                                Your source reveals that ${target.member.mention} is with **${apparentTeam(player).name}**
                            """.trimIndent())
                            Unit
                        }
                    }
}


object DanishIntelligenceOperation : Operation {
    override val title get() = "Danish Intelligence"
    override fun description(player: Player) = "${player.member.mention} intercepts a transmission with two names. One is a virus agent and one is not."

    override fun message(player: Player): Mono<(EmbedCreateSpec) -> Unit> =
            // TODO Should this use apparent or actual team?
            listOf(
                    player.game.players.shuffled().firstOrNull { it != player && apparentTeam(it) == VirusTeam },
                    player.game.players.shuffled().firstOrNull { it != player && apparentTeam(it) == ServiceTeam }
            ).shuffled().toMono()
                    .map { targets ->
                        { embed: EmbedCreateSpec ->
                            embed.setDescription("""
                                The transmission reveals that one of the following two players is a VIRUS agent, and one is not.
                                ${targets[0]?.member?.mention ?: "Unknown"}
                                ${targets[1]?.member?.mention ?: "Unknown"}
                            """.trimIndent())
                            Unit
                        }
                    }
}

object OldPhotographsOperation : Operation {
    override val title get() = "Old Photographs"
    override fun description(player: Player) = "${player.member.mention} found evidence that shows them the names of two agents that were working for the same agency at the start."

    override fun message(player: Player): Mono<(EmbedCreateSpec) -> Unit> {
        val first = player.game.players.shuffled().firstOrNull { it != player && (player.team == ServiceTeam || it.team == ServiceTeam) }
        val second = player.game.players.shuffled().firstOrNull { it != first && it != player && it.startingTeam == first?.startingTeam }
        if (first == null || second == null) {
            return Mono.just { embed -> embed.setDescription("There are not enough agents to provide you with any useful information.") }
        }
        return Mono.just { embed -> embed.setDescription("Evidence reveals that ${first.member.mention} and ${second.member.mention} started on the same team.") }
    }
}

object DeepUndercoverOperation : SelectOperation { // TODO again, actual or apparent team?
    override val title get() = "Deep Undercover"
    override fun description(player: Player) = "${player.member.mention} picks one agent to discover their true agency. " +
            "If they turn out to be a virus agent, the ${player.member.mention} will join their cause."

    override fun prompt(player: Player) = "Pick an agent and discover their identity. If they are a VIRUS agent, you will join their cause, otherwise you stay in the same agency."
    override fun select(state: BasicState, player: Player, selected: Player): Mono<Void> =
            Mono.just((
                    if (selected.team == VirusTeam) {
                        "You now know that ${selected.member.mention} is a **${VirusTeam.name}** agent." + (if (player.role == ServiceLoyalistRole) {
                            "\n\nYou would have switched to **${selected.team.name}**, but you are a __${player.role.name}__, so your agency has not changed."
                        } else {
                            "\n\nIf you weren't before, you are now working for **${VirusTeam.name}**."
                        })
                    } else "You now know that ${selected.member.mention} is with **${selected.team.name}**.") + "\nReact with ✅ when you have read this message.")
                    .doOnEach {
                        if (selected.team == VirusTeam && player.role != ServiceLoyalistRole) {
                            player.team = VirusTeam
                        }
                    }
                    .flatMap { text ->
                        player.channel?.createEmbed { it.setTitle("Incoming Transmission").setDescription(text) }.orEmpty()
                    }
                    .flatMap { state.addReaction(it) }
}

object UnfortunateEncounterOperation : SelectOperation {
    override val title get() = "Unfortunate Encounter"
    override fun description(player: Player) = "${player.member.mention} picks one agent. They will both see whether both or either of them works for VIRUS."

    override fun prompt(player: Player) = "Select an agent. You will both see if both of you or either of you works for VIRUS."

    override fun select(state: BasicState, player: Player, selected: Player): Mono<Void> =
            listOf(player, selected).shuffled().map { it.member.mention }.toMono()
                    .map { players ->
                        (if (apparentTeam(player) == VirusTeam && apparentTeam(selected) == VirusTeam)
                            "New intelligence reveals that both ${players[0]} and ${players[1]} work for **VIRUS**."
                        else if (apparentTeam(player) == VirusTeam || apparentTeam(selected) == VirusTeam)
                            "New intelligence reveals that either ${players[0]} or ${players[1]} work for **VIRUS**."
                        else
                            "New intelligence reveals that neither ${players[0]} nor ${players[1]} work for **VIRUS**."
                                ) + "\n\nReact with ✅ when you have read this message."
                    }.flatMap { description ->
                        player.channel?.createEmbed { it.setTitle("Incoming Transmission").setDescription(description) }?.flatMap { state.addReaction(it) }.orEmpty()
                                .and(selected.channel?.createEmbed { it.setTitle("Incoming Transmission").setDescription(description) }?.flatMap { state.addReaction(it) }.orEmpty())
                    }
                    .then()
}

// TODO Incriminating Evidence

object DefectorOperation : Operation {
    override val title get() = "Defector"
    override fun description(player: Player) = "${player.member.mention} may defect and join the other agency. A VIRUS defector loses if any other VIRUS agent votes for them. A Service defector can't vote."
    override fun canAssign(player: Player) = player.role != ServiceLoyalistRole && player.role != VirusLoyalistRole

    override fun message(player: Player): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setDescription("You are currently working for **${player.team.name}**. Defecting comes at a cost: " +
                (if (player.team == ServiceTeam)
                    "You won't get to vote in the accusation phase." else
                    "You will lose if any VIRUS agent votes for you.") + "\n\n" +
                "\uD83D\uDFE2 - Stay the same\n" +
                "\uD83D\uDFE5 - Defect"
        )
    }

    override fun handleMessage(player: Player, message: Message): Mono<Void> =
            listOf("\uD83D\uDFE2", "\uD83D\uDFE5").toFlux()
                    .flatMap { message.addReaction(ReactionEmoji.unicode(it)) }
                    .then()

    override fun handleReaction(player: Player, message: Message, state: BasicState, reaction: ReactionEmoji): Mono<Void> =
            reaction.toMono()
                    .doOnEach { state.timer = 1 }
                    .filter { it.asUnicodeEmoji().isPresent }
                    .map { it.asUnicodeEmoji().get().raw }
                    .filter { it == "\uD83D\uDFE5" }
                    .doOnEach { player.team = player.team.other }
                    .then()
}

// Hidden Agendas
object ScapegoatOperation : AgendaOperation {
    override fun message(player: Player): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { message ->
        message.setTitle("Scapegoat").setDescription("""
        You now win if and only if you yourself are imprisoned. Try to trick the other agents into voting for you in the accusation phase.
        If you succeed The Service and VIRUS agents lose. Your agency is still the same.
        
        Tip: Try telling everyone you were VIRUS but are now part of The Service.
    """.trimIndent())
    }
}

class GrudgeOperation(private val target: Player) : AgendaOperation {
    override fun message(player: Player): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { message ->
        message.setTitle("Grudge").setDescription("""
            You have a grudge against ${target.member.mention}
        """.trimIndent())
    }

    override fun canAssign(player: Player) = player != target
}

class InfatuationOperation(private val target: Player) : AgendaOperation {
    override fun message(player: Player): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { message ->
        message.setTitle("Infatuation").setDescription("""
            You are love with ${target.member.mention}
        """.trimIndent())
    }

    override fun canAssign(player: Player) = player != target
}

object SleeperAgentOperation : AgendaOperation {
    override fun message(player: Player): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { message ->
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

object SecretTipOperation : AgendaOperation {
    override fun message(player: Player): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { message ->
        val target = player.game.players.shuffled().first { it != player }
        message.setTitle("Secret Tip").setDescription("""
            You get a strange phone call. It reveals that ${target.member.mention} works for **${apparentTeam(target)}**.
            
            Tip: Try lying about your info to see how the agent reacts to being accused.
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