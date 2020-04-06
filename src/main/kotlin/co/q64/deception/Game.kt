package co.q64.deception

import co.q64.deception.state.GameState
import co.q64.deception.state.State
import co.q64.deception.state.WaitingState
import co.q64.deception.theme.NoOperation
import co.q64.deception.theme.agent.AgentTheme
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageReaction


class Game(private val guild: Guild) {
    val players: MutableList<Player> = mutableListOf()
    val members: List<Member> get() = players.map { it.member }
    var state: State = WaitingState
    var theme = AgentTheme
    var selected: Player? = null

    fun start(message: Message) {
        if (state !is WaitingState) {
            return message.reply("Cannot start game. Game is already running!")
        }
        for (player in members) {
            // TODO Check channel name
            if (player.voiceState == null || player.voiceState?.channel == null) {
                return message.reply("Cannot start game. All players must first join voice chat.")
            }
            if (player.voiceState?.isGuildMuted == true) {
                player.mute(false).queue()
            }
        }
        // TODO Check min players
        deleteChannels()
        for (player in players) {
            guild.createTextChannel(theme.channelPrefix + player.member.idLong).complete().let { channel ->
                val everyone = channel.createPermissionOverride(guild.publicRole)
                val user = channel.createPermissionOverride(player.member)
                player.channel = channel
                everyone.deny(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ADD_REACTION).queue()
                user.grant(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).queue()
            }
        }
        for (count in 1..theme.traitorCount(players.size)) {
            players.shuffled().firstOrNull { it.team == theme.player }?.let {
                it.team = theme.traitor
                it.startingTeam = theme.traitor
            }
        }
        val roles = theme.generateRoles()
        for (count in 1..theme.roleCount(players.size)) {
            players.shuffle()
            roles.shuffled().firstOrNull { role -> role !in players.map { it.role } }?.assign(this)
        }
        val operations = theme.generateOperations(this)
        for (player in players) {
            player.operation = operations.shuffled()
                    .firstOrNull { operation -> operation !in players.map { it.operation } && operation.canAssign(player) }
                    ?: NoOperation
        }
        players.shuffle()
        enter(GameState.STARTING)
    }

    fun end(message: Message) {
        if (state is WaitingState) {
            return message.reply("You can not end the game because the game has not started!")
        }
        players.clear()
        state = WaitingState
        deleteChannels()
        unmute()
        return message.reply("Game ended!")
    }

    fun join(message: Message) {
        if (state !is WaitingState) {
            return message.reply("You can not join the game since it has already started!")
        }
        if (inGame(message.member)) {
            return message.reply("You can not join a game that you have already joined!")
        }
        players.add(Player(this, message.member!!))
        return message.reply("Joined game! (${players.size}/${theme.maxPlayers} players)")
    }

    fun leave(message: Message) {
        if (state !is WaitingState) {
            return message.reply("You can not leave a game that has already started!")
        }
        if (!inGame(message.member)) {
            return message.reply("You cannot leave a game that you have not joined!")
        }
        players.removeIf { it.member == message.member }
        return message.reply("Left game! (${players.size}/${theme.maxPlayers} players)")
    }

    fun tick() = state.tick()
    fun handleReaction(member: Member, reaction: MessageReaction) =
            state.handleReaction(member, reaction)

    fun enter(new: GameState) {
        state.exit()
        state = new.generator(this)
        state.enter()
    }

    fun mute() = players.map { it.member }.forEach { member ->
        member.mute(true).queue()
    }

    fun unmute() = players.map { it.member }.forEach { member ->
        member.mute(false).queue()
    }

    fun inGame(member: Member?): Boolean =
            players.map { it.member }.any { it == member }

    fun deleteChannels() {
        for (channel in guild.channels) {
            if (channel.name.startsWith(theme.channelPrefix)) {
                channel.delete().complete()
            }
        }
    }
}
