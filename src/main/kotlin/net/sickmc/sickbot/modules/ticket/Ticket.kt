package net.sickmc.sickbot.modules.ticket

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

@Serializable
data class Ticket(
    val opener: Snowflake,
    val channel: Snowflake,
    var assigned: Snowflake?,
    val additionalUsers: ArrayList<Snowflake>
)