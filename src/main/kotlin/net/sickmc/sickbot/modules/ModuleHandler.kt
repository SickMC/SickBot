package net.sickmc.sickbot.modules

object ModuleHandler {
    suspend fun register(){
        Log.register()
        Rules.register()
        Tickets.register()
        Moderation.register()
        Leveling.register()
        Staff.register()
    }

}
