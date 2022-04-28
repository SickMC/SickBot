package net.sickmc.sickbot.utils

import dev.kord.common.Color
import dev.kord.core.entity.Member
import dev.kord.core.entity.User
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.builder.message.EmbedBuilder
import net.sickmc.sickbot.kord

object EmbedVariables {

    fun title(title: String): String{
        return "**$title | SickMC**"
    }
    suspend fun selfFooter(): EmbedBuilder.Footer {
        val footerbuilder = EmbedBuilder.Footer()
        footerbuilder.icon = kord.getSelf(EntitySupplyStrategy.rest).avatar?.url
        footerbuilder.text = "SickMC • Requested by SickMC"
        return footerbuilder
    }
    fun userFooter(user: User): EmbedBuilder.Footer{
        val footerbuilder = EmbedBuilder.Footer()
        footerbuilder.icon = user.avatar?.url
        footerbuilder.text = "SickMC • Requested by ${user.username}#${user.discriminator}"
        return footerbuilder
    }

    fun color(): Color {
        return Color(237, 142, 26)
    }


}