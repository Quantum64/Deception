package co.q64.deception.theme.agent

import co.q64.deception.Game
import co.q64.deception.theme.Operation
import co.q64.deception.theme.Role
import co.q64.deception.theme.Theme
import java.util.concurrent.ThreadLocalRandom

object AgentTheme : Theme {
    override val minPlayers = 5
    override val maxPlayers = 9

    override val player get() = ServiceTeam
    override val traitor get() = VirusTeam

    override fun traitorCount(players: Int) = 2
    override fun roleCount(players: Int) = (if (players > 5) 2 else 1) + ThreadLocalRandom.current().nextInt(2)

    override val introTitle get() = "Mission Overview"
    override val introDescription
        get() = "You've received intelligence that 2 of the 5 of you are double agents working for ${VirusTeam.name}! " +
                "Your briefing agent will tell you which agency you work for and your objective. You can only imprison one person so make sure " +
                "it's the right one. Remember each other's names, review the below agencies, and figure out who is lying about who they are." + """
                    
                    **The service**  
                    **VIRUS**
                """.trimIndent()

    override val roleAssignmentStartTitle get() = "Incoming Briefing"
    override fun roleAssignmentStartDescription(mention: String) =
            "$mention is about to receive a confidential mission briefing. Voice chat will be disabled during this time."

    override val roleAssignmentMessageWaitTitle get() = "Pleas Wait"
    override fun roleAssignmentMessageWaitDescription(mention: String) =
            "Please wait while $mention receives a confidential mission briefing."

    override val roleAssignmentDiscussTitle get() = "Discussion Time"
    override fun roleAssignmentDiscussDescription(mention: String) =
            "Voice chat has been enabled. $mention may now tell the truth or lie about their new information."

    override val operationActionWaitTitle get() = "Pleas Wait"
    override fun operationActionWaitDescription(mention: String) =
            "Please wait while $mention completes a confidential operation."

    override val operationsTitle get() = "Operations Phase"
    override val operationsDescription get() = "Each player will be given one operation before moving on to the Accusation Phase"

    override val operationDiscussTitle get() = "Discussion Time"
    override fun operationDiscussDescription(mention: String) =
            "Voice chat has been enabled. $mention may now tell the truth or lie about their new information."

    override fun generateRoles(): List<Role> = listOf(
            RougeAgentRole,
            TripleAgentRole,
            DeepCoverAgentRole,
            SuspiciousAgentRole,
            ServiceLoyalistRole,
            VirusLoyalistRole
    )

    override fun generateOperations(game: Game): List<Operation> = listOf(
            SpyTransfer,
            Confession,
            AnonymousTipOperation,
            DanishIntelligenceOperation,
            OldPhotographsOperation,
            DeepUndercover,
            UnfortunateEncounter,
            ScapegoatOperation,
            GrudgeOperation(game.players.random()),
            InfatuationOperation(game.players.random()),
            SleeperAgentOperation,
            SecretTipOperation
    )
}