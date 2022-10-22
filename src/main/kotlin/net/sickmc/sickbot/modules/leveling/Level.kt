package net.sickmc.sickbot.modules.leveling

import dev.kord.common.entity.DiscordPartialEmoji
import net.sickmc.sickapi.obtainables.*
import net.sickmc.sickbot.modules.leveling.rewards.AdvancementReward
import net.sickmc.sickbot.modules.leveling.rewards.GadgetReward
import net.sickmc.sickbot.modules.leveling.rewards.RankReward
import net.sickmc.sickbot.modules.leveling.rewards.SmucksReward
import net.sickmc.sickbot.utils.snowflake
import kotlin.time.Duration.Companion.days

enum class Level(val emoji: DiscordPartialEmoji, val start: Int, val end: Int?, val reward: Reward?) {
    Wood(
        DiscordPartialEmoji(id = "975034271880343552".snowflake(), name = "sickwood"),
        0,
        499,
        null
    ),
    Stone(
        DiscordPartialEmoji(id = "975035457425510410".snowflake(), name = "sickstone"),
        500,
        1499,
        AdvancementReward(Advancement.stoned, 1)
    ),
    Coal(
        DiscordPartialEmoji(name = "sickcoal", id = "975036476247117834".snowflake()), 1500, 2999, SmucksReward(1000)
    ),
    Icon(
        DiscordPartialEmoji(id = "975037086329618502".snowflake(), name = "sickiron"),
        3000,
        4999,
        RankReward("Wither", 5.days)
    ),
    Gold(
        DiscordPartialEmoji(id = "975037608692432927".snowflake(), name = "sickgold"),
        5000,
        7249,
        GadgetReward(Gadget.gold)
    ),
    Redstone(
        DiscordPartialEmoji(id = "975038278115917875".snowflake(), name = "sickredstone"),
        7250,
        9499,
        SmucksReward(5000)
    ),
    Lapis(
        DiscordPartialEmoji(id = "975038899531431936".snowflake(), name = "sicklapis"),
        9500,
        11999,
        RankReward("Dragon", 5.days)
    ),
    Emerald(
        DiscordPartialEmoji(id = "975039429066522634".snowflake(), name = "sickemerald"),
        12000,
        14999,
        GadgetReward(Gadget.emerald)
    ),
    Diamond(
        DiscordPartialEmoji(id = "975039813407354951".snowflake(), name = "sickdiamond"),
        15000,
        19999,
        SmucksReward(10000)
    ),
    Netherite(
        DiscordPartialEmoji(id = "975040225829064754".snowflake(), name = "sicknetherite"),
        20000,
        null,
        RankReward("Wither", null)
    )
}

fun Int.level() = Level.values().find { it.start <= this && (it.end == null || it.end >= this) }
    ?: error("User's rank cannot be determined!")

fun Level.next(): Level? {
    val levels = Level.values()
    return levels.getOrNull(levels.indexOf(this) + 1)
}