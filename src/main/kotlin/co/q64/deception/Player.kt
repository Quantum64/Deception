package co.q64.deception

import co.q64.deception.theme.*
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.channel.TextChannel

class Player(val game: Game, val member: Member) {
    var startingTeam: Team = game.theme.player
    var team: Team = game.theme.player
    var channel: TextChannel? = null
    var role: Role = NoRole
    var operation: Operation = NoOperation
    var votes = 0
    var receivedAssignment = false
    var receivedOperation = false
    var voteCast = false
}