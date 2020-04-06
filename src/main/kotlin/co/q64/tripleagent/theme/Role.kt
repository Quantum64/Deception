package co.q64.tripleagent.theme

import co.q64.tripleagent.Game

interface Role {
    val name: String
    val description: String
    val display: Boolean get() = true
    fun assign(game: Game) {
        game.players.firstOrNull { it.role == NoRole }?.let {
            it.role = this
        }
    }
}

object NoRole : Role {
    override val name get() = "No Role"
    override val description get() = "This role is assigned automatically when no other role is"
    override val display get() = false
    override fun assign(game: Game) = Unit
}