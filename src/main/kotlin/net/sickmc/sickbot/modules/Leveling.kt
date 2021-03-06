package net.sickmc.sickbot.modules

import com.mongodb.client.model.Filters
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.event.interaction.GuildSelectMenuInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import net.sickmc.sickbot.kord
import net.sickmc.sickbot.liveMembers
import net.sickmc.sickbot.mainGuild
import net.sickmc.sickbot.utils.*
import org.bson.Document
import java.util.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val levelingDocs = hashMapOf<Member, Document>()

@OptIn(KordPreview::class)
object Leveling {

    private val messageCooldowns = hashMapOf<Snowflake, Long>()
    private val voiceCooldowns = hashMapOf<Snowflake, Long>()
    private var refreshCooldown = Clock.System.now().toEpochMilliseconds() + 30.seconds.inWholeMilliseconds
    private val ignoredVoiceChannels = arrayListOf<Snowflake>()
    private val ignoredMessageChannels = arrayListOf<Snowflake>()
    private val levelingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var ranking = listOf<Member>()

    suspend fun register() {
        handleMessages()
        handleVoiceJoin()
        handleActiveVoice()
        handleRanking()
        handleRankMessage()
        handleLevelInfo()
        updater()
        UnclaimedRewardHandler.register()
    }

    private suspend fun updater() {
        levelingScope.launch {
            while (true) {
                delay(5.minutes)
                levelingDocs.filter { !it.key.isBot }.forEach {
                    levelingColl.replaceOne(Filters.eq("id", it.key.id.toString()), it.value)
                }
            }
        }
    }

    private fun handleMessages() {
        kord.on<MessageCreateEvent> {
            if (message.getGuild() != mainGuild) return@on
            if (message.author == null) return@on
            if (message.author!!.isBot) return@on
            if (ignoredMessageChannels.contains(message.channel.id)) return@on
            val member = message.getAuthorAsMember()!!
            if (!messageCooldowns.containsKey(member.id)) {
                messageCooldowns[member.id] = Clock.System.now().toEpochMilliseconds() + 1.minutes.inWholeMilliseconds
                handlePoints(member)
                checkLevelMessage(message)
                return@on
            }
            if (messageCooldowns[member.id]!! > Clock.System.now().toEpochMilliseconds()) return@on
            messageCooldowns[member.id] = Clock.System.now().toEpochMilliseconds() + 1.minutes.inWholeMilliseconds
            handlePoints(member)
            checkLevelMessage(message)
        }
    }

    private fun handleVoiceJoin() {
        kord.on<VoiceStateUpdateEvent> {
            if (state.getMember().isBot) return@on
            if (state.getGuild() != mainGuild) return@on
            if (ignoredVoiceChannels.contains(state.channelId)) return@on
            val member = state.getMember()
            if (!voiceCooldowns.containsKey(member.id)) {
                voiceCooldowns[member.id] = Clock.System.now().toEpochMilliseconds() + 5.minutes.inWholeMilliseconds
                handlePoints(member)
                checkLevelVoice(member)
                return@on
            }
            if (voiceCooldowns[member.id]!! > Clock.System.now().toEpochMilliseconds()) return@on
            voiceCooldowns[member.id] = Clock.System.now().toEpochMilliseconds() + 5.minutes.inWholeMilliseconds
            handlePoints(member)
            checkLevelVoice(member)
        }
    }

