package net.sickmc.sickbot.modules

import com.mongodb.client.model.Filters
import dev.kord.common.entity.*
import dev.kord.common.entity.optional.OptionalBoolean
import dev.kord.core.behavior.channel.*
import dev.kord.core.behavior.createTextChannel
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.interaction.updateEphemeralMessage
import dev.kord.core.behavior.requestMembers
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import net.sickmc.sickbot.kord
import net.sickmc.sickbot.mainGuild
import net.sickmc.sickbot.utils.*
import org.bson.Document

object Tickets {

    fun register() {
        handleTicketButtons()
        handleTicketEmbed()
        handleTicketTextInputs()
    }

    private fun handleTicketButtons() {
        val provided = listOf("ticket_open", "ticket_close", "ticket_delete", "ticket_assign", "ticket_addUser")
        kord.on<ButtonInteractionCreateEvent> {
            if (!provided.contains(interaction.component.customId)) return@on
            when (interaction.component.customId) {
                "ticket_open" -> {
                    val response = interaction.deferEphemeralResponse()
                    if (ticketColl.findOne(Filters.eq("memberID", interaction.user.id.toString())) != null) {
                        response.respond {
                            content = "You already have an opened ticket in channel ${
                                mainGuild.getChannel(
                                    Snowflake(
                                        ticketColl.findOne(Filters.eq("memberID", interaction.user.id.toString()))
                                            ?.getString("channelID") ?: error("Channel ID of ticket ${interaction.user.username} is null")
                                    )
                                ).mention
                            }!"
                        }
                        return@on
                    }
                    val ticket = Ticket(interaction.user.asMember(mainGuild.id))
                    ticket.load()
                    ticket.open()
                    response.respond {
                        content = "Your ticket was created in channel ${ticket.channel?.mention}!"
                    }
                }
                "ticket_close" -> {
                    val response = interaction.deferPublicResponse()
                    if (ticketColl.findOne(Filters.eq("channelID", interaction.channel.id.toString())) == null) {
                        response.respond {
                            content = "Your ticket cannot be found! A ${
                                mainGuild.getRole(
                                    RoleIDs.getId("Administration") ?: error("Role ID of Administration cannot be found!")
                                ).mention
                            } was contacted."
                        }
                        return@on
                    }
                    val document = ticketColl.findOne(Filters.eq("channelID", interaction.channelId.toString()))
                    val ticket = Ticket(mainGuild.getMember(Snowflake(document!!.getString("memberID"))))
                    ticket.load()
                    if (ticket.state != Ticket.TicketState.OPENED && ticket.state != Ticket.TicketState.ASSIGNED) {
                        response.respond {
                            content = "Your ticket is not opened yet! A ${
                                mainGuild.getRole(
                                    RoleIDs.getId("Administration") ?: error("Role ID of Administration cannot be found!")
                                ).mention
                            } was contacted."
                        }
                        return@on
                    }
                    ticket.close(interaction.user.asMember(mainGuild.id))
                    response.respond { content = "Ticket closed!" }
                }
                "ticket_delete" -> {
                    interaction.channel.delete("The ticket delete button was pressed")
                }
                "ticket_assign" -> {
                    val response = interaction.deferEphemeralResponse()
                    if (ticketColl.findOne(Filters.eq("channelID", interaction.channel.id.toString())) == null) {
                        response.respond {
                            content = "Your ticket cannot be found! A ${
                                mainGuild.getRole(
                                    RoleIDs.getId("Administration") ?: error("Role ID of Administration cannot be found!")
                                ).mention
                            } was contacted."
                        }
                        return@on
                    }
                    if (!interaction.user.asMember(mainGuild.id).roleIds.contains(RoleIDs.getId("Administration")) && !interaction.user.asMember(
                            mainGuild.id
                        ).roleIds.contains(RoleIDs.getId("Moderation"))
                    ) {
                        response.respond {
                            content = "This is only a **staff interaction**"
                        }
                        return@on
                    }
                    val document = ticketColl.findOne(Filters.eq("channelID", interaction.channelId.toString()))
                    val ticket = Ticket(mainGuild.getMember(Snowflake(document!!.getString("memberID"))))
                    ticket.load()
                    ticket.assign(interaction.user.asMember(mainGuild.id))
                    response.respond {
                        content = "You took over this ticket!"
                    }
                }
                "ticket_addUser" -> {
                    if (ticketColl.findOne(Filters.eq("channelID", interaction.channel.id.toString())) == null) {
                        val response = interaction.deferEphemeralResponse()
                        response.respond {
                            content = "Your ticket cannot be found! A ${
                                mainGuild.getRole(
                                    RoleIDs.getId("Administration") ?: error("Role ID of Administration cannot be found!")
                                ).mention
                            } was contacted."
                        }
                        return@on
                    }
                    if (!interaction.user.asMember(mainGuild.id).roleIds.contains(RoleIDs.getId("Administration")) && !interaction.user.asMember(
                            mainGuild.id
                        ).roleIds.contains(RoleIDs.getId("Moderation"))
                    ) {
                        val response = interaction.deferEphemeralResponse()
                        response.respond {
                            content = "This is only a **staff interaction**"
                        }
                        return@on
                    }
                    interaction.modal("Add User to Ticket", "ticket_adduser_modal") {
                        actionRow {
                            textInput(TextInputStyle.Short, "ticket_addUser_input", "Enter the Username") {
                                placeholder = "Username#1234"
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleTicketTextInputs() {
        kord.on<ModalSubmitInteractionCreateEvent> {
            if (interaction.modalId != "ticket_adduser_modal") return@on
            val input = interaction.textInputs["ticket_addUser_input"]!!.value
            val response = interaction.deferEphemeralResponse()
            try {
                val member =
                    mainGuild.members.first { it.username == input?.split("#")!![0] && it.discriminator == input.split("#")[1] }
                val ticket = Ticket(
                    mainGuild.getMember(
                        Snowflake(
                            ticketColl.findOne(
                                Filters.eq(
                                    "channelID",
                                    interaction.channel.id.toString()
                                )
                            )?.getString("memberID") ?: error("member cannot be retrieved of channel")
                        )
                    )
                )
                ticket.load()
                ticket.addUser(member)
                response.respond {
                    content = "The user was added to this ticket!"
                }
            } catch (e: NoSuchElementException) {
                response.respond { content = "This user cannot be found!" }
            }
        }
    }

    private fun handleTicketEmbed() {
        kord.on<MessageCreateEvent> {
            if (message.content != "!ticket") return@on
            if (message.author?.isBot == true) return@on
            if (!message.author?.asMember(mainGuild.id)?.roleIds?.contains(RoleIDs.getId("Administration"))!!) return@on
            sendTicketMessage(message.channel.fetchChannel())
            message.delete("Ticket message was send")
        }
    }

    private suspend fun sendTicketMessage(channel: MessageChannel) {
        channel.createMessage {
            embed {
                title = EmbedVariables.title("Ticket")
                footer = EmbedVariables.selfFooter()
                color = ticketColor
                description = "When you need help click on the :envelope_with_arrow: button below this message!"
            }
            actionRow {
                interactionButton(ButtonStyle.Primary, "ticket_open") {
                    emoji = DiscordPartialEmoji(name = "\uD83D\uDCE9")
                }
            }
        }
    }

}

class Ticket(val owner: Member) {

    var state = TicketState.UNOPENED
    var channel: TextChannel? = null
    var document: Document? = null
    var assigned: Member? = null
    var users = arrayListOf<Member>()
    var userStrings = arrayListOf<String>()

    suspend fun load() {
        document = ticketColl.findOne(Filters.eq("memberID", owner.id.toString()))
        channel = if (document == null) null
        else mainGuild.getChannel(
            Snowflake(document?.getString("channelID") ?: error("ChannelID was null in Ticket"))
        ) as TextChannel
        if (document != null) state = TicketState.valueOf(document!!.getString("state"))
        if (document != null && document?.getString("assigned") != "") assigned =
            mainGuild.getMember(Snowflake(document?.getString("assigned") ?: error("Assigned Ticket Member is null")))
        if (document != null) {
            document?.getList("extraUsers", String::class.java)?.forEach {
                users.add(mainGuild.getMember(Snowflake(it)))
                userStrings.add(it)
            }
        }
    }

    suspend fun open() {
        channel = mainGuild.createTextChannel(owner.username) {
            parentId = Snowflake(config.getString("openedTicketCategory"))
            permissionOverwrites = mutableSetOf(
                Overwrite(
                    owner.id,
                    OverwriteType.Member,
                    Permissions(Permission.ReadMessageHistory, Permission.SendMessages, Permission.ViewChannel),
                    Permissions()
                ),
                Overwrite(
                    RoleIDs.getId("Moderation") ?: error("Moderator ID for Ticket cannot be found!"),
                    OverwriteType.Role,
                    Permissions(Permission.ReadMessageHistory, Permission.SendMessages, Permission.ManageMessages, Permission.ViewChannel),
                    Permissions()
                ),
                Overwrite(
                    mainGuild.everyoneRole.id,
                    OverwriteType.Role,
                    Permissions(),
                    Permissions(Permission.ReadMessageHistory, Permission.SendMessages, Permission.ManageMessages, Permission.ViewChannel)
                ),
                getBlockOverwrite("Player"),
                getBlockOverwrite("Wither"),
                getBlockOverwrite("Dragon"),
                getBlockOverwrite("Warden"),
                getBlockOverwrite("Developer"),
                getBlockOverwrite("Builder"),
                getBlockOverwrite("Brain")
            )
        }
        val doc = Document("memberID", owner.id.toString())
            .append("channelID", channel?.id.toString())
            .append("state", TicketState.OPENED.toString())
            .append("assigned", "")
            .append("extraUsers", listOf<String>())
        ticketColl.insertOne(doc)
        document = doc
        state = TicketState.OPENED
        channel?.createMessage {
            embed {
                title = EmbedVariables.title("Ticket")
                footer = EmbedVariables.userFooter(owner)
                timestamp = Clock.System.now()
                color = ticketColor
                description = "${owner.mention}\n" +
                        "Welcome to the support!\n" +
                        "A ${mainGuild.getRole(RoleIDs.getId("Moderation") ?: error("Role ID of Moderator cannot be found")).mention} will support you soon!\n" +
                        "When your request is clarified you can easily close this ticket with the :lock: button below this message."
            }
            actionRow {
                interactionButton(ButtonStyle.Danger, "ticket_close") {
                    emoji = DiscordPartialEmoji(name = "\uD83D\uDD12", animated = OptionalBoolean.Value(false))
                }
                interactionButton(ButtonStyle.Secondary, "ticket_assign") {
                    emoji = DiscordPartialEmoji(name = "\uD83D\uDCC3", animated = OptionalBoolean.Value(false))
                }
                interactionButton(ButtonStyle.Primary, "ticket_addUser") {
                    emoji = DiscordPartialEmoji(name = "➕", animated = OptionalBoolean.Value(false))
                }
            }
        }
    }

    suspend fun assign(member: Member) {
        if (state == TicketState.CLOSED || state == TicketState.UNOPENED) return
        channel?.editRolePermission(RoleIDs.getId("Moderation") ?: error("Moderator ID for Ticket cannot be found!")) {
            denied = Permissions(Permission.SendMessages, Permission.ViewChannel)
        }
        channel?.editMemberPermission(member.id) {
            allowed = Permissions(Permission.SendMessages, Permission.ViewChannel)
        }
        assigned = member
        document?.set("assigned", member.id.toString())
        document?.set("state", TicketState.ASSIGNED.toString())
        ticketColl.replaceOne(
            Filters.eq("memberID", owner.id.toString()),
            document ?: error("Document of Ticket ${owner.username} was null")
        )
        state = TicketState.ASSIGNED
        channel?.createEmbed {
            title = EmbedVariables.title("Ticket")
            footer = EmbedVariables.userFooter(member)
            timestamp = Clock.System.now()
            color = ticketColor
            description = "${member.mention} took over this ticket!"
        }
    }

    suspend fun addUser(member: Member) {
        if (state == TicketState.CLOSED || state == TicketState.UNOPENED) return
        channel?.editMemberPermission(member.id) {
            allowed = Permissions(Permission.SendMessages, Permission.ReadMessageHistory, Permission.ViewChannel)
        }
        users.add(member)
        userStrings.add(member.id.toString())
        document?.set("extraUsers", userStrings)
        ticketColl.replaceOne(
            Filters.eq("memberID", owner.id.toString()),
            document ?: error("Document of Ticket ${owner.username} was null")
        )
        channel?.createEmbed {
            title = EmbedVariables.title("User Added")
            footer = EmbedVariables.userFooter(member)
            timestamp = Clock.System.now()
            color = ticketColor
            description = "The user ${member.mention} was added to this ticket!"
        }
    }

    suspend fun close(closer: Member) {
        if (state != TicketState.OPENED && state != TicketState.ASSIGNED) return
        channel?.editMemberPermission(owner.id) {
            denied = Permissions(Permission.ReadMessageHistory, Permission.SendMessages, Permission.ViewChannel)
        }
        channel?.editRolePermission(
            RoleIDs.getId("Moderation") ?: error("Moderator ID for Ticket ${owner.username} cannot be found")
        ) {
            denied = Permissions(Permission.ReadMessageHistory, Permission.SendMessages, Permission.ViewChannel)
        }
        users.forEach {
            channel?.editMemberPermission(it.id) {
                denied = Permissions(Permission.ReadMessageHistory, Permission.SendMessages, Permission.ViewChannel)
            }
        }
        channel?.edit {
            parentId = Snowflake(config.getString("closedTicketCategory"))
        }
        ticketColl.deleteOne(Filters.eq("memberID", owner.id.toString()))
        state = TicketState.CLOSED
        channel?.createMessage {
            embed {
                title = EmbedVariables.title("Ticket Closed")
                footer = EmbedVariables.userFooter(closer)
                timestamp = Clock.System.now()
                color = ticketColor
                description = "The user ${closer.mention} closed this ticket!"
            }
            actionRow {
                interactionButton(ButtonStyle.Danger, "ticket_delete") {
                    emoji = DiscordPartialEmoji(name = "\uD83D\uDDD1️", animated = OptionalBoolean.Value(false))
                }
            }
        }
    }

    enum class TicketState {
        UNOPENED,
        OPENED,
        ASSIGNED,
        CLOSED
    }

    private fun getBlockOverwrite(name: String): Overwrite{
        return Overwrite(RoleIDs.getId(name) ?: error("Builder ID for Ticket cannot be found!"), OverwriteType.Role, Permissions(), Permissions(Permission.ReadMessageHistory, Permission.SendMessages, Permission.ManageMessages, Permission.ViewChannel))
    }

}