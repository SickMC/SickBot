package net.sickmc.sickbot.modules.leveling.rewards

import net.sickmc.sickapi.SickPlayer
import net.sickmc.sickapi.obtainables.Advancement
import net.sickmc.sickapi.obtainables.PlayerAdvancement
import net.sickmc.sickapi.obtainables.Reward
import net.sickmc.sickapi.util.ColorHolder
import net.sickmc.sickapi.util.players
import org.litote.kmongo.eq

class AdvancementReward(private val advancement: Advancement, private val level: Int) : Reward() {
    override val color: ColorHolder = advancement.color
    override val name: String = "${advancement.name} advancement"

    override suspend fun applyTo(player: SickPlayer) {
        var playerAdvancement = player.advancements.find { it.advancement.uuid == advancement.uuid }
        if (playerAdvancement == null) {
            playerAdvancement = PlayerAdvancement(advancement, level)
            player.advancements += playerAdvancement
        } else {
            player.advancements -= (playerAdvancement)
            playerAdvancement.currentLevel = level
            player.advancements += playerAdvancement
        }

        players.replaceOne(SickPlayer::uuidString eq player.uuidString, player)
        sendRewardSuccessMessage(
            player,
            "You received the advancement ${advancement.name} with level $level as a discord leveling reward!",
            color
        )
    }
}