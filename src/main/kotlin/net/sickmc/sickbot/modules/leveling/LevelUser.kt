package net.sickmc.sickbot.modules.leveling

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import net.sickmc.sickapi.util.UUIDSerializer
import java.util.*

@Serializable
data class LevelUser(
    val snowflake: Snowflake,
    val points: Int,
    val unclaimedRewards: List<LevelingReward>,
    val mcUUID: @Serializable(with = UUIDSerializer::class) UUID?
)