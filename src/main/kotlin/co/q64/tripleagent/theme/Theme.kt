package co.q64.tripleagent.theme

import co.q64.tripleagent.Game

interface Theme {
    val channelPrefix: String get() = "game-"
    val minPlayers: Int
    val maxPlayers: Int

    val player: Team
    val traitor: Team
    fun traitorCount(players: Int): Int
    fun roleCount(players: Int): Int
    fun generateRoles(): List<Role>
    fun generateOperations(game: Game): List<Operation>

    val introTitle: String
    val introDescription: String

    val roleAssignmentStartTitle: String
    fun roleAssignmentStartDescription(mention: String): String
    val roleAssignmentMessageWaitTitle: String
    fun roleAssignmentMessageWaitDescription(mention: String): String
    val roleAssignmentDiscussTitle: String
    fun roleAssignmentDiscussDescription(mention: String): String
    val operationActionWaitTitle: String
    fun operationActionWaitDescription(mention: String): String
    val operationDiscussTitle: String
    fun operationDiscussDescription(mention: String): String

    val operationsTitle: String
    val operationsDescription: String
}