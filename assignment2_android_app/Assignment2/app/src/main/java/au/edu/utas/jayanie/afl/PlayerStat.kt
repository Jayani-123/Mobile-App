package au.edu.utas.jayanie.afl

import PlayerStatAdapter
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import au.edu.utas.jayanie.afl.databinding.ActivityPlayerStatBinding
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.graphics.toColorInt

class PlayerStat : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerStatBinding
    private lateinit var db: FirebaseFirestore
    private var currentMatchId: String = ""
    private lateinit var team1Adapter: PlayerStatAdapter
    private lateinit var team2Adapter: PlayerStatAdapter
    private var winnerText: String = ""
    // Team colors and logos
    private var team1Color: String = "#FFFFFF" // Default white
    private var team2Color: String = "#FFFFFF" // Default white
    private var team1Logo: String = ""
    private var team2Logo: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPlayerStatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.apply {
            title = "Player Stat"
            setDisplayHomeAsUpEnabled(true)
        }

        db = FirebaseFirestore.getInstance()
        currentMatchId = intent.getStringExtra("MATCH_ID") ?: ""

        winnerText= intent.getStringExtra("WINNER_TEXT") ?: ""
        if (winnerText.isEmpty()) {
            finish()
            return
        }

        if (currentMatchId.isEmpty()) {
            Toast.makeText(this, "No match ID provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        binding.textMatchCompare.setOnClickListener {
            startActivity(Intent(this, ComparePlayers::class.java).apply {
                putExtra("MATCH_ID", currentMatchId)
                putExtra("WINNER_TEXT", winnerText)
            })
        }
        // Initialize RecyclerViews
        setupRecyclerViews()

        // Load match data including team colors and logos
        loadMatchData()

        // Navigation buttons
        binding.Score.setOnClickListener {
            startActivity(Intent(this, ActionList::class.java).apply {
                putExtra("MATCH_ID", currentMatchId)
                putExtra("WINNER_TEXT", winnerText)
            })
        }

        binding.Action.setOnClickListener {
            startActivity(Intent(this, ActionList::class.java).apply {
                putExtra("MATCH_ID", currentMatchId)
                putExtra("WINNER_TEXT", winnerText)
            })
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerViews() {
        team1Adapter = PlayerStatAdapter()
        team2Adapter = PlayerStatAdapter()

        binding.team1RecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PlayerStat)
            adapter = team1Adapter
        }

        binding.team2RecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PlayerStat)
            adapter = team2Adapter
        }
    }

    private fun loadMatchData() {
        db.collection("matches").document(currentMatchId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Get team names
                    val team1Name = document.getString("team1Name") ?: "Team 1"
                    val team2Name = document.getString("team2Name") ?: "Team 2"

                    // Get team colors and logos
                    team1Color = document.getString("team1color") ?: "#FFFFFF"
                    team2Color = document.getString("team2color") ?: "#FFFFFF"
                    team1Logo = document.getString("team1Logo") ?: ""
                    team2Logo = document.getString("team2Logo") ?: ""

                    // Update UI
                    binding.team1Name.text = team1Name
                    binding.team2Name.text = team2Name

                    // Set card colors
                    binding.cardView.strokeColor = team1Color.toColorInt()
                    binding.cardView2.strokeColor = team2Color.toColorInt()

                    // Load team logos
                    decodeBase64ToImage(team1Logo, binding.team1Logo)
                    decodeBase64ToImage(team2Logo, binding.team2Logo)

                    // Now load players for each team
                    loadTeamPlayers(team1Name, true)
                    loadTeamPlayers(team2Name, false)
                } else {
                    Toast.makeText(this, "Match not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading match data", Toast.LENGTH_SHORT).show()
                Log.e("PlayerStat", "Error fetching match data", e)
            }
    }

    private fun loadTeamPlayers(teamName: String, isTeam1: Boolean) {
        val collection = if (isTeam1) "playersTeam1" else "playersTeam2"

        db.collection("matches").document(currentMatchId)
            .collection(collection)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val players = mutableListOf<PlayerStats>()
                for (document in querySnapshot.documents) {
                    val player = PlayerStats(
                        id = document.id,
                        number = document.getLong("number")?.toInt() ?: 0,
                        name = document.getString("name") ?: "",
                        teamName = teamName

                    )
                    players.add(player)
                }

                // Fetch stats for each player
                players.forEach { player ->
                    fetchPlayerStats(player, isTeam1)
                }
            }
            .addOnFailureListener { e ->
                Log.e("PlayerStat", "Error loading players for $teamName", e)
            }
    }

    private fun fetchPlayerStats(player: PlayerStats, isTeam1: Boolean) {
        db.collection("matches").document(currentMatchId)
            .collection("actions")
            .whereEqualTo("playerNumber", player.number)
            .whereEqualTo("team", player.teamName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val stats = player.copy(
                    goals = querySnapshot.count { it.getString("actionType") == "Goal" },
                    behinds = querySnapshot.count { it.getString("actionType") == "Behind" },
                    kicks = querySnapshot.count { it.getString("actionType") == "Kick" },
                    handballs = querySnapshot.count { it.getString("actionType") == "Handball" },
                    marks = querySnapshot.count { it.getString("actionType") == "Mark" },
                    tackles = querySnapshot.count { it.getString("actionType") == "Tackle" },
                    disposals = querySnapshot.count { it.getString("actionType") == "Disposal" }
                )

                // Update the appropriate adapter
                if (isTeam1) {
                    val currentList = team1Adapter.currentList.toMutableList()
                    currentList.add(stats)
                    team1Adapter.submitList(currentList)
                } else {
                    val currentList = team2Adapter.currentList.toMutableList()
                    currentList.add(stats)
                    team2Adapter.submitList(currentList)
                }
            }
            .addOnFailureListener { e ->
                Log.e("PlayerStat", "Error fetching stats for player ${player.name}", e)
            }
    }

    private fun decodeBase64ToImage(base64: String?, imageView: ImageView) {
        if (!base64.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(base64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                imageView.setImageResource(R.drawable.afl_icon)
                Log.e("PlayerStat", "Error decoding team logo", e)
            }
        } else {
            imageView.setImageResource(R.drawable.afl_icon)
        }
    }
}