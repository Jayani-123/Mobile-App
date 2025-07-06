package au.edu.utas.jayanie.afl

data class PlayerStats(
    val id: String = "",
    val number: Int = 0,
    val name: String = "",
    val teamName: String = "",
    val goals: Int = 0,
    val behinds: Int = 0,
    val kicks: Int = 0,
    val handballs: Int = 0,
    val marks: Int = 0,
    val tackles: Int = 0,
    val disposals: Int = 0
)