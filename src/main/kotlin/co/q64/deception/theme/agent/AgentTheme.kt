package co.q64.deception.theme.agent

import co.q64.deception.Game
import co.q64.deception.Player
import co.q64.deception.theme.Operation
import co.q64.deception.theme.Role
import co.q64.deception.theme.Theme
import discord4j.core.`object`.entity.Member
import discord4j.core.spec.EmbedCreateSpec
import org.atteo.evo.inflector.English
import reactor.core.publisher.Mono
import java.util.concurrent.ThreadLocalRandom

object AgentTheme : Theme {
    override val minPlayers = 3
    override val maxPlayers = 9

    override val player get() = ServiceTeam
    override val traitor get() = VirusTeam

    override fun traitorCount(players: Int) = 2
    override fun roleCount(players: Int) = (if (players > 5) 2 else 1) + ThreadLocalRandom.current().nextInt(3)

    override fun intro(game: Game): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setTitle("Mission Overview")
                .setDescription("You've received intelligence that 2 of the ${game.players.size} of you are double agents working for ${VirusTeam.name}! " +
                        "Your briefing agent will tell you which agency you work for and your objective. You can only imprison one person so make sure " +
                        "it's the right one. Remember each other's names, review the below agencies, and figure out who is lying about who they are." + """
                            
                        The agencies:
                         -  **${ServiceTeam.name}**
                         -  **${VirusTeam.name}**
                            
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
        embed.setTitle("Discussion Phase").setDescription(game.players.joinToString("\n") { "${it.member.mention} (${it.operation.title})" } +
                "\n\nReact with ✅ when you have completed the discussion.")
    }

    override fun accusationVote(reactions: String): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setTitle("Vote").setDescription("Whoever has the most votes of suspicion will be imprisoned. " +
                "If they are a VIRUS double agent, The Service wins, otherwise the VIRUS agents win.\n\n$reactions")
    }

    override fun accusationNotEligible(player: Player): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setTitle("Can't Vote").setDescription(player.operation.let { operation ->
            when {
                operation is DefectorOperation && operation.defected(player) && player.startingTeam == ServiceTeam ->
                    "You defected from The Service, so you are not eligible to cast a vote."
                else -> "Unknown Reason"
            }
        })
    }

    override fun accusationComplete(member: Member): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setTitle("Complete").setDescription("Voted for ${member.mention}")
    }

    override fun resultsVotes(player: Player, count: Int, emoji: String): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setDescription("${player.member.mention} received $emoji ${English.plural("vote", count)}.")
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
                    is DefectorOperation -> when {
                        operation.defected(target) && target.startingTeam == ServiceTeam -> "Defected from The Service and couldn't vote"
                        operation.defected(target) && target.startingTeam == VirusTeam && target.votes.any { it.team == VirusTeam } ->
                            "Defected from VIRUS and lost due to a vote frm a VIRUS agent"
                        operation.defected(target) && target.startingTeam == VirusTeam -> "Defected from VIRUS"
                        else -> ""
                    }
                    is ScapegoatOperation -> "Wanted to be imprisoned"
                    is GrudgeOperation -> "Wanted ${operation.target.member.mention} to be imprisoned"
                    is InfatuationOperation -> "Wanted ${operation.target.member.mention} to win"
                    else -> ""
                }
            }


    override fun canVote(player: Player) = player.operation.let { operation ->
        when {
            operation is DefectorOperation && operation.defected(player) && player.startingTeam == ServiceTeam -> false
            else -> true
        }
    }

    override fun calculateVotes(player: Player) = player.votes.size + player.operation.let { operation ->
        when (operation) {
            is IncriminatingEvidenceOperation -> when {
                operation.action == IncriminatingEvidenceOperation.Action.ADD -> 1
                operation.action == IncriminatingEvidenceOperation.Action.SHIELD && player.votes.size > 0 -> -1
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
                operation is DefectorOperation && player.startingTeam == VirusTeam && operation.defected(player) ->
                    player.votes.none { it.team == VirusTeam } && player.team == team.other
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
            //SpyTransferOperation,
            //ConfessionOperation,
            //AnonymousTipOperation,
            //DanishIntelligenceOperation,
            //OldPhotographsOperation,
            //DeepUndercoverOperation,
            //UnfortunateEncounterOperation,
            DefectorOperation,
            //ScapegoatOperation,
            //GrudgeOperation(game.players.random()),
            //InfatuationOperation(game.players.random()),
            SleeperAgentOperation,
            SecretTipOperation
    )
}