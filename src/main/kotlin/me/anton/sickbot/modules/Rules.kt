package me.anton.sickbot.modules

import dev.kord.common.Color
import dev.kord.common.entity.*
import dev.kord.common.entity.optional.OptionalBoolean
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.Image
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.actionRow
import io.ktor.network.sockets.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.JsonNull.content
import me.anton.sickbot.SickBot
import me.anton.sickbot.utils.EmbedVariables

class Rules {

    init {
        handleInteraction()
        handleRuleCreate()
    }

    private suspend fun sendRuleMessage(channel: MessageChannel) {
        channel.createEmbed {
            color = EmbedVariables.color()
            title = "**Discord Rules | SickMC**"
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
                interactionButton(ButtonStyle.Success, "rule_accept"){
                    emoji = DiscordPartialEmoji(Snowflake(959175780770918411), "checkmark")
                }
            }
        }
    }

    private fun handleInteraction(){
        SickBot.instance.kord.on<ButtonInteractionCreateEvent> {
            val response = interaction.deferEphemeralResponse()
            if (interaction.componentId != "rule_accept") return@on
            if (SickBot.instance.getMainGuild().getMember(interaction.user.id).roleIds.contains(SickBot.instance.getIdOfRankGroup("Player"))){
                response.respond {
                    content = "**Already accepted the rules!**\nCannot accept the rules twice!"
                }
                return@on
            }
            SickBot.instance.getMainGuild().getMember(interaction.user.id).addRole(SickBot.instance.getIdOfRankGroup("Player"), "Accepted the rules!")
            response.respond { content = "**Accepted the rules!**\nHave fun!" }
        }
    }

    private fun handleRuleCreate(){
        SickBot.instance.kord.on<MessageCreateEvent> {
            if (message.content != "!rules")return@on
            if (message.author!!.isBot)return@on
            if (!message.author!!.asMember(SickBot.instance.getMainGuild().id).roleIds.contains(SickBot.instance.getIdOfRankGroup("Administration")))return@on
            sendRuleMessage(message.channel.fetchChannel())
            message.delete("Rule message was send")
        }
    }

}