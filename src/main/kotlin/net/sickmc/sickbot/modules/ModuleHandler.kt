package net.sickmc.sickbot.modules

object ModuleHandler {
    suspend fun register(){
        Log.register()
        Rules.register()
        Lobby.register()
        MemberHandler.register()
    }

}