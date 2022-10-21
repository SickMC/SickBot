package net.sickmc.sickbot.utils

import dev.kord.core.entity.Member

fun Member.isAdmin() = this.roleIds.contains(config.getString("admin_id").snowflake())