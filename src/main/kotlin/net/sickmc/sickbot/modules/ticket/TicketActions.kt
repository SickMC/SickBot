package net.sickmc.sickbot.modules.ticket

import dev.kord.common.entity.*
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.edit
import dev.kord.core.behavior.channel.editMemberPermission
import dev.kord.core.behavior.createTextChannel
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import net.sickmc.sickbot.mainGuild
import net.sickmc.sickbot.utils.*
import org.litote.kmongo.eq

private val openTicketCategory = config.getString("opened_tickets").snowflake()
private val closedTicketCategory = config.getString("closed_tickets").snowflake()
suspend fun openTicket(user: Member): Ticket {
    val channel = mainGuild.createTextChannel(user.username) {
        parentId = openTicketCategory
        permissionOverwrites = mutableSetOf(
            Overwrite(
                mainGuild.everyoneRole.id, OverwriteType.Role, Permissions(), Permissions(Permission.ViewChannel)
            ), Overwrite(
                config.getString("moderator_id").snowflake(),
                OverwriteType.Role,
                Permissions(Permission.ReadMessageHistory, Permission.ViewChannel, Permission.SendMessages),
                Permissions()
            ), Overwrite(
                user.id,
                OverwriteType.Member,
                Permissions(Permission.ReadMessageHistory, Permission.ViewChannel, Permission.SendMessages),
                Permissions()
            )
        )
    }

    channel.createMessage {
        val moderatorMention = config.getString("moderator_id").snowflake().role().mention
        content = moderatorMention
        embed {
            title = "Welcome to the ticket support!"
            description =
                "A $moderatorMention will support you soon. \nPlease understand that we have also other tickets to handle.\n\n_You can close the ticket with the :lock: button below. \nThe other button is only for supporters._"
            color(BotColors.Ticket)
            userFooter(user)
        }

        actionRow {
            interactionButton(ButtonStyle.Danger, "ticket_close") {
                emoji = DiscordPartialEmoji(name = "\uD83D\uDD12")
            }
            interactionButton(ButtonStyle.Primary, "ticket_assign") {
                emoji = DiscordPartialEmoji(name = "\uD83D\uDCC3")
            }
        }
    }

    val ticket = Ticket(user.id, channel.id, null, arrayListOf())
    tickets.insertOne(ticket)
    return ticket
}

suspend fun Ticket.assign(user: Member) {
    this.assigned = user.id
    this.channel.channel<TextChannel>().createMessage {
        embed {
            title = "Ticket assigned"
            description = "The ticket was assigned to ${user.mention}"
            color(BotColors.Ticket)
            userFooter(user)
        }
    }
    tickets.replaceOne(Ticket::opener eq this.opener, this)
}

suspend fun Ticket.addUser(user: Member) {
    this.additionalUsers += user.id
    val channel = this.channel.channel<TextChannel>()
    channel.editMemberPermission(user.id) {
        allowed = Permissions(Permission.ReadMessageHistory, Permission.ViewChannel, Permission.SendMessages)
    }
    channel.createMessage {
        embed {
            title = "User added"
            description = "The user ${user.mention} was added to this ticket!"
            color(BotColors.Ticket)
            userFooter(user)
        }
    }
    tickets.replaceOne(Ticket::opener eq this.opener, this)
}

suspend fun Ticket.close(closer: Member) {
    val channel = this.channel.channel<TextChannel>()
    channel.createMessage {
        content = "This ticket was closed by ${closer.mention}"
        actionRow {
            interactionButton(ButtonStyle.Danger, "ticket_delete") {
                emoji = DiscordPartialEmoji(name = "\uD83D\uDDD1️")
            }
        }
    }

    channel.edit {
        parentId = closedTicketCategory
        permissionOverwrites = mutableSetOf(
            Overwrite(
                mainGuild.everyoneRole.id, OverwriteType.Role, Permissions(), Permissions(Permission.ViewChannel)
            ), Overwrite(
                config.getString("moderator_id").snowflake(),
                OverwriteType.Role,
                Permissions(),
                Permissions(Permission.ViewChannel)
            ), Overwrite(
                this@close.opener, OverwriteType.Member, Permissions(), Permissions(Permission.ViewChannel)
            )
        )
    }
    tickets.deleteOne(Ticket::opener eq this.opener)
}