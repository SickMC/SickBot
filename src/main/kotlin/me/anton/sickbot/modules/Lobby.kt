package me.anton.sickbot.modules

import dev.kord.common.entity.MessageType
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
import me.anton.sickbot.SickBot
import me.anton.sickbot.utils.EmbedVariables

class Lobby {

    private val kord = SickBot.instance.kord
    private val main = SickBot.instance

    init {
        handleMemberJoin()
        handleServerBoost()
    }

    private fun handleServerBoost(){
        val allowed = listOf(MessageType.UserPremiumGuildSubscription, MessageType.UserPremiumGuildSubscriptionTierOne, MessageType.UserPremiumGuildSubscriptionThree, MessageType.UserPremiumGuildSubscriptionTwo)

        kord.on<MessageCreateEvent> {
            if (!allowed.contains(message.type))return@on
            val lobby = main.getMainGuild().getChannel(Snowflake(main.getConfigValue("lobbyChannel"))) as? GuildMessageChannel ?: return@on
            val author = message.getAuthorAsMember()
            val descriptionBuilder: StringBuilder = StringBuilder("<a:party:959481387092676618> A very cool person just boosted the server!!!\nAnd his name is " + author!!.mention + "\n")
            when(message.type){
                MessageType.UserPremiumGuildSubscriptionTwo -> descriptionBuilder.append("The server is now tier two!")
                MessageType.UserPremiumGuildSubscriptionThree -> descriptionBuilder.append("The server is now tier three!")
                MessageType.UserPremiumGuildSubscriptionTierOne -> descriptionBuilder.append("The server is now tier one!")
            }
            descriptionBuilder.append(" <a:party:959481387092676618>")
            lobby.createMessage {
                embed {
                    title = "**Booooost | SickMC**"
                    description = descriptionBuilder.toString()
                    footer = EmbedVariables.selfFooter()
                    timestamp = Clock.System.now()
                    color = EmbedVariables.color()
                }
            }
        }
    }

    private fun handleMemberJoin(){
        kord.on<MemberJoinEvent> {
            val lobby =
                main.getMainGuild().getChannel(Snowflake(main.getConfigValue("lobbyChannel"))) as? GuildMessageChannel
                    ?: return@on
            lobby.createMessage {
                embed {
                    title = "**Join | SickMC**"
                    description =
                        "<a:party:959481387092676618> ${member.mention} joined the server <a:party:959481387092676618>!"
                    footer = EmbedVariables.userFooter(member)
                    timestamp = Clock.System.now()
                    color = EmbedVariables.color()
                }
            }
        }
    }

}