package net.sickmc.sickbot.modules.leveling

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.modify.embed
import kotlinx.datetime.Clock
import net.sickmc.sickbot.kord
import net.sickmc.sickbot.utils.BotColors
import net.sickmc.sickbot.utils.color
import net.sickmc.sickbot.utils.userFooter

val rankingButtonHandler = kord.on<GuildButtonInteractionCreateEvent> {
    when (interaction.component.customId) {
        "leveling_rank" -> {
            val response = interaction.deferEphemeralResponse()
            response.respond {
                embed {
                    val user = levelingCache.find { it.snowflake == interaction.user.id } ?: error("cannot find user in leveling cache")
                    val level = user.points.level()
                    val next = level.next()
                    val percentToNext =
                        ((user.points.toDouble() / next.start.toDouble()) * 100).toInt()
                    val progress = buildString {
                        val progressOnBar = percentToNext / 5
                        var bar = 20
                        repeat(progressOnBar) {
                            append("| ")
                            bar--
                        }
                        repeat(bar) {
                            append("  ")
                        }
                    }

                    title = "Rank"
                    description = """
                        > **Points:** ${user.points}
                        > **Rank:** ${levelingCache.sortedByDescending { it.points }.indexOf(user) + 1}
                        > **Level:** <:${level.emoji.name}:${level.emoji.id}> ${level.name}
                        > **Next level:** <:${next.emoji.name}:${next.emoji.id}> ${next.name}
                        > **Progress:** `$progress` $percentToNext%
                    """.trimIndent()

                    timestamp = Clock.System.now()
                    userFooter(interaction.user)
                    color(BotColors.Level)
                }
            }
        }
    }
}