    private suspend fun handleActiveVoice() {
        levelingScope.launch {
            while (true) {
                delay(5.minutes)
                val actives = liveMembers.filter {
                    it.member.getVoiceStateOrNull() != null && it.member.getVoiceState().channelId != null && !it.member.isBot && !it.member.getVoiceState().isSuppressed && !it.member.getVoiceState().isSelfMuted && !it.member.getVoiceState().isMuted && !it.member.getVoiceState().isDeafened && !it.member.getVoiceState().isSelfDeafened && it.member.getVoiceState().channelId != config.getString(
                        "afkchannel"
                    ).toSnowflake()
                }
                actives.forEach {
                    if (!voiceCooldowns.containsKey(it.id)) {
                        voiceCooldowns[it.id] = Clock.System.now().toEpochMilliseconds() + 5.minutes.inWholeMilliseconds
                        handlePoints(it.member, 2)
                        checkLevelVoice(it.member, 2)
                        return@forEach
                    }
                    if (voiceCooldowns[it.id]!! > Clock.System.now().toEpochMilliseconds()) return@forEach
                    voiceCooldowns[it.id] = Clock.System.now().toEpochMilliseconds() + 5.minutes.inWholeMilliseconds
                    handlePoints(it.member, 2)
                    checkLevelVoice(it.member, 2)
                }
            }
        }
    }

    private suspend fun handleRanking() {
        levelingScope.launch {
            while (true) {
                delay(1.minutes)
                val channel = mainGuild.getChannel(Snowflake(config.getString("rankingChannel"))) as MessageChannel
                var message = channel.getMessageOrNull(Snowflake(config.getString("rankingMessage")))
                if (message == null) {
                    reloadConfig()
                    message = channel.getMessageOrNull(Snowflake(config.getString("rankingMessage")))
                }
                ranking = levelingDocs.filter { !it.key.isBot }.entries.sortedBy { it.value.getInteger("points") }
                    .map { it.key }.reversed()
                message!!.edit {
                    embed {
                        title = EmbedVariables.title("Ranking")
                        timestamp = Clock.System.now()
                        footer = EmbedVariables.userFooter(ranking[0])
                        color = levelColor
                        description = """
                            > **1.** ${ranking[0].mention} - ${levelingDocs[ranking[0]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **2.** ${ranking[1].mention} - ${levelingDocs[ranking[1]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **3.** ${ranking[2].mention} - ${levelingDocs[ranking[2]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **4.** ${ranking[3].mention} - ${levelingDocs[ranking[3]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **5.** ${ranking[4].mention} - ${levelingDocs[ranking[4]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **6.** ${ranking[5].mention} - ${levelingDocs[ranking[5]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **7.** ${ranking[6].mention} - ${levelingDocs[ranking[6]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **8.** ${ranking[7].mention} - ${levelingDocs[ranking[7]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **9.** ${ranking[8].mention} - ${levelingDocs[ranking[8]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **10.** ${ranking[9].mention} - ${levelingDocs[ranking[9]]?.getInteger("points")} <:sickball:975024822520283156>
                            *updates every minute*
                        """.trimIndent()
                    }
                    actionRow {
                        interactionButton(ButtonStyle.Primary, "leveling_rank") {
                            label = "Your rank"
                        }
                        interactionButton(ButtonStyle.Primary, "level_info") {
                            label = "Levels"
                            emoji = DiscordPartialEmoji(id = Snowflake("975024822520283156"))
                        }
                        interactionButton(ButtonStyle.Secondary, "level_unclaimed") {
                            label = "Unclaimed Rewards"
                        }
                    }
                }
            }
        }
        kord.on<GuildButtonInteractionCreateEvent> {
            if (interaction.componentId != "leveling_rank") return@on
            if (interaction.user.isBot) return@on
            val response = interaction.deferEphemeralResponse()
            val member = this.interaction.user.asMember()
            if (!ranking.contains(member)) {
                response.respond {
                    content =
                        "Your rank cannot be found!\nCheck if you interacted on this guild before (chatting/talking)\nIf you think this is an error please open a ticket."
                }
                return@on
            }
            response.respond {
                embed {
                    val points = levelingDocs[member]?.getInteger("points")!!
                    val percentToNext =
                        ((points.toDouble() / Level.getLevel(points).getNext().from.toDouble()) * 100).toInt()
                    val progressBuilder = StringBuilder()
                    val progressOnBar = percentToNext / 5
                    var bar = 20
                    repeat(progressOnBar) {
                        progressBuilder.append("| ")
                        bar--
                    }
                    repeat(bar) {
                        progressBuilder.append("  ")
                    }
                    title = EmbedVariables.title("Rank")
                    description = """
                        > **Points:** $points
                        > **Rank:** ${ranking.indexOf(member) + 1}
                        > **Level:** ${Level.getLevel(points).icon} ${Level.getLevel(points).formattedName}
                        > **Next Level:** ${Level.getLevel(points).getNext().icon} ${
                        Level.getLevel(points).getNext().formattedName
                    }
                        > **Progress:** `$progressBuilder` $percentToNext%
                    """.trimIndent()
                    timestamp = Clock.System.now()
                    footer = EmbedVariables.userFooter(member)
                    color = levelColor
                }
            }
        }
    }

