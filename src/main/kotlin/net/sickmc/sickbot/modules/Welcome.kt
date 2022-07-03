package net.sickmc.sickbot.modules

import dev.kord.common.Color
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import net.sickmc.sickbot.kord
import net.sickmc.sickbot.mainGuild
import net.sickmc.sickbot.utils.RoleIDs

object Welcome {

    fun register() {
        handleWelcomeEmbeds()
        handleJoiningPlayers()
    }

    private val rules = listOf(
        Pair(
            0xff0000, Pair(
                "1. Follow Discord's rules", """
                As a Discord-based community, we require everyone to follow Discord's [Community Guidelines](https://discord.com/guidelines) and [Terms of Service](https://discord.com/terms)
                """.trimIndent()
            )
        ), Pair(
            0xff8c00, Pair(
                "2. Try to speak English",
                """The common language in our community is English, but we understand that not everyone speaks English at the same level. 
                   Try to speak English to the best of your ability, and don't worry if you need to use translation tools.
                   We will not tolerate making fun of people for poor English.""".trimIndent()
            )
        ), Pair(
            0xe1ff00, Pair(
                "3. Listen to & respect all community team members",
                """Please take requests from community team members seriously and listen when a team member asks you to do something. 
                   If you have a problem with what a team member is asking you to do, we advise that you step away from the situation and send a private message to another team member.
                   """.trimIndent()
            )
        ), Pair(
            0x55ff00, Pair(
                "4. Keep all projects legal, legitimate & appropriate",
                """We will not tolerate any projects that may break laws, breach a Terms of Service or an EULA, involve sexual content, or that the moderators determine to be malicious or inappropriate.""".trimIndent()
            )
        ), Pair(
            0x00ff37, Pair(
                "5. Don’t spam",
                """Don't spam our channels or users, and don't advertise your projects outside of the relevant showcase channels."""
            )
        ), Pair(
            0x00ffc8, Pair(
                "6. Respect our channels",
                """Follow the given channel topic, and don't derail a discussion.""".trimIndent()
            )
        ), Pair(
            0x00aaff, Pair(
                "7. Don't import drama or controversy from other communities",
                """One of our most important rules - don't make things difficult for other users by bringing up controversies from other communities.""".trimIndent()
            )
        )
    )

    private suspend fun sendRuleMessages(channel: MessageChannel) = rules.forEach { pair ->
        channel.createEmbed {
            color = Color(pair.first)
            title = pair.second.first
            description = pair.second.second
        }
    }

    private fun handleJoiningPlayers() {
        kord.on<MemberJoinEvent> {
            if (guildId != mainGuild.id) return@on
            member.addRole(RoleIDs.getId("Player") ?: error("Player id cannot be found!"))
        }
    }

    private fun handleWelcomeEmbeds() {
        kord.on<MessageCreateEvent> {
            if (message.content != "!rules" && message.content != "!links") return@on
            if (message.getGuildOrNull() == null) return@on
            if (message.author?.isBot == true) return@on
            if (!message.author?.asMember(mainGuild.id)?.roleIds?.contains(RoleIDs.getId("Administration"))!!) return@on
            when (message.content) {
                "!rules" -> sendRuleMessages(message.channel.asChannel())
                "!links" -> sendLinkEmbed(message.channel.asChannel())
            }
            message.delete("Action performed")
        }
    }

    private suspend fun sendLinkEmbed(channel: MessageChannel) {
        channel.createEmbed {
            title = "Useful Links"
            description = """
                » [Github Organization](https://github.com/SickMC)
                » [Core(Mods, Plugins, Backend)](https://github.com/SickMC/SickCore)
                » [Discord Bot](https://github.com/SickMC/SickBot)
                » [SickMod](https://modrinth.com/mod/sickmod)
            """.trimIndent()
            color = Color(0x3053FF)
        }
    }

}