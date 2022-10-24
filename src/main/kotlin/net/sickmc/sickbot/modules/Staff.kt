package net.sickmc.sickbot.modules

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.create.actionRow
import net.sickmc.sickbot.kord
import net.sickmc.sickbot.mainGuild
import net.sickmc.sickbot.staffGuild
import net.sickmc.sickbot.utils.config
import net.sickmc.sickbot.utils.snowflake

suspend fun registerStaffModule() {
    enableAnnouncements()
}

private suspend fun enableAnnouncements() {
    val announcementID = config.getString("staff_announcement_create").snowflake()
    val announcementChannel =
        staffGuild.getChannelOf<TextChannel>(announcementID)
    val newsChannel = mainGuild.getChannelOf<TextChannel>(config.getString("announcementChannel").snowflake())
    kord.on<MessageCreateEvent> {
        if (message.channelId != announcementID) return@on
        if (message.author == null) return@on
        if (message.author!!.isBot) return@on
        announcementChannel.createMessage {
            content = message.content
            actionRow {
                interactionButton(ButtonStyle.Success, "announcement_send") {
                    emoji = DiscordPartialEmoji(name = "\uD83D\uDE80")
                }
                interactionButton(ButtonStyle.Danger, "announcement_send_ping") {
                    emoji = DiscordPartialEmoji(name = "\uD83D\uDD14")
                    label = "Send with Ping"
                }
            }
        }
    }
    kord.on<ButtonInteractionCreateEvent> {
        if (interaction.componentId != "announcement_send" && interaction.componentId != "announcement_send_ping") return@on
        if (interaction.user.isBot) return@on
        val response = interaction.deferPublicResponse()
        when (interaction.componentId) {
            "announcement_send" -> {
                newsChannel.createMessage {
                    content = interaction.message.content
                }
                response.respond {
                    content = "Announcement send! MessageID = ${interaction.message.id}"
                }
            }

            "announcement_send_ping" -> {
                newsChannel.createMessage {
                    content = interaction.message.content + "\n" + staffGuild.getEveryoneRole().mention
                }
                response.respond {
                    content = "Announcement send! MessageID = ${interaction.message.id}"
                }
            }
        }
    }
}