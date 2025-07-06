package au.edu.utas.jayanie.afl

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.io.Serializable

data class Match(
    @get:Exclude var id: String? = null,
    var matchName: String = "",
    var venue: String = "",
    var date: String = "",
    var time: String = "",
    var team1Name: String = "",
    var team2Name: String = "",
    var team1color: String = "",
    var team2color: String = "",
    var team1Logo: String = "",
    var team2Logo: String = "",
    var playersTeam1: MutableList<Player> = mutableListOf(),
    var playersTeam2: MutableList<Player> = mutableListOf(),
    @field:ServerTimestamp
    val createdAt: Timestamp? = null

)
    : Serializable