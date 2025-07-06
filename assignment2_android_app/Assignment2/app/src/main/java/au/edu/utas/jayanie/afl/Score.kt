package au.edu.utas.jayanie.afl

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import au.edu.utas.jayanie.afl.databinding.ActivityScoreBinding
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.graphics.toColorInt
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

class Score : AppCompatActivity() {

    private lateinit var binding: ActivityScoreBinding
    private lateinit var db: FirebaseFirestore
    private var currentMatchId: String = ""
    private var team1Name: String = ""
    private var team2Name: String = ""
    private var winnerText: String = ""
    private var team1LogoBase64: String? = null
    private var team2LogoBase64: String? = null
    private var team1TotalPoints = 0
    private var team2TotalPoints = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()
        // Toolbar setup
        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.apply {
            title = "Scoreboard"
            setDisplayHomeAsUpEnabled(true)
        }
        // Get winner text  from intent
        winnerText= intent.getStringExtra("WINNER_TEXT") ?: ""
        if (winnerText.isEmpty()) {
            finish()
            return
        }
        // Get match ID from intent
        currentMatchId = intent.getStringExtra("MATCH_ID") ?: ""
        if (currentMatchId.isEmpty()) {
            finish()
            return
        }

        // Load match data
        loadMatchData()

        // Button click listeners
        binding.Action.setOnClickListener {
            startActivity(Intent(this, ActionList::class.java).apply {
                putExtra("MATCH_ID", currentMatchId)
                putExtra("WINNER_TEXT", winnerText)
            })
        }

