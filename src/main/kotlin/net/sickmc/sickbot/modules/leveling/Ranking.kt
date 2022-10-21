package net.sickmc.sickbot.modules.leveling

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.behavior.edit
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import net.sickmc.sickbot.utils.*
import kotlin.time.Duration.Companion.minutes

private var rankingMessageId = config.getString("ranking_message").snowflake()
private var rankingChannelId = config.getString("ranking_channel").snowflake()
val rankingMessageUpdater = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
    val ranked = levelingCache
    ranked.sortBy { it.points }
    val channel = rankingChannelId.channel<TextChannel>()
    channel.getMessage(rankingMessageId).edit {
        embed {
            title = "Ranking"
            timestamp = Clock.System.now()
            userFooter(ranked[0].snowflake.member())
            color(BotColors.Level)
            description = buildString {
                ranked.take(10).forEachIndexed { index, levelUser ->
                    append("> **$index** ${levelUser.snowflake.member().mention} - ${levelUser.points} <:sickball:975024822520283156>")
                    if (index == 10) append("\n")
                }
                append("*updates every minute")
            }
        }

        actionRow {
            interactionButton(ButtonStyle.Primary, "leveling_rank") {
                label = "Your rank"
            }
            interactionButton(ButtonStyle.Primary, "level_info") {
                label = "Levels"
                emoji = DiscordPartialEmoji(id = "975024822520283156".snowflake())
            }
            interactionButton(ButtonStyle.Secondary, "level_unclaimed") {
                label = "Unclaimed Rewards"
            }
        }
    }
    delay(1.minutes)
}