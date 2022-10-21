package net.sickmc.sickbot.utils

import dev.kord.common.Color
import dev.kord.core.entity.User
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.builder.message.EmbedBuilder
import net.sickmc.sickbot.kord

suspend fun EmbedBuilder.selfFooter() = this.footer {
    icon = kord.getSelf(EntitySupplyStrategy.rest).avatar?.url
    text = "SickMC - Requested by SickMC"
}

fun EmbedBuilder.userFooter(user: User) = this.footer {
    icon = user.avatar?.url
    text = "Requested by ${user.username}#${user.discriminator}"
}

fun EmbedBuilder.color(color: BotColors) {
    this.color = color.color
}

enum class BotColors(val color: Color) {
    Level(Color(47, 131, 11)),
    Ticket(Color(57, 167, 227)),
    Verify(Color(58, 161, 32)),
    Log(Color(181, 181, 181)),
    Default(Color(255, 182, 76))
}