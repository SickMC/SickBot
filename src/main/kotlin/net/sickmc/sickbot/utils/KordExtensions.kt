package net.sickmc.sickbot.utils

import dev.kord.common.entity.Snowflake

fun String.toSnowflake(): Snowflake {
    return Snowflake(this)
}