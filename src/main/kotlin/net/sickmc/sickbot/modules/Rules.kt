package net.sickmc.sickbot.modules

import dev.kord.common.entity.*
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.create.actionRow
import net.sickmc.sickbot.kord
import net.sickmc.sickbot.mainGuild
import net.sickmc.sickbot.utils.EmbedVariables
import net.sickmc.sickbot.utils.RoleIDs

object Rules {

    fun register(){
        handleInteraction()
        handleRuleCreate()
    }
    private suspend fun sendRuleMessage(channel: MessageChannel) {
        channel.createEmbed {
            color = EmbedVariables.color()
            title = EmbedVariables.title("Discord Rules")
            description = """
                **Verhalten**
                Textchats:
                - Bitte nur auf Deutsch oder Englisch schreiben!
                - Spam ist zu unterlassen
                - Keine übermäßige Benutzung von Caps oder Emotes
                
                Sprachchats:
                - Störgeräusche sind zu unterlassen
                - Sprachverzehrer sind nicht erlaubt
                - Channel-hopping (ständiges Wechseln der Kanäle) ist zu unterlassen
                - Das Aufnehmen von anderen ohne deren Erlaubnis ist nicht erlaubt!
                
                Allgemein:
                - Beleidigungen sind sowohl in Sprachchats und Textchats verboten, sollte euch jemand in einem Sprachchannel beleidigen meldet dies einem der Mods
                - Rassismus oder Antisemitismus führen zu einem sofortigen Ban ohne Chance wieder entbannt zu werden!
                
                Datenschutz:
                - Die Weitergabe von persönlichen Daten wie z.B Telefonnummer, Email-Adresse oder ähnlichem ist untersagt
                
                **Bestrafungen**
                - der mehrfache Verstoß gegen die Serverregeln führt zu einem permanenten Bann
                - Falls du der Meinung bist, dass du zu Unrecht gebannt oder gekickt wurdest melde dich bei einem der Moderatoren per DM (DirectMessage)
                
                Nickname
                - Nicknames dürfen keine beleidigenden oder andere verbotenen oder geschützte Namen oder Namensteile enthalten.
                - Der Nickname darf nicht aus Sonderzeichen bestehen.
                """.trimIndent()

            footer = EmbedVariables.selfFooter()
        }

        channel.createMessage {
            this.content = "**Click here to get access to the server!**"
            this.actionRow {
                interactionButton(ButtonStyle.Success, "rule_accept") {
                    emoji = DiscordPartialEmoji(Snowflake(959175780770918411), "checkmark")
                }
            }
        }
    }

    private fun handleInteraction() {
        kord.on<ButtonInteractionCreateEvent> {
            if (interaction.componentId != "rule_accept") return@on
            val response = interaction.deferEphemeralResponse()
            if (mainGuild.getMember(interaction.user.id).roleIds.contains(RoleIDs.getId("Player"))) {
                response.respond {
                    content = "**Already accepted the rules!**\nCannot accept the rules twice!"
                }
                return@on
            }
            mainGuild.getMember(interaction.user.id)
                .addRole(RoleIDs.getId("Player")?: error("Player role not found"), "Accepted the rules!")
            response.respond { content = "**Accepted the rules!**\nHave fun!" }
        }
    }
    private fun handleRuleCreate() {
        kord.on<MessageCreateEvent> {
            if (message.content != "!rules") return@on
            if (message.getGuildOrNull() == null)return@on
            if (message.author?.isBot == true) return@on
            if (!message.author?.asMember(mainGuild.id)?.roleIds?.contains(RoleIDs.getId("Administration"))!!) return@on
            sendRuleMessage(message.channel.asChannel())
            message.delete("Rule message was send")
        }
    }
}