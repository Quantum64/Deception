package co.q64.deception.theme.agent

import co.q64.deception.Game
import co.q64.deception.Player
import co.q64.deception.color
import co.q64.deception.theme.Team
import discord4j.core.`object`.entity.Member
import discord4j.core.spec.EmbedCreateSpec
import reactor.core.publisher.Mono
import java.awt.Color

object ServiceTeam : Team {
    override val name get() = "The Service"
    override val genericName get() = "agent"
    override val other get() = VirusTeam
    override val color get() = color(0, 0, 160)
    override fun assignmentCard(player: Player): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setTitle("Mission Briefing").setDescription("For the moment you work for **$name**. " +
                "To win you must imprison one of them or anyone they recruit!" +
                (if (player.role.display) "\n\n__${player.role.name}:__ ${player.role.description}" else ""))
    }
}

object VirusTeam : Team {
    override val name get() = "VIRUS"
    override val genericName get() = "agent"
    override val other get() = ServiceTeam
    override val color get() = color(200, 0, 0)
    override fun assignmentCard(player: Player): Mono<(EmbedCreateSpec) -> Unit> = Mono.just { embed ->
        embed.setTitle("Mission Briefing").setDescription("For the moment you work for **$name**. " +
                "Your mission is to stay hidden and get the agents to imprison one of their own." +
                (if (player.role.display) "\n\n__${player.role.name}:__ ${player.role.description}\n\n" else "") +
                "There is 1 other $name agent working with you" +
                (if (displayVirusMembers(player.game).isEmpty()) ", but you do not know their identity." else ". ") +
                (if (displayVirusMembers(player.game).size > 2) "Wait... there are more names on this list than there should be." else "") +
                displayVirusMembers(player.game).filter { it != player.member }.joinToString("") { "\n  - " + it.mention })
    }

    private fun displayVirusMembers(game: Game): List<Member> =
            game.players.filter { (it.team == VirusTeam || it.role == TripleAgentRole) && it.role != RougeAgentRole }.map { it.member }
}
