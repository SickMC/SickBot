package net.sickmc.sickbot.modules.leveling.rewards

import net.sickmc.sickapi.SickPlayer
import net.sickmc.sickapi.obtainables.Reward
import net.sickmc.sickapi.rank.parent
import net.sickmc.sickapi.rank.rankCache
import net.sickmc.sickapi.util.ColorHolder
import net.sickmc.sickapi.util.Colors
import net.sickmc.sickapi.util.StaticColor
import net.sickmc.sickapi.util.players
import org.litote.kmongo.eq
import kotlin.time.Duration

class RankReward(private val rankName: String, private val expiration: Duration?) : Reward() {
    override val name: String = "$rankName rank for ${expiration ?: "permanent"}"
    override val color: ColorHolder = StaticColor(0xFF497C)

    override suspend fun applyTo(player: SickPlayer) {
        val rank = rankCache.get(rankName) ?: error("rank of RankReward with name $rankName not found!")
        if (player.permanentRank.parent.priority < rank.parent.priority || player.currentRank.parent.priority < rank.parent.priority) {
            sendRewardSuccessMessage(
                player,
                "You did not receive the rank reward as you already have a higher rank on this network",
                StaticColor(Colors.red)
            )
            return
        }

        if (player.currentRank.name == rankName && player.permanentRank.name != rankName && player.rankExpire != null) {
            if (expiration == null) {
                player.rankExpire = null
                player.permanentRank = rank
                players.replaceOne(SickPlayer::uuidString eq player.uuidString, player)
                sendRewardSuccessMessage(
                    player, "Your rank $rankName is now permanent as a discord leveling reward.", color
                )
                return
            }
            player.rankExpire = player.rankExpire!! + expiration.inWholeMilliseconds
            players.replaceOne(SickPlayer::uuidString eq player.uuidString, player)
            sendRewardSuccessMessage(
                player,
                "You received a extension of your current rank by $expiration. The rank will expire in ${player.rankExpire}",
                color
            )
            return
        }

        player.rankExpire = System.currentTimeMillis() + expiration!!.inWholeMilliseconds
        player.currentRank = rank
        players.replaceOne(SickPlayer::uuidString eq player.uuidString, player)
        sendRewardSuccessMessage(
            player, "You received the rank $rank for $expiration as an discord leveling reward!", color
        )
    }
}