package co.q64.deception.theme.agent

import co.q64.deception.Game
import co.q64.deception.Player
import co.q64.deception.theme.Operation
import co.q64.deception.theme.Role
import co.q64.deception.theme.Theme
import discord4j.core.`object`.entity.Member
import discord4j.core.spec.EmbedCreateSpec
import reactor.core.publisher.Mono
import java.util.concurrent.ThreadLocalRandom

object AgentTheme : Theme {
    override val minPlayers = 5
    override val maxPlayers = 9

    override val player get() = ServiceTeam
    override val traitor get() = VirusTeam

    override fun traitorCount(players: Int) = 2
    override fun roleCount(players: Int) = (if (players > 5) 2 else 1) + ThreadLocalRandom.current().nextInt(3)

    override fun intro(): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setTitle("Mission Overview")
                .setDescription("You've received intelligence that 2 of the 5 of you are double agents working for ${VirusTeam.name}! " +
                        "Your briefing agent will tell you which agency you work for and your objective. You can only imprison one person so make sure " +
                        "it's the right one. Remember each other's names, review the below agencies, and figure out who is lying about who they are." + """
                            
                            **The service**  
                            **VIRUS**
                            
                            React with ✅ when you have read this message.
                        """.trimIndent())
    }

    override fun roleAssignmentStart(member: Member): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setTitle("Incoming Briefing")
                .setDescription("${member.mention} is about to receive a confidential mission briefing. Voice chat will be disabled during this time.\n\n" +
                        "React with ✅ when you have read this message.")
    }

    override fun roleAssignmentWait(member: Member): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setTitle("Pleas Wait")
                .setDescription("Please wait while ${member.mention} receives a confidential mission briefing.")
    }

    override fun roleAssignmentDiscuss(member: Member): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setTitle("Discussion Time")
                .setDescription("Voice chat has been enabled. ${member.mention} may now tell the truth or lie about their new information.\n\n" +
                        "React with ✅ when you have completed the discussion.")
    }

    override fun operationIntro(): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setTitle("Operations Phase").setDescription("Each player will be given one operation before moving on to the Accusation Phase\n\n" +
                "React with ✅ when you have completed the discussion.")
    }

    override fun operationActionWait(member: Member): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setTitle("Pleas Wait").setDescription("Please wait while ${member.mention} completes a confidential operation.")
    }

    override fun operationDiscuss(member: Member): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setTitle("Discussion Time").setDescription("Voice chat has been enabled. ${member.mention} may now tell the truth or lie about their new information.\n\n" +
                "React with ✅ when you have completed the discussion.")
    }

    override fun accusationIntro(game: Game): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setTitle("Discussion Phase").setDescription("Discuss\n\n" +
                game.players.joinToString("\n") { "${it.member.mention} (${it.operation.title})" } +
                "\n\nReact with ✅ when you have completed the discussion.")
    }

    override fun accusationVote(reactions: String): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setTitle("Vote").setDescription("Pick one\n\n$reactions")
    }

    override fun accusationComplete(member: Member): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setTitle("Complete").setDescription("Voted for ${member.mention}")
    }

    override fun resultsVotes(player: Player, emoji: String): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setDescription("${player.member.mention} received $emoji votes.")
    }

    override fun resultsNoSelection(): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setDescription("No one was imprisoned.")
    }

    override fun resultsSelected(target: Player): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setDescription("${target.member.mention} was imprisoned and ${target.member.mention} worked for **${target.team.name}**.")
    }

    override fun resultsListEntry(target: Player): String = "${target.member.mention} [**" +
            when (target.team) {
                target.startingTeam -> target.team.name
                else -> "${target.startingTeam.name} -> ${target.team.name}"
            } + "**] " +
            when (target.role.display) {
                true -> "(__${target.role.name}__) "
                else -> ""
            } +
            target.operation.let { operation ->
                when (operation) {
                    is ScapegoatOperation -> "Wanted to be imprisoned"
                    is GrudgeOperation -> "Wanted ${operation.target.member.mention} to be imprisoned"
                    is InfatuationOperation -> "Wanted ${operation.target.member.mention} to win"
                    else -> ""
                }
            }


    override fun calculateVotes(player: Player) = player.votes + player.operation.let { operation ->
        when (operation) {
            is IncriminatingEvidenceOperation -> when {
                operation.action == IncriminatingEvidenceOperation.Action.ADD -> 1
                operation.action == IncriminatingEvidenceOperation.Action.SHIELD && player.votes > 0 -> -1
                else -> 0
            }
            else -> 0
        }
    }

    override fun winner(player: Player, selected: Player?): Boolean = (selected?.team ?: ServiceTeam).let { team ->
        player.operation.let { operation ->
            when {
                operation is ScapegoatOperation -> player == selected
                selected?.operation is ScapegoatOperation -> false
                operation is InfatuationOperation -> winner(operation.target, selected)
                operation is GrudgeOperation -> operation.target == selected
                player.team == team.other -> true
                else -> false
            }
        }
    }

    override fun generateRoles(): List<Role> = listOf(
            RougeAgentRole,
            TripleAgentRole,
            DeepCoverAgentRole,
            SuspiciousAgentRole,
            ServiceLoyalistRole,
            VirusLoyalistRole
    )

    override fun generateOperations(game: Game): List<Operation> = listOf(
            SpyTransferOperation,
            ConfessionOperation,
            AnonymousTipOperation,
            DanishIntelligenceOperation,
            OldPhotographsOperation,
            DeepUndercoverOperation,
            UnfortunateEncounterOperation,
            DefectorOperation,
            ScapegoatOperation,
            GrudgeOperation(game.players.random()),
            InfatuationOperation(game.players.random()),
            SleeperAgentOperation,
            SecretTipOperation
    )
}