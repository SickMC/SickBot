package net.sickmc.sickbot.modules

import com.mongodb.client.model.Filters
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.core.on
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.modify.embed
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.actionRow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import net.sickmc.sickbot.kord
import net.sickmc.sickbot.mainGuild
import net.sickmc.sickbot.utils.EmbedVariables
import net.sickmc.sickbot.utils.config
import net.sickmc.sickbot.utils.levelingColl
import net.sickmc.sickbot.utils.*
import org.bson.Document
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object Leveling {

    private val messageCooldowns = hashMapOf<Member, Long>()
    private val voiceCooldowns = hashMapOf<Member, Long>()
    private val players = hashMapOf<Member, Document>()
    private var refreshCooldown = Clock.System.now().toEpochMilliseconds() + 30.seconds.inWholeMilliseconds
    private val ignoredVoiceChannels = arrayListOf<Snowflake>()
    private val ignoredMessageChannels = arrayListOf<Snowflake>()
    private val levelingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var ranking = listOf<Member>()
    private var rankingData = hashMapOf<Member, Document>()

    suspend fun register() {
        handleMessages()
        handleVoiceJoin()
        //handleActiveVoice()
        handleRanking()
        handleRankMessage()
        handleLevelInfo()
        updater()
    }

    private suspend fun updater() {
        levelingScope.launch {
            while (true) {
                delay(5.minutes)
                players.forEach {
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
            if (!messageCooldowns.containsKey(member)){
                messageCooldowns[member] = Clock.System.now().toEpochMilliseconds() + 1.minutes.inWholeMilliseconds
                handlePoints(member)
                checkLevelMessage(message)
                return@on
            }
            if (messageCooldowns[member]!! > Clock.System.now().toEpochMilliseconds()) return@on
            messageCooldowns[member] = Clock.System.now().toEpochMilliseconds() + 1.minutes.inWholeMilliseconds
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
            if (!voiceCooldowns.containsKey(state.getMember())){
                voiceCooldowns[member] = Clock.System.now().toEpochMilliseconds() + 5.minutes.inWholeMilliseconds
                handlePoints(member)
                checkLevelVoice(member)
                return@on
            }
            if (voiceCooldowns[member]!! > Clock.System.now().toEpochMilliseconds()) return@on
            voiceCooldowns[member] = Clock.System.now().toEpochMilliseconds() + 5.minutes.inWholeMilliseconds
            handlePoints(member)
            checkLevelVoice(member)
        }
    }

    private suspend fun handleActiveVoice() {
        levelingScope.launch {
            while (true) {
                delay(5.minutes)
                val states = mainGuild.voiceStates.filter {
                    !it.isDeafened && !it.isSelfDeafened && !it.isMuted && !it.isSelfMuted && !it.isSuppressed && !ignoredVoiceChannels.contains(it.channelId)
                }.toList()
                states.forEach {
                    val member = it.getMember()
                    if (!voiceCooldowns.containsKey(member)){
                        voiceCooldowns[member] = Clock.System.now().toEpochMilliseconds() + 5.minutes.inWholeMilliseconds
                        handlePoints(member, 2)
                        checkLevelVoice(member, 2)
                        return@forEach
                    }
                    if (voiceCooldowns[member]!! > Clock.System.now().toEpochMilliseconds()) return@forEach
                    voiceCooldowns[member] = Clock.System.now().toEpochMilliseconds() + 5.minutes.inWholeMilliseconds
                    handlePoints(member, 2)
                    checkLevelVoice(member, 2)
                }
            }
        }
    }

    private suspend fun handleRanking() {
        val channel = mainGuild.getChannel(Snowflake(config.getString("rankingChannel"))) as MessageChannel
        val message = channel.getMessage(Snowflake(config.getString("rankingMessage")))
        levelingScope.launch {
            while (true) {
                delay(5.minutes)
                val filteredPlayers = hashMapOf<Member, Document>()
                rankingData = filteredPlayers
                levelingColl.find().toList().forEach {
                    try {
                        val member = mainGuild.getMember(Snowflake(it.getString("id")))
                        filteredPlayers[member] = it
                    } catch (e: EntityNotFoundException) {
                        return@forEach
                    }
                }
                val sorted = filteredPlayers.entries.sortedBy { it.value.getInteger("points") }
                    .map { it.key }.reversed()
                ranking = sorted
                message.edit {
                    embed {
                        title = EmbedVariables.title("Ranking")
                        timestamp = Clock.System.now()
                        footer = EmbedVariables.userFooter(sorted[0])
                        color = levelColor
                        description = """
                            > **1.** ${sorted[0].mention} - ${filteredPlayers[sorted[0]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **2.** ${sorted[1].mention} - ${filteredPlayers[sorted[1]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **3.** ${sorted[2].mention} - ${filteredPlayers[sorted[2]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **4.** ${sorted[3].mention} - ${filteredPlayers[sorted[3]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **5.** ${sorted[4].mention} - ${filteredPlayers[sorted[4]]?.getInteger("points")} <:sickball:975024822520283156>
                            *updates every 5 minutes*
                        """.trimIndent()
                    }
                    actionRow {
                        interactionButton(ButtonStyle.Primary, "leveling_rank"){
                            label = "Your rank"
                        }
                        interactionButton(ButtonStyle.Primary, "level_info"){
                            label = "Levels"
                            emoji = DiscordPartialEmoji(id = Snowflake("975024822520283156"))
                        }
                    }
                }
            }
        }
        kord.on<ButtonInteractionCreateEvent> {
            if(interaction.componentId != "leveling_rank")return@on
            if(interaction.user.isBot)return@on
            val response = interaction.deferEphemeralResponse()
            val member = interaction.user.asMember(mainGuild.id)
            if(!ranking.contains(member)){
                response.respond { content = "Your rank cannot be found!\nCheck if you interacted on this guild before (chatting/talking)\nIf you think this is an error please open a ticket." }
                return@on
            }
            response.respond {
                embed {
                    val points = rankingData[member]?.getInteger("points")!!
                    val percentToNext = ((points.toDouble() / Level.getLevel(points).getNext().from.toDouble()) * 100).toInt()
                    val progressBuilder = StringBuilder()
                    val progressOnBar = percentToNext / 5
                    var bar = 20
                    repeat(progressOnBar){
                        progressBuilder.append("| ")
                        bar--
                    }
                    repeat(bar){
                        progressBuilder.append("  ")
                    }
                    title = EmbedVariables.title("Rank")
                    description = """
                        > **Points:** $points
                        > **Rank:** ${ranking.indexOf(member) + 1}
                        > **Level:** ${Level.getLevel(points).icon} ${Level.getLevel(points).formattedName}
                        > **Next Level:** ${Level.getLevel(points).getNext().icon} ${Level.getLevel(points).getNext().formattedName}
                        > **Progress:** `$progressBuilder` $percentToNext%
                    """.trimIndent()
                    timestamp = Clock.System.now()
                    footer = EmbedVariables.userFooter(member)
                    color = levelColor
                }
            }
        }
    }

    private fun handleLevelInfo(){
        kord.on<ButtonInteractionCreateEvent> {
            if (interaction.componentId != "level_info")return@on
            if (interaction.user.isBot)return@on
            val response = interaction.deferEphemeralResponse()
            response.respond {
                val builder = StringBuilder()
                Level.levels.forEach{
                    builder.append("\n > ${it.icon} **${it.formattedName}:** (${it.from} <:sickball:975024822520283156>) × ${it.reward?.rewardDescription ?: "none"}")
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
            val filteredPlayers = hashMapOf<Member, Document>()
            rankingData = filteredPlayers
            levelingColl.find().toList().forEach {
                try {
                    val member = mainGuild.getMember(Snowflake(it.getString("id")))
                    filteredPlayers[member] = it
                } catch (e: EntityNotFoundException) {
                    return@forEach
                }
            }
            val sorted = filteredPlayers.entries.sortedBy { it.value.getInteger("points") }
                .map { it.key }.reversed()
            ranking = sorted
            config.replace("rankingMessage", message.getChannel().createMessage {
                embed {
                    title = EmbedVariables.title("Ranking")
                    timestamp = Clock.System.now()
                    footer = EmbedVariables.selfFooter()
                    color = levelColor
                    description = """
                            > **1.** ${sorted[0].mention} - ${filteredPlayers[sorted[0]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **2.** ${sorted[1].mention} - ${filteredPlayers[sorted[1]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **3.** ${sorted[2].mention} - ${filteredPlayers[sorted[2]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **4.** ${sorted[3].mention} - ${filteredPlayers[sorted[3]]?.getInteger("points")} <:sickball:975024822520283156>
                            > **5.** ${sorted[4].mention} - ${filteredPlayers[sorted[4]]?.getInteger("points")} <:sickball:975024822520283156>
                            *updates every 5 minutes*
                        """.trimIndent()
                }
                actionRow {
                    interactionButton(ButtonStyle.Primary, "leveling_rank"){
                        label = "Your rank"
                    }
                    interactionButton(ButtonStyle.Primary, "level_info"){
                        label = "Levels"
                        emoji = DiscordPartialEmoji(id = Snowflake("975024822520283156"))
                    }
                }
            }.id.toString())
            configColl.replaceOne(Filters.eq("type", "discordbot"), config)
            message.delete("Ranking was send!")
        }
    }

    private suspend fun handlePoints(member: Member, increment: Int = 1) {
        if (!players.containsKey(member)) {
            var doc = levelingColl.findOne(Filters.eq("id", member.id.toString()))
            if (doc == null) {
                doc = Document("id", member.id.toString()).append("points", increment).append("unclaimedRewards", arrayListOf<String>())
                levelingColl.insertOne(doc)
            }
            players[member] = doc ?: error("document was null in Leveling Module - ${member.id}")
        } else {
            players[member]?.replace("points", players[member]?.getInteger("points")?.plus(increment))
            if (refreshCooldown < Clock.System.now().toEpochMilliseconds()) {
                levelingColl.replaceOne(
                    Filters.eq("id", member.id.toString()),
                    players[member] ?: error("${member.id} was not in players")
                )
                refreshCooldown = Clock.System.now().toEpochMilliseconds() + 30.seconds.inWholeMilliseconds
            }
        }
    }

    private fun checkLevel(member: Member, increment: Int = 1): LevelChange?{
        if (players[member] == null)return null
        val to = Level.getLevel(players[member]!!.getInteger("points"))
        if (Level.getLevel(players[member]!!.getInteger("points")) == Level.getLevel(players[member]!!.getInteger("points").minus(increment)))return null
        players[member]?.replace("unclaimedRewards", (players[member]?.getList("unclaimedRewards", String::class.java) as ArrayList<String>).add(to.name))
        return LevelChange(Level.getLevel(players[member]!!.getInteger("points").minus(increment)), to)
    }

    private suspend fun checkLevelMessage(message: Message){
        val member = message.getAuthorAsMember()!!
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
                            > **your Reward:** ${levelChange.to.reward?.rewardDescription}
                        """.trimIndent()
            }
            actionRow {
                interactionButton(ButtonStyle.Primary, "level_info"){
                    label = "Levels"
                    emoji = DiscordPartialEmoji(id = Snowflake("975024822520283156"))
                }
            }
        }
    }

    private suspend fun checkLevelVoice(member: Member, increment: Int = 1){
        val levelChange = checkLevel(member, increment) ?: return
        val chat = mainGuild.getChannel(Snowflake(config.getString("generalChat"))) as MessageChannel
        chat.createMessage{
            embed {
                title = EmbedVariables.title("Level Up")
                timestamp = Clock.System.now()
                footer = EmbedVariables.userFooter(member)
                color = levelColor
                description = """
                            *Congratulations ${member.mention}* <a:party:959481387092676618>
                            > **previous Level** ${levelChange.from.formattedName}
                            > **your Level** ${levelChange.to.formattedName}
                            > **your Reward** ${levelChange.to.reward?.rewardDescription}
                        """.trimIndent()
            }
            actionRow {
                interactionButton(ButtonStyle.Primary, "level_info"){
                    label = "Levels"
                    emoji = DiscordPartialEmoji(Snowflake("975024822520283156"))
                }
            }
        }
    }
}

enum class Level(val from: Int, val to: Int?, val reward: LevelReward?, val formattedName: String, val icon: String){

    WOOD(0, 499, null, "Wood", "<:sickwood:975034271880343552>"),
    STONE(500, 1499, AchievementReward("stoned"), "Stone", "<:sickstone:975035457425510410>"),
    COAL(1500, 2999, CoinReward(1000), "Coal", "<:sickcoal:975036476247117834>"),
    IRON(3000, 4999, RankReward("Wither", 5.days.inWholeMilliseconds), "Iron", "<:sickiron:975037086329618502>"),
    GOLD(5000, 7249, GadgetReward("gold"), "Gold", "<:sickgold:975037608692432927>"),
    REDSTONE(7250, 9499, CoinReward(5000), "Redstone", "<:sickredstone:975038278115917875>"),
    LAPIS(9500, 11999, RankReward("Dragon", 5.days.inWholeMilliseconds), "Lapis", "<:sicklapis:975038899531431936>"),
    EMERALD(12000, 14999, GadgetReward("emerald"), "Emerald", "<:sickemerald:975039429066522634>"),
    DIAMOND(15000, 19999, CoinReward(10000), "Diamond", "<:sickdiamond:975039813407354951>"),
    NETHERITE(20000, null, RankReward("Wither", null), "Netherite", "<:sicknetherite:975040225829064754>");

    companion object{

        val levels = listOf(WOOD, STONE, COAL, IRON, GOLD, REDSTONE, LAPIS, EMERALD, DIAMOND, NETHERITE)
        fun getLevel(points: Int): Level{
            var level: Level? = null
            Level.values().forEach {
                if (it.from > points)return@forEach
                if (it.to == null)return NETHERITE
                if (it.to < points)return@forEach
                level = it
            }
            return level!!
        }

    }
    fun getNext(): Level{
        val slot = levels.indexOf(this)
        return levels[slot + 1]
    }

}

data class LevelChange(val from: Level, val to: Level)
abstract class LevelReward{

    abstract val rewardDescription: String
    abstract fun perform(member: Member)

}

class GadgetReward(gadget: String) : LevelReward() {

    override val rewardDescription: String = "GadgetReward ($gadget)"
    override fun perform(member: Member) {
        TODO("Not yet implemented")
    }

}

class CoinReward(coins: Int): LevelReward(){

    override val rewardDescription: String = "CoinReward ($coins)"
    override fun perform(member: Member) {
        TODO("Not yet implemented")
    }

}

class RankReward(rank: String, expire: Long?): LevelReward(){

    override val rewardDescription: String = "RankReward ($rank - ${expire?.milliseconds?.toString() ?: "lifetime"})"
    override fun perform(member: Member) {
        TODO("Not yet implemented")
    }

}

class AchievementReward(achievment: String): LevelReward(){

    override val rewardDescription: String = "AchievementReward ($achievment)"

    override fun perform(member: Member) {
        TODO("Not yet implemented")
    }

}




