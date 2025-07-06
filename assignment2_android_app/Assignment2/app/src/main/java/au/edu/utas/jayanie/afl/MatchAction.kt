package au.edu.utas.jayanie.afl

data class MatchAction(
    val matchId: String = "",
    val actionType: String = "",
    val playerRef: String = "",  // Changed from playerId
    val playerName: String = "",
    val playerNumber: Int = 0,
    val team: String = "",  // Added team property
    val quarter: String = "",
    val gameTime: Long = 0,
    val gameTimeFormatted: String = "",  // Note the double 'f' to match your original
    val timestamp: String = ""
)