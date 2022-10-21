package net.sickmc.sickbot.modules.ticket

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.event.interaction.GuildSelectMenuInteractionCreateEvent
import dev.kord.core.event.interaction.GuildUserCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.SelectOptionBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.actionRow
import net.sickmc.sickbot.kord
import net.sickmc.sickbot.mainGuild
import net.sickmc.sickbot.utils.*
import org.litote.kmongo.eq

fun registerTicketHandlers() {
    ticketMessageListener
    ticketButtonListener
    ticketUserAddListener
    ticketSelectAddUserListener
}

private val ticketMessageListener = kord.on<MessageCreateEvent> {
    if (message.content != "!ticket") return@on
    if (message.getGuildOrNull() == null) return@on
    if (message.author!!.isBot) return@on
    if (!message.author?.asMember(mainGuild.id)?.isAdmin()!!) return@on
    this.message.channel.createMessage {
        embed {
            title = "Ticket"
            description =
                "If you need support just open a ticket by clicking on the :envelope_with_arrow: button. \nA supporter will help you as fast as he can."
            color(BotColors.Ticket)
            selfFooter()
        }
        actionRow {
            interactionButton(ButtonStyle.Secondary, "ticket_create") {
                label = "Open Ticket"
                emoji = DiscordPartialEmoji(name = "\uD83D\uDCE9")
            }
        }
    }
    this.message.delete()
}

private val ticketButtonListener = kord.on<GuildButtonInteractionCreateEvent> {
    when (interaction.component.customId) {
        "ticket_create" -> {
            val response = interaction.deferEphemeralResponse()
            val previousTicket = tickets.findOne(Ticket::opener eq interaction.user.id)
            if (previousTicket != null) {
                response.respond {
                    content = "You already have opened a ticket in <#${previousTicket.channel}>!"
                }
                return@on
            }
            val ticket = openTicket(interaction.user)
            response.respond {
                content = "Your ticket is opened in <#${ticket.channel}>"
            }
        }

        "ticket_assign" -> {
            val response = interaction.deferEphemeralResponse()
            val ticket = tickets.findOne(Ticket::channel eq interaction.channelId) ?: return@on
            if (!interaction.user.roleIds.contains(
                    config.getString("moderator_id").snowflake()
                ) && !interaction.user.isAdmin()
            ) {
                response.respond {
                    content = "This command is for staff only!"
                }
                return@on
            }
            if (ticket.assigned == interaction.user.id) {
                response.respond {
                    content = "You are already assigned to this ticket!"
                }
                return@on
            }

            ticket.assign(interaction.user)
            response.respond {
                content = "You took over the ticket!"
            }
        }

        "ticket_close" -> {
            val response = interaction.deferEphemeralResponse()
            val ticket = tickets.findOne(Ticket::channel eq interaction.channelId) ?: return@on

            ticket.close(interaction.user)
            response.respond {
                content = "Ticket closed!"
            }
        }

        "ticket_delete" -> {
            interaction.channel.delete()
        }
    }
}

private val ticketUserAddListener = kord.on<GuildUserCommandInteractionCreateEvent> {
    if (interaction.invokedCommandName != "add to ticket") return@on
    val response = interaction.deferEphemeralResponse()
    response.respond {
        content = "Click the ticket where the user should be added to"
        actionRow {
            selectMenu("ticket_adduser/${interaction.targetId}") {
                options.addAll(tickets.find().toList()
                    .map { SelectOptionBuilder(it.opener.member().username, it.opener.toString()) })
            }
        }
    }
}

private val ticketSelectAddUserListener = kord.on<GuildSelectMenuInteractionCreateEvent> {
    if (!interaction.component.customId.startsWith("ticket_adduser")) return@on
    val response = interaction.deferEphemeralResponse()
    val userToAdd = interaction.component.customId.split("/")[1].snowflake().member()
    val ticketId = interaction.values[0].snowflake()
    val ticket = tickets.findOne(Ticket::opener eq ticketId) ?: error("Ticket was not opened as expected!")
    if (ticket.additionalUsers.contains(userToAdd.id)) {
        response.respond {
            content = "The user was already added to the ticket!"
        }
        return@on
    }
    ticket.addUser(userToAdd)
    response.respond {
        content = "You added the user ${userToAdd.mention} to the ticket of ${ticketId.member().mention}"
    }
}