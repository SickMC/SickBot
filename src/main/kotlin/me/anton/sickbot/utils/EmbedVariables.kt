package me.anton.sickbot.utils

import dev.kord.common.Color
import dev.kord.core.entity.Member
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.builder.message.EmbedBuilder
import me.anton.sickbot.SickBot

class EmbedVariables {

    companion object{

        suspend fun selfFooter(): EmbedBuilder.Footer{
            val footerbuilder = EmbedBuilder.Footer()
            footerbuilder.icon = SickBot.instance.kord.getSelf(EntitySupplyStrategy.rest).avatar!!.url
            footerbuilder.text = "SickMC • Requested by SickMC"
            return footerbuilder
        }

        fun userFooter(member: Member): EmbedBuilder.Footer{
            val footerbuilder = EmbedBuilder.Footer()
            footerbuilder.icon = member.avatar!!.url
            footerbuilder.text = "SickMC • Requested by ${member.username}#${member.discriminator}"
            return footerbuilder
        }

        fun color(): Color{
            return Color(237, 142, 26)
        }

    }

}