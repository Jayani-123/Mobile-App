package au.edu.utas.jayanie.afl

data class QuarterScores(
    val goals: Int = 0,
    val behinds: Int = 0,
    val kicks: Int = 0,
    val tackels: Int = 0,
    val handballs: Int = 0,
    val marks: Int = 0,


) {
    // Calculated property
    val total: Int
        get() = goals * 6 + behinds

    // Calculated property
    val disposals: Int
        get() = kicks + handballs

    // Optional: Formatted string output (e.g., "3.2.20")
    fun formattedString(): String {
        return "$goals.$behinds.$total"
    }

    fun formattedStringDisposal(): String {
        return "($kicks + $handballs) $disposals"
    }

    fun formattedStringTotalScoreAllQuarters(): String {
        return "$goals.$behinds.$total"
    }
    fun formattedStringMark(): String {
        return "$marks"
    }
    fun formattedStringTackle(): String {
        return "$tackels"
    }
    fun totalPoints(): Int {
        return (this.goals * 6) + (this.behinds * 1)
    }
}