package net.sickmc.sickbot.modules

import dev.kord.common.entity.MessageType
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
import net.sickmc.sickbot.kord
import net.sickmc.sickbot.mainGuild
import net.sickmc.sickbot.utils.EmbedVariables
import net.sickmc.sickbot.utils.config
import net.sickmc.sickbot.utils.lobbyColor

object Lobby {
    fun register() {
        handleMemberJoin()
        handleServerBoost()
    }
    private fun handleServerBoost(){
        val allowed = listOf(MessageType.UserPremiumGuildSubscription, MessageType.UserPremiumGuildSubscriptionTierOne, MessageType.UserPremiumGuildSubscriptionThree, MessageType.UserPremiumGuildSubscriptionTwo)

        kord.on<MessageCreateEvent> {
            if (!allowed.contains(message.type))return@on
            val lobby = mainGuild.getChannel(Snowflake(config.getString("lobbyChannel"))) as? GuildMessageChannel ?: return@on
            val author = message.getAuthorAsMember()
            val descriptionBuilder: StringBuilder = StringBuilder("<a:party:959481387092676618> A very cool person just boosted the server!!!\nAnd his name is " + author!!.mention + "\n")
            when(message.type){
                MessageType.UserPremiumGuildSubscriptionTwo -> descriptionBuilder.append("The server is now tier two!")
                MessageType.UserPremiumGuildSubscriptionThree -> descriptionBuilder.append("The server is now tier three!")
                MessageType.UserPremiumGuildSubscriptionTierOne -> descriptionBuilder.append("The server is now tier one!")
                else -> {}
            }
            descriptionBuilder.append(" <a:party:959481387092676618>")
            lobby.createMessage {
                embed {
                    title = EmbedVariables.title("Booooost")
                    description = descriptionBuilder.toString()
                    footer = EmbedVariables.selfFooter()
                    timestamp = Clock.System.now()
                    color = lobbyColor
                }
            }
        }
    }

    private fun handleMemberJoin(){
        kord.on<MemberJoinEvent> {
            val lobby = mainGuild.getChannel(Snowflake(config.getString("lobbyChannel"))) as? GuildMessageChannel ?: return@on
            lobby.createMessage {
                embed {
                    title = EmbedVariables.title("Join")
                    description =
                        "<a:party:959481387092676618> ${member.mention} joined the server <a:party:959481387092676618>!"
                    footer = EmbedVariables.userFooter(member)
                    timestamp = Clock.System.now()
                    color = lobbyColor
                }
            }
        }
    }

}