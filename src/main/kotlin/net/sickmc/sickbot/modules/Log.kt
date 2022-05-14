package net.sickmc.sickbot.modules

import dev.kord.common.entity.AuditLogEvent
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.getAuditLogEntries
import dev.kord.core.entity.AuditLogEntry
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.guild.*
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.core.on
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import net.sickmc.sickbot.*
import net.sickmc.sickbot.utils.EmbedVariables
import net.sickmc.sickbot.utils.config
import net.sickmc.sickbot.utils.logColor
import kotlin.time.Duration.Companion.seconds

object Log {

    suspend fun register(){
        logEvents()
    }

    suspend fun logEvents(){
        val logChannel = staffGuild.getChannel(Snowflake(config.getString("staff_logChannel"))).asChannel() as TextChannel

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
                color = logColor
                footer = EmbedVariables.userFooter(user)
                timestamp = Clock.System.now()
                description = "The member **${user.mention}** was banned by **${banner?.mention}**\n**Reason:**\n ```${getBan().reason}```"
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
                color = logColor
                footer = EmbedVariables.userFooter(user)
                timestamp = Clock.System.now()
                description = "The member **${user.mention}** was unbanned by **${unbanner?.mention}**"
            }
        }

        kord.on<InviteCreateEvent> {
            logChannel.createEmbed {
                title = EmbedVariables.title("Invite Create")
                color = logColor
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
                color = logColor
                timestamp = Clock.System.now()
                description = "The invite **$code** was deleted"
            }
        }

        kord.on<MemberJoinEvent> {
            logChannel.createEmbed {
                title = EmbedVariables.title("Member Join")
                color = logColor
                footer = EmbedVariables.userFooter(member)
                timestamp = Clock.System.now()
                description = "The user **${member.mention}** joined the server"
            }
        }
        kord.on<MemberLeaveEvent> {
            logChannel.createEmbed {
                title = EmbedVariables.title("Member Leave")
                color = logColor
                footer = EmbedVariables.userFooter(user)
                timestamp = Clock.System.now()
                description = "The user **${user.mention}** left the server"
            }
        }

        kord.on<MessageDeleteEvent> {
            if (guildId == null)return@on
            if (message == null)return@on
            if (message?.author!!.isBot)return@on
            delay(2.seconds)

            logChannel.createEmbed {
                val deleter = mainGuild.getMember(checkAuditLog(AuditLogEvent.MessageDelete)[0].userId)
                title = EmbedVariables.title("Message Delete")
                color = logColor
                footer = EmbedVariables.userFooter(deleter)
                timestamp = Clock.System.now()
                description = "A message of **${message?.author?.mention}** was deleted by **${deleter.mention}**\n**Content:** ```${message?.content}```"
            }
        }
        kord.on<MessageUpdateEvent> {
            logChannel.createEmbed {
                title = EmbedVariables.title("Message Update")
                color = logColor
                if (old?.author != null) footer = EmbedVariables.userFooter(old?.author!!) else EmbedVariables.selfFooter()
                timestamp = Clock.System.now()
                description = "The message of **${old?.author?.mention}** changed\n**Old:** ```${old?.content}```\n\n**New:** \n```${new.content.value}```"
            }
        }
    }
    private suspend fun checkAuditLog(logAction: AuditLogEvent): List<AuditLogEntry>{
        val list = arrayListOf<AuditLogEntry>()
        mainGuild.getAuditLogEntries(builder = {
            action = logAction
            limit = 1
            before = Snowflake(5.seconds.inWholeMilliseconds)
        }).collect(){
            list.add(it)
        }
        return list
    }

}