    private fun handleLevelInfo() {
        kord.on<GuildButtonInteractionCreateEvent> {
            if (interaction.componentId != "level_info") return@on
            if (interaction.user.isBot) return@on
            val response = interaction.deferEphemeralResponse()
            response.respond {
                val builder = StringBuilder()
                Level.levels.forEach {
                    builder.append("\n > ${it.icon} **${it.formattedName}:** (${it.from} <:sickball:975024822520283156>) ?? ${it.reward.rewardDescription}")
                }
                embed {
                    title = EmbedVariables.title("Levels")
                    footer = EmbedVariables.userFooter(interaction.user)
                    color = levelColor

                    description = """
                        $builder
                    """.trimIndent()
                }
            }
        }
    }

    private fun handleRankMessage() {
        kord.on<MessageCreateEvent> {
            if (message.content != "!ranking") return@on
            if (message.getGuildOrNull() == null) return@on
            if (message.author?.isBot == true) return@on
            if (!message.author?.asMember(mainGuild.id)?.roleIds?.contains(RoleIDs.getId("Administration"))!!) return@on
            ranking = levelingDocs.entries.sortedBy { it.value.getInteger("points") }.map { it.key }.reversed()
            config.replace("rankingMessage", message.getChannel().createMessage {
                embed {
                    title = EmbedVariables.title("Ranking")
                    timestamp = Clock.System.now()
                    footer = EmbedVariables.userFooter(ranking[0])
                    color = levelColor
                    description = """
                            > **1.** ${ranking[0].mention} - ${levelingDocs[ranking[0]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **2.** ${ranking[1].mention} - ${levelingDocs[ranking[1]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **3.** ${ranking[2].mention} - ${levelingDocs[ranking[2]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **4.** ${ranking[3].mention} - ${levelingDocs[ranking[3]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **5.** ${ranking[4].mention} - ${levelingDocs[ranking[4]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **6.** ${ranking[5].mention} - ${levelingDocs[ranking[5]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **7.** ${ranking[6].mention} - ${levelingDocs[ranking[6]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **8.** ${ranking[7].mention} - ${levelingDocs[ranking[7]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **9.** ${ranking[8].mention} - ${levelingDocs[ranking[8]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **10.** ${ranking[9].mention} - ${levelingDocs[ranking[9]]?.getInteger("points")} <:sickball:975024822520283156>
                            *updates every minute*
                        """.trimIndent()
                }
                actionRow {
                    interactionButton(ButtonStyle.Primary, "leveling_rank") {
                        label = "Your rank"
                    }
                    interactionButton(ButtonStyle.Primary, "level_info") {
                        label = "Levels"
                        emoji = DiscordPartialEmoji(id = Snowflake("975024822520283156"))
                    }
                    interactionButton(ButtonStyle.Secondary, "level_unclaimed") {
                        label = "Unclaimed Rewards"
                    }
                }
            }.id.toString())
            configColl.replaceOne(Filters.eq("type", "discordbot"), config)
            message.delete("Ranking was send!")
        }
    }

