package net.sickmc.sickbot.utils

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.channel.GuildChannel
import net.sickmc.sickbot.mainGuild
import net.sickmc.sickbot.staffGuild

fun String.snowflake(): Snowflake = Snowflake(this)

suspend fun Snowflake.member(guild: SickGuild = SickGuild.Main) = when (guild) {
    SickGuild.Main -> mainGuild.getMember(this)
    SickGuild.Staff -> staffGuild.getMember(this)
}

suspend inline fun <reified T : GuildChannel> Snowflake.channel(guild: SickGuild = SickGuild.Main) = when (guild) {
    SickGuild.Main -> mainGuild.getChannelOf<T>(this)
    SickGuild.Staff -> staffGuild.getChannelOf<T>(this)
}

suspend fun Snowflake.role(guild: SickGuild = SickGuild.Main) = when (guild) {
    SickGuild.Main -> mainGuild.getRole(this)
    SickGuild.Staff -> staffGuild.getRole(this)
}

enum class SickGuild {
    Main, Staff
}