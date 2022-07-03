package net.sickmc.sickbot.modules

import dev.kord.core.behavior.createChatInputCommand
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.int
import dev.kord.rest.builder.interaction.user
import kotlinx.coroutines.flow.toList
import net.sickmc.sickbot.kord
import net.sickmc.sickbot.mainGuild
import net.sickmc.sickbot.utils.RoleIDs

object Moderation {

    suspend fun register() {
        ClearCommand.register()
    }

}

object ClearCommand {
    suspend fun register() {
        mainGuild.createChatInputCommand("clear", "Clears multiple messages in chat") {
            int("clear_amount", "The amount of messages") {
                required = true
            }
            user("clear_user", "The author of which messages should be deleted") {
                required = false
            }
        }

        kord.on<GuildChatInputCommandInteractionCreateEvent> {
            if (interaction.invokedCommandName != "clear") return@on
            val response = interaction.deferEphemeralResponse()
            if (!interaction.user.roleIds.contains(RoleIDs.getId("Moderation")) && !interaction.user.roleIds.contains(
                    RoleIDs.getId("Administration")
                )
            ) {
                response.respond {
                    content = "This is a staff command!"
                }
                return@on
            }
            val command = interaction.command
            val channel = interaction.getChannel()
            val amount = command.integers["clear_amount"]!!
            val user = command.users["clear_user"]
            if (channel.lastMessageId == null) {
                response.respond {
                    content = "There are no messages to delete!"
                }
                return@on
            }
            if (user == null) {
                channel.getMessagesBefore(channel.lastMessageId!!, amount.toInt()).toList().forEach {
                    it.delete("Clear command was used by ${interaction.user.mention}!")
                }
                response.respond {
                    content = "$amount messages where deleted!"
                }
                return@on
            }
            channel.getMessagesBefore(channel.lastMessageId!!, amount.toInt()).toList().forEach {
                if (it.author != user) return@forEach
                it.delete("Clear command was used by ${interaction.user.mention}!")
            }
            response.respond {
                content = "$amount messages of user ${user.mention} where deleted!"
            }
        }
    }

}