    private suspend fun handlePoints(member: Member, increment: Int = 1) {
        if (member.isBot) return
        if (!levelingDocs.containsKey(member)) {
            var doc = levelingColl.findOne(Filters.eq("id", member.id.toString()))
            if (doc == null) {
                doc = Document("id", member.id.toString()).append("points", increment)
                    .append("unclaimedRewards", arrayListOf<String>())
                levelingDocs[member] = doc
                levelingColl.insertOne(doc)
            }
            levelingDocs[member] = doc ?: error("document was null in Leveling Module - ${member.id.toString()}")
        } else {
            val doc = levelingDocs[member]!!
            doc.replace("points", doc.getInteger("points").plus(increment))
            levelingDocs[member] = doc
            if (refreshCooldown < Clock.System.now().toEpochMilliseconds()) {
                levelingColl.replaceOne(
                    Filters.eq("id", member.id.toString()),
                    levelingDocs[member] ?: error("${member.id.toString()} was not in players")
                )
                refreshCooldown = Clock.System.now().toEpochMilliseconds() + 30.seconds.inWholeMilliseconds
            }
        }
    }

    private fun checkLevel(member: Member, increment: Int = 1): LevelChange? {
        if (member.isBot) return null
        if (levelingDocs[member] == null) return null
        val to = Level.getLevel(levelingDocs[member]!!.getInteger("points"))
        if (Level.getLevel(levelingDocs[member]!!.getInteger("points")) == Level.getLevel(
                levelingDocs[member]!!.getInteger(
                    "points"
                ).minus(increment)
            )
        ) return null
        val rewards = levelingDocs[member]!!.getList("unclaimedRewards", String::class.java) as ArrayList<String>
        rewards.add(to.name)
        levelingDocs[member]?.replace("unclaimedRewards", rewards)
        return LevelChange(Level.getLevel(levelingDocs[member]!!.getInteger("points").minus(increment)), to)
    }

    private suspend fun checkLevelMessage(message: Message) {
        val member = message.getAuthorAsMember()!!
        if (member.isBot) return
        val levelChange = checkLevel(member) ?: return
        message.reply {
            embed {
                title = EmbedVariables.title("Level Up")
                timestamp = Clock.System.now()
                footer = EmbedVariables.userFooter(member)
                color = levelColor
                description = """
                            *Congratulations ${member.mention}* <a:party:959481387092676618>
                            > **previous Level:** ${levelChange.from.icon} ${levelChange.from.formattedName}
                            > **your Level:** ${levelChange.to.icon} ${levelChange.to.formattedName}
                            > **your Reward:** ${levelChange.to.reward.rewardDescription}
                            You can claim your reward in <#${config.getString("rankingChannel")}>
                        """.trimIndent()
            }
            actionRow {
                interactionButton(ButtonStyle.Primary, "level_info") {
                    label = "Levels"
                    emoji = DiscordPartialEmoji(id = Snowflake("975024822520283156"))
                }
            }
        }
    }

    private suspend fun checkLevelVoice(member: Member, increment: Int = 1) {
        if (member.isBot) return
        val levelChange = checkLevel(member, increment) ?: return
        val chat = mainGuild.getChannel(Snowflake(config.getString("generalChat"))) as MessageChannel
        chat.createMessage {
            embed {
                title = EmbedVariables.title("Level Up")
                timestamp = Clock.System.now()
                footer = EmbedVariables.userFooter(member)
                color = levelColor
                description = """
                            *Congratulations ${member.mention}* <a:party:959481387092676618>
                            > **previous Level** ${levelChange.from.formattedName}
                            > **your Level** ${levelChange.to.formattedName}
                            > **your Reward** ${levelChange.to.reward.rewardDescription}
                        """.trimIndent()
            }
            actionRow {
                interactionButton(ButtonStyle.Primary, "level_info") {
                    label = "Levels"
                    emoji = DiscordPartialEmoji(Snowflake("975024822520283156"))
                }
            }
        }
    }
}

