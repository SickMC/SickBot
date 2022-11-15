package net.sickmc.sickbot.modules.leveling.rewards

import net.sickmc.sickapi.SickPlayer
import net.sickmc.sickapi.obtainables.Reward
import net.sickmc.sickapi.util.ColorHolder
import net.sickmc.sickapi.util.StaticColor
import net.sickmc.sickapi.util.players
import org.litote.kmongo.eq

class SmucksReward(private val smucks: Int) : Reward() {
    override val name: String = "$smucks smucks"
    override val color: ColorHolder = StaticColor(0xFFC675)

    override suspend fun applyTo(player: SickPlayer) {
        player.smucks = player.smucks + smucks

        sendRewardSuccessMessage(
            player,
            "You received $smucks smucks as a discord leveling reward!",
            color
        )
        players.replaceOne(SickPlayer::uuidString eq player.uuidString, player)
    }
}