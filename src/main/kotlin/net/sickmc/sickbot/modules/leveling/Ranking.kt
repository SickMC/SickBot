package net.sickmc.sickbot.modules.leveling

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.GuildMessageCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import net.sickmc.sickbot.kord
import net.sickmc.sickbot.mainGuild
import net.sickmc.sickbot.utils.*
import kotlin.time.Duration.Companion.minutes

private var rankingMessageId = config.getString("ranking_message").snowflake()
private var rankingChannelId = config.getString("ranking_channel").snowflake()
val rankingMessageUpdater = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
    while (true) {
        val channel = rankingChannelId.channel<TextChannel>()
        channel.getMessage(rankingMessageId).edit {
            embed { generateRanking() }
            actionRow { generateRankingRow() }
        }
        delay(1.minutes)
    }
}

val rankingMessageCreateHandler = kord.on<MessageCreateEvent> {
    if (message.content != "!ranking") return@on
    if (message.getGuildOrNull() == null) return@on
    if (message.author!!.isBot) return@on
    if (!message.author?.asMember(mainGuild.id)?.isAdmin()!!) return@on
    message.channel.createMessage {
        embed { generateRanking() }
        actionRow { generateRankingRow() }
    }
    message.delete("Action performed")
}

val rankingMessageUpdateHandler = kord.on<GuildMessageCommandInteractionCreateEvent> {
    if (interaction.invokedCommandName != "update rank message") return@on
    if (!interaction.user.isAdmin()) return@on
    val response = interaction.deferEphemeralResponse()
    val message = interaction.channel.getMessageOrNull(rankingMessageId) ?: return@on
    message.edit {
        embed { generateRanking() }
        actionRow { generateRankingRow() }
    }

    response.respond {
        content = "updated"
    }
}

private suspend fun EmbedBuilder.generateRanking() {
    val ranked = levelingCache.sortedByDescending { it.points }
    title = "Ranking"
    timestamp = Clock.System.now()
    userFooter(ranked[0].snowflake.member())
    color(BotColors.Level)
    description = buildString {
        ranked.take(10).forEachIndexed { index, levelUser ->
            append("> **${index + 1}.**  ${levelUser.snowflake.member().mention} - ${levelUser.points} <:sickball:975024822520283156>")
            append("\n")
        }
        append("*updates every minute*")
    }
}

private fun ActionRowBuilder.generateRankingRow() {
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