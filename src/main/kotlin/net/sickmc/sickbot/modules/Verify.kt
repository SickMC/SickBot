package net.sickmc.sickbot.modules

import com.mongodb.client.model.Filters
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.TextInputStyle
import dev.kord.common.entity.optional.OptionalBoolean
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.builder.components.emoji
import dev.kord.core.entity.Member
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.event.interaction.GuildModalSubmitInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.x.emoji.Emojis
import dev.kord.x.emoji.from
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import net.sickmc.sickbot.kord
import net.sickmc.sickbot.mainGuild
import net.sickmc.sickbot.utils.*
import java.util.*

object Verify {

    private val verificationCache = hashMapOf<Int, UUID>()

    fun register() {
        handleVerificationMessage()
        handleVerifyButtons()
        messageReceiver()
    }

    private fun messageReceiver() {
        databaseScope.launch {
            listenChannel("verify") {
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val components = frame.readText().split("/")
                    if (components[0] != "request") continue
                    val uuid = UUID.fromString(components[1])
                    val code = components[2].toInt()
                    verificationCache[code] = uuid
                }
            }
        }
    }

    private fun handleVerificationMessage() {
        kord.on<MessageCreateEvent> {
            if (guildId == null) return@on
            if (message.content != "!verify") return@on
            if (message.author!!.isBot) return@on
            if (!message.author!!.asMember(mainGuild.id).roleIds.contains(RoleIDs.getId("Administration"))) return@on

            message.channel.createMessage {
                embed {
                    title = "**Verification**"
                    color = verifyColor
                    description = """
                        **Steps for your Minecraft-Discord verification**
                        > 1. Join play.sickmc.net in Minecraft
                        > 2. Type /verify in the minecraft chat and copy the received code
                        > 3. Click on the <a:checkmark:959175780770918411> button below and insert the code
                        > 4. You're verified! <a:party:959481387092676618>
                        If you need help feel free to create a <#908093243177197618> or get support in <#908092816545157170>!
                    """.trimIndent()
                    footer = EmbedVariables.selfFooter()
                }

                actionRow {
                    interactionButton(ButtonStyle.Success, "verify") {
                        emoji = DiscordPartialEmoji(
                            Snowflake(959175780770918411), "checkmark", animated = OptionalBoolean.Value(true)
                        )
                    }
                    interactionButton(ButtonStyle.Danger, "verification_unlink") {
                        emoji(ReactionEmoji.from(Emojis.wastebasket))
                        label = "Unlink"
                    }
                }
            }
            message.delete("Message sent!")
        }
    }

    private fun handleVerifyButtons() {
        kord.on<GuildButtonInteractionCreateEvent> {
            when (interaction.component.customId) {
                "verify" -> {
                    if (interaction.user.asMember().isVerified()) {
                        interaction.respondEphemeral {
                            content = "You're already verified!"
                        }
                        return@on
                    }
                    interaction.modal("Verification", "verification_code_modal") {
                        actionRow {
                            textInput(TextInputStyle.Short, "verification_code_insert_field", "Insert code") {
                                allowedLength = 6..6
                                placeholder = "e.g. 123456"
                                required = true
                            }
                        }
                    }
                }
                "verification_unlink" -> {
                    val response = interaction.deferEphemeralResponse()
                    val member = interaction.user.asMember()
                    if (!member.isVerified()) {
                        response.respond {
                            content = "You're not verified!"
                        }
                        return@on
                    }
                    databaseScope.launch {
                        val doc = playerColl.findOne(Filters.eq("discordID", member.id.toString()))!!
                        doc.replace("discordID", "0")
                        playerColl.replaceOne(Filters.eq("discordID", member.id.toString()), doc)
                    }
                    response.respond {
                        content = "Your minecraft account is now unlinked!"
                    }
                }
            }
        }
        kord.on<GuildModalSubmitInteractionCreateEvent> {
            if (interaction.modalId != "verification_code_modal") return@on
            val input = interaction.textInputs["verification_code_insert_field"]!!.value!!
            val code = input.toIntOrNull()
            if (code == null || !verificationCache.containsKey(code)) {
                interaction.respondEphemeral {
                    content = "Your code is invalid!"
                }
                return@on
            }
            val member = interaction.user.asMember()
            databaseScope.launch {
                val doc = playerColl.findOne(Filters.eq("uuid", verificationCache[code].toString()))!!
                doc.replace("discordID", member.id.toString())
                playerColl.replaceOne(Filters.eq("uuid", verificationCache[code].toString()), doc)
                sendChannel(
                    "verify",
                    Frame.Text("success/${verificationCache[code]}/${member.username}#${member.discriminator}")
                )
                interaction.respondEphemeral {
                    content = "Your account is now linked with **${doc.getString("name")}**!"
                }
                verificationCache.remove(code)
            }
        }
    }

}

suspend fun Member.isVerified(): Boolean {
    return playerColl.findOne(Filters.eq("discordID", this.id.toString())) != null
}