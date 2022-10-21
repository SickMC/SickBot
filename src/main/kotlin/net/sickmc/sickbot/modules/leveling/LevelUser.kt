package net.sickmc.sickbot.modules.leveling

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import net.sickmc.sickapi.obtainables.Reward
import net.sickmc.sickapi.util.UUIDSerializer
import java.util.*
import kotlin.collections.ArrayList

@Serializable
data class LevelUser(
    val snowflake: Snowflake,
    var points: Int,
    val unclaimedRewards: ArrayList<Reward>,
    var mcUUID: @Serializable(with = UUIDSerializer::class) UUID?
)