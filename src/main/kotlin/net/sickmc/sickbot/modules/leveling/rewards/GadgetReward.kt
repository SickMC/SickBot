package net.sickmc.sickbot.modules.leveling.rewards

import net.sickmc.sickapi.SickPlayer
import net.sickmc.sickapi.obtainables.Gadget
import net.sickmc.sickapi.obtainables.PlayerGadget
import net.sickmc.sickapi.obtainables.Reward
import net.sickmc.sickapi.util.ColorHolder
import net.sickmc.sickapi.util.players
import org.litote.kmongo.eq

class GadgetReward(private val gadget: Gadget) : Reward() {
    override val color: ColorHolder = gadget.color
    override val name: String = "${gadget.name} gadget"

    override suspend fun applyTo(player: SickPlayer) {
        player.gadgets += PlayerGadget(gadget, 1)

        sendRewardSuccessMessage(
            player,
            "You received the gadget ${gadget.name} as a discord leveling reward!",
            color
        )
        players.replaceOne(SickPlayer::uuidString eq player.uuidString, player)
    }
}