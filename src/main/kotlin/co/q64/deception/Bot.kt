package co.q64.deception

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val commands = mapOf<String, (Game, Message) -> Unit>(
        "join" to { g, m -> g.join(m) },
        "leave" to { g, m -> g.leave(m) },
        "start" to { g, m -> g.start(m) },
        "end" to { g, m -> g.end(m) }
)

class Bot(private val token: String) : ListenerAdapter() {
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val games: MutableMap<Guild, Game> = mutableMapOf()
    private val client: JDA = JDABuilder.create(token,
            GatewayIntent.GUILD_PRESENCES,
            GatewayIntent.GUILD_VOICE_STATES,
            GatewayIntent.GUILD_EMOJIS,
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.GUILD_MESSAGE_REACTIONS).build()

    init {
        client.addEventListener(this)
        scheduler.scheduleAtFixedRate({
            games.values.forEach { kotlin.runCatching { it.tick() }.onFailure { it.printStackTrace() } }
        }, 1, 1, TimeUnit.SECONDS)
    }

    private fun game(guild: Guild): Game =
            games.getOrPut(guild, { Game(guild) })

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.message.contentRaw.startsWith(commandPrefix)) {
            commands[event.message.contentRaw.removePrefix(commandPrefix).toLowerCase()]?.let { action ->
                action(game(event.guild), event.message)
            }
        }
    }

    override fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) {
        game(event.guild).handleReaction(event.member, event.reaction)
    }
}