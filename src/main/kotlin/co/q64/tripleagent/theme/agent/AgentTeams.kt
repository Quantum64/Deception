package co.q64.tripleagent.theme.agent

import co.q64.tripleagent.Game
import co.q64.tripleagent.Player
import co.q64.tripleagent.theme.Team
import net.dv8tion.jda.api.entities.Member

object ServiceTeam : Team {
    override val name get() = "The Service"
    override val genericName get() = "agent"
    override val other get() = VirusTeam
    override fun assignmentCard(player: Player) = "For the moment you work for **$name**. " +
            "To win you must imprison one of them or anyone they recruit!" +
            (if (player.role.display) "\n\n__${player.role.name}:__ ${player.role.description}" else "")
}

object VirusTeam : Team {
    override val name get() = "VIRUS"
    override val genericName get() = "agent"
    override val other get() = ServiceTeam
    override fun assignmentCard(player: Player) = "For the moment you work for **$name**. " +
            "Your mission is to stay hidden and get the agents to imprison one of their own." +
            (if (player.role.display) "\n\n__${player.role.name}:__ ${player.role.description}" else "") +
            "There is 1 other $name agent working with you. " +
            (if (displayVirusMembers(player.game).size > 2) "Wait... there are more names on this list than there should be." else "") +
            displayVirusMembers(player.game).filter { it != player.member }.joinToString("") { "\n  - " + it.asMention }
}

private fun displayVirusMembers(game: Game): List<Member> =
        game.players.filter { (it.team == VirusTeam || it.role == TripleAgentRole) && it.role != RougeAgentRole }.map { it.member }