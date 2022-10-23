package net.sickmc.sickbot.modules.leveling

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sickmc.sickapi.SickPlayer
import net.sickmc.sickapi.obtainables.Reward
import net.sickmc.sickapi.util.players
import net.sickmc.sickbot.kord
import net.sickmc.sickbot.utils.BotColors
import net.sickmc.sickbot.utils.color
import net.sickmc.sickbot.utils.config
import net.sickmc.sickbot.utils.userFooter
import org.litote.kmongo.eq

val rankingButtonHandler = kord.on<GuildButtonInteractionCreateEvent> {
    when (interaction.component.customId) {
        "leveling_rank" -> {
            val response = interaction.deferEphemeralResponse()
            response.respond {
                embed {
                    val user = levelingCache.find { it.snowflake == interaction.user.id }
                        ?: error("cannot find user in leveling cache")
                    val level = user.points.level()
                    val next = level.next()
                    val percentToNext = ((user.points.toDouble() / (next?.start?.toDouble() ?: 0.0)) * 100).toInt()
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
                        > **Points:** ${user.points} <:sickball:975024822520283156>
                        > **Rank:** ${levelingCache.sortedByDescending { it.points }.indexOf(user) + 1}
                        > **Level:** <:${level.emoji.name}:${level.emoji.id}> ${level.name}
                        ${if (next != null) "> **Next level:** <:${next.emoji.name}:${next.emoji.id}> ${next.name}" else ""}
                        > **Progress:** `$progress` $percentToNext%
                    """.trimIndent()

                    timestamp = Clock.System.now()
                    userFooter(interaction.user)
                    color(BotColors.Level)
                }
            }
        }

        "level_info" -> {
            val response = interaction.deferEphemeralResponse()
            response.respond {
                embed {
                    title = "Levels"
                    description = buildString {
                        Level.values().forEach { level ->
                            append(
                                "> <:${level.emoji.name}:${level.emoji.id}> **${level.name}**  " + "${level.start} <:sickball:975024822520283156> - ${level.end ?: "∞"} <:sickball:975024822520283156> \n"
                            )
                        }
                    }
                    color(BotColors.Level)
                    userFooter(interaction.user)
                }
            }
        }

        "level_unclaimed" -> {
            val response = interaction.deferEphemeralResponse()
            val levelUser =
                levelingCache.find { it.snowflake == interaction.user.id } ?: error("cannot find user in levelingCache")
            if (levelUser.mcUUID == null) {
                response.respond {
                    content =
                        "To claim rewards you have to link your Minecraft account with Discord." + "\nYou have to follow the instructions in <#${
                            config.getString("verify_channel")
                        }>"
                }
                return@on
            }

            if (levelUser.unclaimedRewards.isEmpty()) {
                response.respond {
                    content = "You don't have any rewards to claim!"
                }
                return@on
            }

            response.respond {
                content = "Click the reward you want to claim"
                actionRow {
                    selectMenu("levelreward_claim") {
                        levelUser.unclaimedRewards.forEach {
                            option(it.name, Json.encodeToString(it))
                        }

                        allowedValues = 1..levelUser.unclaimedRewards.size
                    }
                }
            }
        }
    }
}

val rewardSelectMenuHandler = kord.on<SelectMenuInteractionCreateEvent> {
    if (interaction.component.customId != "levelreward_claim") return@on
    val rewards = interaction.values.map { Json.decodeFromString<Reward>(it) }
    val response = interaction.deferEphemeralResponse()
    response.respond {
        content = buildString {
            append("The following rewards has been redeemed:\n")
            rewards.forEach {
                it.applyTo(
                    players.findOne(SickPlayer::discordID eq interaction.user.id.toString())
                        ?: error("cannot find player with discordID but he is verified häää")
                )
                append("- ${it.name}\n")
            }

            append("\nEnjoy!")
        }
    }
}