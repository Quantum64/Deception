package co.q64.deception

import co.q64.deception.theme.*
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.channel.TextChannel

open class Player(val game: Game, val member: Member) {
    open val mention: String get() = member.mention
    var startingTeam: Team = game.theme.player
    var team: Team = game.theme.player
    var channel: TextChannel? = null
    var role: Role = NoRole
    var operation: Operation = NoOperation
    var votes = mutableListOf<Player>()
    var receivedAssignment = false
    var receivedOperation = false
    var voteCast = false
}

class SyntheticPlayer(game: Game, member: Member) : Player(game, member) {
    override val mention: String get() = "The Current Player"
}