enum class Level(val from: Int, val to: Int?, val reward: LevelReward, val formattedName: String, val icon: String) {

    WOOD(0, 499, EmptyReward(), "Wood", "<:sickwood:975034271880343552>"), STONE(
        500, 1499, AchievementReward("stoned"), "Stone", "<:sickstone:975035457425510410>"
    ),
    COAL(1500, 2999, SmucksReward(1000), "Coal", "<:sickcoal:975036476247117834>"), IRON(
        3000, 4999, RankReward("Wither", 5.days.inWholeMilliseconds), "Iron", "<:sickiron:975037086329618502>"
    ),
    GOLD(5000, 7249, GadgetReward("gold"), "Gold", "<:sickgold:975037608692432927>"), REDSTONE(
        7250, 9499, SmucksReward(5000), "Redstone", "<:sickredstone:975038278115917875>"
    ),
    LAPIS(
        9500, 11999, RankReward("Dragon", 5.days.inWholeMilliseconds), "Lapis", "<:sicklapis:975038899531431936>"
    ),
    EMERALD(12000, 14999, GadgetReward("emerald"), "Emerald", "<:sickemerald:975039429066522634>"), DIAMOND(
        15000, 19999, SmucksReward(10000), "Diamond", "<:sickdiamond:975039813407354951>"
    ),
    NETHERITE(20000, null, RankReward("Wither", null), "Netherite", "<:sicknetherite:975040225829064754>");

    companion object {

        val levels = listOf(WOOD, STONE, COAL, IRON, GOLD, REDSTONE, LAPIS, EMERALD, DIAMOND, NETHERITE)
        fun getLevel(points: Int): Level {
            var level: Level? = null
            Level.values().forEach {
                if (it.from > points) return@forEach
                if (it.to == null) return NETHERITE
                if (it.to < points) return@forEach
                level = it
            }
            return level!!
        }

    }

    fun getNext(): Level {
        val slot = levels.indexOf(this)
        return levels[slot + 1]
    }

}

data class LevelChange(val from: Level, val to: Level)
abstract class LevelReward {

    abstract val rewardDescription: String
    abstract suspend fun perform(member: Member)

}

class EmptyReward : LevelReward() {

    override val rewardDescription: String = "No Reward"
    override suspend fun perform(member: Member) {}

}

class GadgetReward(val gadget: String) : LevelReward() {

    override val rewardDescription: String = "GadgetReward ($gadget)"
    override suspend fun perform(member: Member) {
        val playerDoc = playerColl.findOne(Filters.eq("discordID", member.id.toString()))!!
        val lobbyDoc = lobbyColl.findOne(
            Filters.eq("uuid", playerDoc.getString("uuid"))
        )!!
        val gadgets = lobbyDoc.getList("gadgets", String::class.java).add(gadget)
        lobbyDoc.replace("gadgets", gadgets)
        lobbyColl.updateOne(
            Filters.eq("uuid", playerDoc.getString("uuid")),
            lobbyDoc
        )
        sendLevelPerformMessage(
            UUID.fromString(playerDoc.getString("uuid")),
            "You have received your GadgetReward $gadget"
        )
    }

}

class SmucksReward(val coins: Int) : LevelReward() {

    override val rewardDescription: String = "SmucksReward ($coins)"
    override suspend fun perform(member: Member) {
        val playerDoc = playerColl.findOne(Filters.eq("discordID", member.id.toString()))!!
        val smucks = playerDoc.getInteger("smucks")
        playerDoc.replace("smucks", smucks + coins)
        playerColl.replaceOne(Filters.eq("discordID", member.id.toString()), playerDoc)
        sendLevelPerformMessage(
            UUID.fromString(playerDoc.getString("uuid")),
            "You claimed your SmucksReward: $coins Smucks!"
        )
    }

}

