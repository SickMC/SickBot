package me.anton.sickbot.modules

import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.create.actionRow
import io.ktor.network.sockets.*
import kotlinx.coroutines.flow.*
import me.anton.sickbot.SickBot

class Rules {

    init {
        handleInteraction()
        handleRuleCreate()
    }

    private suspend fun sendRuleMessage(channel: MessageChannel) {
        channel.createEmbed {
            this.color = Color(237, 142, 26)
            this.title = "**Discord Rules | SickMC**"
            this.description = """
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

        }

        channel.createMessage {
            this.content = "**Click here to get access to the server!**"
            this.actionRow {
                interactionButton(ButtonStyle.Success, "rule_accept"){
                    emoji = DiscordPartialEmoji(name = "U+2705")
                }
            }
        }
    }

    private fun handleInteraction(){
        SickBot.instance.kord.on<ButtonInteractionCreateEvent> {
            if (interaction.componentId != "rule_accept") return@on
            if (!SickBot.instance.getMainGuild().getMember(interaction.user.id).roleIds.contains(SickBot.instance.getIdOfRankGroup("Player")))return@on
            SickBot.instance.getMainGuild().getMember(interaction.user.id).addRole(SickBot.instance.getIdOfRankGroup("Player"), "Accepted the rules!")
        }
    }

    private fun handleRuleCreate(){
        SickBot.instance.kord.on<MessageCreateEvent> {
            println("asdasdasd")
            println("Content: ${message.content}")
            if (message.content != "!rules")return@on
            print("rule")
            if (message.author!!.isBot)return@on
            print("bot")
            if (!message.author!!.asMember(SickBot.instance.getMainGuild().id).roleIds.contains(SickBot.instance.getIdOfRankGroup("Administration")))return@on
            print("amin")
            sendRuleMessage(message.channel.fetchChannel())
            message.delete("Rule message was send")
        }
    }

}