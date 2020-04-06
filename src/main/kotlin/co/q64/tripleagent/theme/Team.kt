package co.q64.tripleagent.theme

import co.q64.tripleagent.Player

interface Team {
    val name: String
    val genericName: String
    val other: Team
    fun assignmentCard(player: Player): String
}