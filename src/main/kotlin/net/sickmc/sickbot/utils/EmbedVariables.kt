package net.sickmc.sickbot.utils

import dev.kord.common.Color
import dev.kord.core.entity.User
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.builder.message.EmbedBuilder
import net.sickmc.sickbot.kord

val levelColor = Color(47, 131, 11)
val ticketColor = Color(57, 167, 227)
val verifyColor = Color(58, 161, 32)
val logColor = Color(181, 181, 181)

object EmbedVariables {

    fun title(title: String): String {
        return "**$title | SickMC**"
    }

    suspend fun selfFooter(): EmbedBuilder.Footer {
        val footerbuilder = EmbedBuilder.Footer()
        footerbuilder.icon = kord.getSelf(EntitySupplyStrategy.rest).avatar?.url
        footerbuilder.text = "SickMC • Requested by SickMC"
        return footerbuilder
    }

    fun userFooter(user: User): EmbedBuilder.Footer {
        val footerbuilder = EmbedBuilder.Footer()
        footerbuilder.icon = user.avatar?.url
        footerbuilder.text = "SickMC • Requested by ${user.username}#${user.discriminator}"
        return footerbuilder
    }

    fun color(): Color {
        return Color(237, 142, 26)
    }


}