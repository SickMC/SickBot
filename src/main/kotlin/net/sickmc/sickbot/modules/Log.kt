package net.sickmc.sickbot.modules

import dev.kord.common.entity.AuditLogEvent
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.getAuditLogEntries
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.guild.*
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.core.on
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.sickmc.sickbot.kord
import net.sickmc.sickbot.kordScope
import net.sickmc.sickbot.mainGuild
import net.sickmc.sickbot.secondGuild
import net.sickmc.sickbot.utils.EmbedVariables
import net.sickmc.sickbot.utils.config

object Log {

    suspend fun register(){
        logEvents()
    }

    suspend fun logEvents(){
        val logChannel = secondGuild.getChannel(Snowflake(config.getString("staff_logChannel"))).asChannel() as TextChannel

        println("Bitetetetetetet")
        kord.on<BanAddEvent> {
            var banner: User? = null
            guild.getAuditLogEntries(builder = {
                action = AuditLogEvent.MemberBanAdd
                limit = 1
            }).collect(){
                banner = mainGuild.getMember(it.userId).asUser()
            }

            logChannel.createEmbed {
                title = EmbedVariables.title("Ban Add")
                color = EmbedVariables.color()
                footer = EmbedVariables.userFooter(user)
                timestamp = Clock.System.now()
                description = "The member **${user.mention}** was banned by **${banner?.mention}**\nReason\n ```${getBan().reason}```"
            }
        }
        kord.on<BanRemoveEvent> {
            var unbanner: User? = null
            guild.getAuditLogEntries(builder = {
                action = AuditLogEvent.MemberBanRemove
                limit = 1
            }).collect(){
                unbanner = mainGuild.getMember(it.userId).asUser()
            }

            logChannel.createEmbed {
                title = EmbedVariables.title("Ban Remove")
                color = EmbedVariables.color()
                footer = EmbedVariables.userFooter(user)
                timestamp = Clock.System.now()
                description = "The member **${user.mention}** was unbanned by **${unbanner?.mention}**"
            }
        }

        kord.on<InviteCreateEvent> {
            print("send?")
            logChannel.createEmbed {
                title = EmbedVariables.title("Invite Create")
                color = EmbedVariables.color()
                footer = EmbedVariables.userFooter(inviter?.asUser() ?: return@on)
                timestamp = Clock.System.now()
                description = "The user **${inviter?.mention}** created a invite"
                field {
                    name = "Channel"
                    value = channel.mention
                    inline = true
                }
                field {
                    name = "Code"
                    value = code
                    inline = true
                }
                field {
                    name = "Duration"
                    value = maxAge.toString()
                    inline = true
                }
                field {
                    name = "Max Uses"
                    value = maxUses.toString()
                    inline = true
                }
                field {
                    name = "Target User"
                    value = if (targetUser == null) "Undefined" else "${targetUser?.mention}"
                    inline = true
                }
            }
        }
        kord.on<InviteDeleteEvent> {
            logChannel.createEmbed {
                title = EmbedVariables.title("Invite Delete")
                color = EmbedVariables.color()
                timestamp = Clock.System.now()
                description = "The invite $code was deleted"
            }
        }

        kord.on<MemberJoinEvent> {
            logChannel.createEmbed {
                title = EmbedVariables.title("Member Join")
                color = EmbedVariables.color()
                footer = EmbedVariables.userFooter(member)
                timestamp = Clock.System.now()
                description = "The user **${member.mention}** joined the server"
            }
        }
        kord.on<MemberLeaveEvent> {
            logChannel.createEmbed {
                title = EmbedVariables.title("Member Leave")
                color = EmbedVariables.color()
                footer = EmbedVariables.userFooter(user)
                timestamp = Clock.System.now()
                description = "The user **${user.mention}** left the server"
            }
        }

        kord.on<MessageDeleteEvent> {
            if (guildId == null)return@on
            if (message?.author!!.isBot)return@on
            var deleter: User? = null
            guild?.asGuild()?.getAuditLogEntries(builder = {
                action = AuditLogEvent.MessageDelete
                limit = 1
            })?.collect(){
                deleter = mainGuild.getMember(it.userId).asUser()
            }

            logChannel.createEmbed {
                title = EmbedVariables.title("Message Delete")
                color = EmbedVariables.color()
                footer = EmbedVariables.userFooter(deleter!!)
                timestamp = Clock.System.now()
                description = "A message of **${message?.author?.mention}** was deleted by **${deleter?.mention}**\nContent: ```${message?.content}```"
            }
        }
        kord.on<MessageUpdateEvent> {
            logChannel.createEmbed {
                title = EmbedVariables.title("Message Update")
                color = EmbedVariables.color()
                footer = EmbedVariables.userFooter(old?.author!!)
                timestamp = Clock.System.now()
                description = "The message of **${old?.author?.mention}** changed\nOld: ```${old?.content}```\n\nNew: \n```${new.content}```"
            }
        }
    }

}