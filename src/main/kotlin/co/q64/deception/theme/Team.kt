package co.q64.deception.theme

import co.q64.deception.Player

interface Team {
    val name: String
    val genericName: String
    val other: Team
    fun assignmentCard(player: Player): String
}