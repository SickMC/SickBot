package net.sickmc.sickbot.modules

import dev.kord.common.Color
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.create.embed
import net.sickmc.sickbot.kord
import net.sickmc.sickbot.load
import net.sickmc.sickbot.mainGuild
import net.sickmc.sickbot.utils.BotColors
import net.sickmc.sickbot.utils.isAdmin


fun registerWelcomeHandlers() {
    handleWelcomeEmbeds()
    handleJoiningPlayers()
}

private val rules = listOf(
    "1. Follow Discord's rules" to """
                As a Discord-based community, we require everyone to follow Discord's [Community Guidelines](https://discord.com/guidelines) and [Terms of Service](https://discord.com/terms)
                """.trimIndent(),

    "2. Try to speak English" to """The common language in our community is English, but we understand that not everyone speaks English at the same level. 
                   Try to speak English to the best of your ability, and don't worry if you need to use translation tools.
                   We will not tolerate making fun of people for poor English.""".trimIndent(),

    "3. Listen to & respect all community team members" to """Please take requests from community team members seriously and listen when a team member asks you to do something. 
                   If you have a problem with what a team member is asking you to do, we advise that you step away from the situation and send a private message to another team member.
                   """.trimIndent(),

    "4. Keep all projects legal, legitimate & appropriate" to """We will not tolerate any projects that may break laws, breach a Terms of Service or an EULA, involve sexual content, or that the moderators determine to be malicious or inappropriate.""".trimIndent(),

    "5. Don’t spam" to """Don't spam our channels or users, and don't advertise your projects outside of the relevant showcase channels.""",

    "6. Respect our channels" to """Follow the given channel topic, and don't derail a discussion.""".trimIndent(),

    "7. Don't import drama or controversy from other communities" to """One of our most important rules - don't make things difficult for other users by bringing up controversies from other communities.""".trimIndent()
)

private suspend fun sendRuleMessages(channel: MessageChannel) = channel.createMessage {
    rules.forEach { pair ->
        embed {
            color = BotColors.Default.color
            title = pair.first
            description = pair.second
        }
    }
}

private fun handleJoiningPlayers() = kord.on<MemberJoinEvent> {
    if (guildId != mainGuild.id) return@on
    this.member.load()
}

private fun handleWelcomeEmbeds() = kord.on<MessageCreateEvent> {
    if (message.content != "!rules" && message.content != "!links") return@on
    if (message.getGuildOrNull() == null) return@on
    if (message.author!!.isBot) return@on
    if (!message.author?.asMember(mainGuild.id)?.isAdmin()!!) return@on
    when (message.content) {
        "!rules" -> sendRuleMessages(message.channel.asChannel())
        "!links" -> sendLinkEmbed(message.channel.asChannel())
    }
    message.delete("Action performed")
}

private suspend fun sendLinkEmbed(channel: MessageChannel) = channel.createEmbed {
    title = "Useful Links"
    description = """
                » [Github Organization](https://github.com/SickMC)
                » [Core(Mods, Plugins)](https://github.com/SickMC/SickCore)
                » [API](https://github.com/SickMC/SickAPI)
                » [Discord Bot](https://github.com/SickMC/SickBot)
            """.trimIndent()
    color = Color(0xFF722E)
}