package co.q64.tripleagent.theme.agent

import co.q64.tripleagent.Game
import co.q64.tripleagent.theme.NoRole
import co.q64.tripleagent.theme.Role

object RougeAgentRole : Role {
    override val name get() = "Rouge Agent"
    override val description get() = "You are a Rouge Agent. The other VIRUS agents do not know that you are a double agent."

    override fun assign(game: Game) {
        game.players.firstOrNull { it.role == NoRole && it.team == ServiceTeam }?.let {
            it.role = this
            it.team = VirusTeam
        }

    }
}

object TripleAgentRole : Role {
    override val name get() = "Triple Agent"
    override val description get() = "You are a Triple Agent. The VIRUS double agents think you are on their side but you are actually working for The Service."

    override fun assign(game: Game) {
        game.players.firstOrNull { it.role == NoRole && it.team == ServiceTeam }?.let { it.role = this }
    }
}

object DeepCoverAgentRole : Role {
    override val name get() = "Deep Cover Agent"
    override val description get() = "You are operating under deep cover. Anytime someone tries to check your status they will see you as a Service agent."

    override fun assign(game: Game) {
        game.players.firstOrNull { it.role == NoRole && it.team == VirusTeam }?.let { it.role = this }
    }
}

object SuspiciousAgentRole : Role {
    override val name get() = "Suspicious Agent"
    override val description get() = "Your past includes some ties to suspicious figures. Anytime someone tries to check your status they will see you as a VIRUS agent."

    override fun assign(game: Game) {
        game.players.firstOrNull { it.role == NoRole && it.team == ServiceTeam }?.let { it.role = this }
    }
}

object ServiceLoyalistRole : Role {
    override val name get() = "Service Loyalist"
    override val description get() = "You are a die-hard loyalist. Any operation that attempts to change your team from a Service agent will be canceled."

    override fun assign(game: Game) {
        game.players.firstOrNull { it.role == NoRole && it.team == ServiceTeam }?.let { it.role = this }
    }
}


object VirusLoyalistRole : Role {
    override val name get() = "VIRUS Loyalist"
    override val description get() = "You are a die-hard loyalist. Any operation that attempts to change your team from a VIRUS agent will be canceled."

    override fun assign(game: Game) {
        game.players.firstOrNull { it.role == NoRole && it.team == VirusTeam }?.let { it.role = this }
    }
}
