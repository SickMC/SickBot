package net.sickmc.sickbot.modules.leveling

import dev.kord.core.entity.Member

interface LevelingReward {
    val name: String
    val description: String

    suspend fun award(member: Member)
}