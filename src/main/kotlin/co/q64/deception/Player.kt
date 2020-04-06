package co.q64.deception

import co.q64.deception.theme.*
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel

class Player(val game: Game, val member: Member) {
    var startingTeam: Team = game.theme.player
    var team: Team = game.theme.player
    var channel: TextChannel? = null
    var role: Role = NoRole
    var operation: Operation = NoOperation
    var receivedAssignment = false
    var receivedOperation = false
}