        binding.Score.setOnClickListener {
            loadMatchData() // Refresh data
        }
        // Action button click listener - FIXED context usage
        binding.Player.setOnClickListener {
            Intent(this, ComparePlayers::class.java).apply {
                putExtra("MATCH_ID", currentMatchId)
                putExtra("WINNER_TEXT", winnerText)

            }.also { startActivity(it) }
        }


    }

    private fun loadMatchData() {
        db.collection("matches")
            .document(currentMatchId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Set team colors
                    val team1Color = document.getString("team1color") ?: "#FF0000"
                    val team2Color = document.getString("team2color") ?: "#0000FF"
                    team1LogoBase64 = document.getString("team1Logo") ?: ""
                    team2LogoBase64 = document.getString("team2Logo") ?: ""
                    binding.cardTeam1.strokeColor = team1Color.toColorInt()
                    binding.cardTeam2.strokeColor = team2Color.toColorInt()

                    // Set team names
                    team1Name = document.getString("team1Name") ?: "Team 1"
                    team2Name = document.getString("team2Name") ?: "Team 2"
                    binding.textScore.text = "$team1Name vs $team2Name"
                    binding.team1.text = team1Name
                    binding.team2.text = team2Name

                    // Load team logos
                    loadTeamLogo(team1LogoBase64, binding.logoTeam1, "team1")
                    loadTeamLogo(team2LogoBase64, binding.logoTeam2, "team2")

                    // Load scores and statistics for both teams
                    loadTeamStatistics(team1Name, true)
                    loadTeamStatistics(team2Name, false)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading match data", Toast.LENGTH_SHORT).show()
                Log.e("ScoreActivity", "Error loading match data", e)
            }
    }

    private fun loadTeamLogo(logoBase64: String?, imageView: android.widget.ImageView, team: String) {
        logoBase64?.let { logo ->
            try {
                val imageBytes = Base64.decode(logo, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading $team logo", Toast.LENGTH_SHORT).show()
                Log.e("ScoreActivity", "Error loading $team logo", e)
            }
        }
    }

    private fun loadTeamStatistics(teamName: String, isTeam1: Boolean) {
        // Load all action types in parallel
        val goalsQuery = db.collection("matches")
            .document(currentMatchId)
            .collection("actions")
            .whereEqualTo("team", teamName)
            .whereEqualTo("actionType", "Goal")
            .get()

        val behindsQuery = db.collection("matches")
            .document(currentMatchId)
            .collection("actions")
            .whereEqualTo("team", teamName)
            .whereEqualTo("actionType", "Behind")
            .get()

        val kicksQuery = db.collection("matches")
            .document(currentMatchId)
            .collection("actions")
            .whereEqualTo("team", teamName)
            .whereEqualTo("actionType", "Kick")
            .get()

        val handballsQuery = db.collection("matches")
            .document(currentMatchId)
            .collection("actions")
            .whereEqualTo("team", teamName)
            .whereEqualTo("actionType", "Handball")
            .get()

        val marksQuery = db.collection("matches")
            .document(currentMatchId)
            .collection("actions")
            .whereEqualTo("team", teamName)
            .whereEqualTo("actionType", "Mark")
            .get()

        val tacklesQuery = db.collection("matches")
            .document(currentMatchId)
            .collection("actions")
            .whereEqualTo("team", teamName)
            .whereEqualTo("actionType", "Tackle")
            .get()

        Tasks.whenAllSuccess<List<MatchAction>>(
            goalsQuery,
            behindsQuery,
            kicksQuery,
            handballsQuery,
            marksQuery,
            tacklesQuery
        ).addOnSuccessListener { results ->
            try {
                val goals = (results[0] as QuerySnapshot).documents
                val behinds = (results[1] as QuerySnapshot).documents
                val kicks = (results[2] as QuerySnapshot).documents
                val handballs = (results[3] as QuerySnapshot).documents
                val marks = (results[4] as QuerySnapshot).documents
                val tackles = (results[5] as QuerySnapshot).documents

                // Calculate all statistics
                val scores = calculateQuarterlyScores(goals, behinds)
                val totalScores = calculateTotalScores(goals, behinds)
                val totalDisposals = calculateTotalDisposals(kicks, handballs)
                val totalMarks = calculateTotalMarks(marks)
                val totalTackles = calculateTotalTackle(tackles)
                // Calculate total points

                // Update UI
                updateTeamUI(
                    isTeam1,
                    scores,
                    totalScores,
                    totalDisposals,
                    totalMarks,
                    totalTackles
                )
                Log.d("FirestoreData", "Goals size: ${goals.size}, Behinds size: ${behinds.size}")
                // Update leading team text if this is the second team

            } catch (e: Exception) {

                Log.e("ScoreActivity", "Error processing team statistics", e)
                Toast.makeText(this, "Error processing statistics", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Log.e("ScoreActivity", "Error loading team statistics", e)
            Toast.makeText(this, "Error loading statistics", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTeamUI(
        isTeam1: Boolean,
        scores: Map<String, QuarterScores>,
        totalScores: QuarterScores,
        totalDisposals: QuarterScores,
        totalMarks: QuarterScores,
        totalTackles: QuarterScores
    ) {
        if (isTeam1) {
            binding.team1Q1.text = scores["Quarter 1"]?.formattedString() ?: "0.0.0"
            binding.team1Q2.text = scores["Quarter 2"]?.formattedString() ?: "0.0.0"
            binding.team1Q3.text = scores["Quarter 3"]?.formattedString() ?: "0.0.0"
            binding.team1Q4.text = scores["Quarter 4"]?.formattedString() ?: "0.0.0"
            binding.team1Total.text = totalScores.formattedStringTotalScoreAllQuarters()
            binding.team1Disposal.text = totalDisposals.formattedStringDisposal()
            binding.team1Mark.text = totalMarks.formattedStringMark()
            binding.team1Tackle.text = totalTackles.formattedStringTackle()

            // Store team1's total points
            team1TotalPoints = totalScores.totalPoints()
        } else {
            binding.team2Q1.text = scores["Quarter 1"]?.formattedString() ?: "0.0.0"
            binding.team2Q2.text = scores["Quarter 2"]?.formattedString() ?: "0.0.0"
            binding.team2Q3.text = scores["Quarter 3"]?.formattedString() ?: "0.0.0"
            binding.team2Q4.text = scores["Quarter 4"]?.formattedString() ?: "0.0.0"
            binding.team2Total.text = totalScores.formattedStringTotalScoreAllQuarters()
            binding.team2Disposal.text = totalDisposals.formattedStringDisposal()
            binding.team2Mark.text = totalMarks.formattedStringMark()
            binding.team2Tackle.text = totalTackles.formattedStringTackle()
            // Store team2's total points
            team2TotalPoints = totalScores.totalPoints()
        }
        updateLeadingTeamText()
        }



    private fun updateLeadingTeamText() {
        val leadingText = when {
            team1TotalPoints > team2TotalPoints -> "$team1Name is $winnerText"
            team2TotalPoints > team1TotalPoints -> "$team2Name is $winnerText"
            else -> "Scores are tied"
        }
        binding.textScore.text = leadingText
    }
    private fun calculateQuarterlyScores(goals: List<DocumentSnapshot>, behinds: List<DocumentSnapshot>): Map<String, QuarterScores> {
        val quarters = listOf("Quarter 1", "Quarter 2", "Quarter 3", "Quarter 4")
        return quarters.associateWith { quarter ->
            QuarterScores(
                goals = goals.count { it.getString("quarter") == quarter },
                behinds = behinds.count { it.getString("quarter") == quarter }
            )
        }
    }

    fun calculateTotalScores(goals: List<DocumentSnapshot>, behinds: List<DocumentSnapshot>): QuarterScores {
        return QuarterScores(goals = goals.size, behinds = behinds.size)
    }

    private fun calculateTotalDisposals(kicks:List<DocumentSnapshot>, handballs:List<DocumentSnapshot>): QuarterScores {
        return QuarterScores(
            kicks = kicks.size,
            handballs = handballs.size
        )
    }

    private fun calculateTotalMarks(marks: List<DocumentSnapshot>): QuarterScores {
        return QuarterScores(
            marks = marks.size
        )
    }

    private fun calculateTotalTackle(tackles: List<DocumentSnapshot>): QuarterScores {
        return QuarterScores(
            tackels = tackles.size
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        return true
    }
}