class RankReward(private val rank: String, private val expire: Long?) : LevelReward() {

    override val rewardDescription: String = "RankReward ($rank - ${expire?.milliseconds?.toString() ?: "lifetime"})"
    override suspend fun perform(member: Member) {
        val playerDoc = playerColl.findOne(Filters.eq("discordID", member.id.toString()))!!
        if (expire == null) playerDoc.replace("permanentRank", rank)
        else {
            playerDoc.replace("rank", rank)
            playerDoc.replace("rankExpire", expire.toString())
        }
        playerColl.replaceOne(Filters.eq("discordID", member.id.toString()), playerDoc)
        sendLevelPerformMessage(
            UUID.fromString(playerDoc.getString("uuid")),
            if (expire == null) "You claimed the rank: $rank" else "You claimed the rank: $rank for ${expire.milliseconds.toString()}"
        )
    }

}

class AchievementReward(private val achievement: String) : LevelReward() {

    override val rewardDescription: String = "AchievementReward ($achievement)"

    override suspend fun perform(member: Member) {
        val playerDoc = playerColl.findOne(Filters.eq("discordID", member.id.toString()))!!
        playerDoc.getList("achievements", String::class.java).add(achievement)
        playerColl.replaceOne(Filters.eq("discordID", member.id.toString()), playerDoc)
        sendLevelPerformMessage(
            UUID.fromString(playerDoc.getString("uuid")),
            "You claimed the achievement: $achievement"
        )
    }

}

suspend fun sendLevelPerformMessage(player: UUID, message: String) {
    sendChannelMessage("rewards", "discord/$player/$message")
}

object UnclaimedRewardHandler {

    fun register() {
        buttonListener
        menuListener
    }

    private val buttonListener = kord.on<GuildButtonInteractionCreateEvent> {
        if (interaction.componentId != "level_unclaimed") return@on
        val doc = levelingDocs[interaction.user.asMember()] ?: return@on
        val response = interaction.deferEphemeralResponse()
        val member = interaction.user.asMember()
        if (!member.isVerified()) {
            response.respond {
                content = "You must be verified to use this feature!"
            }
            return@on
        }
        if (doc.getList("unclaimedRewards", String::class.java).isEmpty()) {
            response.respond {
                embed {
                    title = "No Unclaimed Rewards"
                    description = """
                            You have to level to get rewards.
                            You can check your level and the rewards in the <#${config.getString("rankingChannel")}> channel.
                        """.trimIndent()
                    color = levelColor
                }
            }
            return@on
        }
        val rewardLevels = doc.getList("unclaimedRewards", String::class.java).map { Level.valueOf(it) }
        response.respond {
            actionRow {
                selectMenu("unclaimed_rewards_picking") {
                    rewardLevels.forEach {
                        option(
                            label = it.reward.rewardDescription,
                            value = it.name
                        )
                    }
                    allowedValues = 1 .. rewardLevels.size
                }
            }
        }
    }

    private val menuListener = kord.on<GuildSelectMenuInteractionCreateEvent> {
        if (interaction.componentId != "unclaimed_rewards_picking") return@on
        val member = interaction.user.asMember()
        val doc = levelingDocs[interaction.user.asMember()] ?: return@on
        val docList = doc.getList("unclaimedRewards", String::class.java)
        val selected = interaction.values.map { Level.valueOf(it) }
        selected.forEach {
            it.reward.perform(member)
            docList.remove(it.name)
        }
        doc.replace("unclaimedRewards", docList)
        levelingDocs[member] = doc
        interaction.respondEphemeral {
            embed {
                title = "Rewards"
                description = """
                        You have claimed the following rewards: 
                        **${selected.joinToString("\n") { it.reward.rewardDescription }}**
                    """.trimIndent()
                color = levelColor
            }
        }
    }

}
