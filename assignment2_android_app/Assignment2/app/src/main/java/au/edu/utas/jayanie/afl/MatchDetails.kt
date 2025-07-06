package au.edu.utas.jayanie.afl

import android.content.Intent
import android.os.Bundle
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.edu.utas.jayanie.afl.databinding.ActivityMatchDetailsBinding
import com.google.firebase.firestore.FirebaseFirestore

class MatchDetails : AppCompatActivity() {
    private lateinit var ui: ActivityMatchDetailsBinding
    private lateinit var team1Adapter: PlayerAdapter
    private lateinit var team2Adapter: PlayerAdapter
    private val db = FirebaseFirestore.getInstance()
    private var matchName: String = ""
    private var team1Name: String = ""
    private var team2Name: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize view binding
        ui = ActivityMatchDetailsBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // Toolbar setup
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Match Details"

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize RecyclerViews
        setupRecyclerViews()

        // Fetch match details
        val matchId = intent.getStringExtra("matchId") ?: return
        fetchMatchDetails(matchId)

        ui.btnYes.setOnClickListener {
            // Validate data
            if (matchId.isNullOrEmpty()) {
                Toast.makeText(this, "Match ID missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create and launch intent
            Intent(this, RecordMatch::class.java).apply {
                putExtra("MATCH_ID", matchId)
                putExtra("MATCH_NAME", matchName)
                putExtra("team1Name", team1Name)
                putExtra("team2Name", team2Name)


            }.let { intent ->
                startActivity(intent)

            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(ui.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerViews() {
        team1Adapter = PlayerAdapter()
        team2Adapter = PlayerAdapter()

        ui.team1RecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MatchDetails)
            adapter = team1Adapter
            setHasFixedSize(true)
        }

        ui.team2RecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MatchDetails)
            adapter = team2Adapter
            setHasFixedSize(true)
        }
    }

    private fun fetchMatchDetails(matchId: String) {
        // Fetch match document
        db.collection("matches").document(matchId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    matchName = document.getString("matchName") ?: ""
                    // Update match details
                    document.getString("matchName")?.let { ui.matchName.setText(it) }
                    document.getString("date")?.let { ui.editTextDate.setText(it) }
                    document.getString("time")?.let { ui.matchTime.setText(it) }
                    document.getString("venue")?.let { ui.venue.setText(it) }
                    team1Name = document.getString("team1Name") ?: ""
                    team2Name= document.getString("team2Name") ?: ""
                    // Update team names
                    document.getString("team1Name")?.let { ui.team1TextView.text = it }
                    document.getString("team2Name")?.let { ui.team2TextView.text = it }

                    // Update team colors
                    document.getString("team1color")?.let { colorHex ->
                        try {
                            val color = Color.parseColor(colorHex)
                            ui.team1CardView.setCardBackgroundColor(color)
                            ui.team1TextView.setTextColor(getContrastColor(color))
                        } catch (e: Exception) { /* ... */ }
                    }
                    document.getString("team2color")?.let { colorHex ->
                        try {
                            val color = Color.parseColor(colorHex)
                            ui.team2CardView.setCardBackgroundColor(color)
                            ui.team2TextView.setTextColor(getContrastColor(color))
                        } catch (e: Exception) { /* ... */ }
                    }
                    // Update team Logo
                    // Update team logos (corrected implementation)
                    document.getString("team1Logo")?.let { base64String ->
                        try {
                            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            ui.imageView1.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            Log.e("MatchDetails", "Error loading team1 logo", e)

                        }
                    }

                    document.getString("team2Logo")?.let { base64String ->
                        try {
                            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            ui.imageView2.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            Log.e("MatchDetails", "Error loading team2 logo", e)

                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("MatchDetails", "Error loading match details", e)
                Toast.makeText(this, "Failed to load match details", Toast.LENGTH_SHORT).show()
            }

        // Fetch team 1 players
        db.collection("matches").document(matchId).collection("playersTeam1")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val players = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        Player(
                            id = doc.id,
                            number = doc.getLong("number")?.toInt() ?: 0,
                            name = doc.getString("name") ?: "",
                            position = doc.getString("position") ?: "",
                            age = doc.getLong("age")?.toInt() ?: 0,
                            image = doc.getString("image") ?: "",
                            height = doc.getLong("height") ?.toInt() ?: 0,

                        )
                    } catch (e: Exception) {
                        Log.e("MatchDetails", "Error parsing player data", e)
                        null
                    }
                }
                team1Adapter.submitList(players)
            }
            .addOnFailureListener { e ->
                Log.e("MatchDetails", "Error loading team 1 players", e)
            }

        // Fetch team 2 players
        db.collection("matches").document(matchId).collection("playersTeam2")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val players = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        Player(
                            id = doc.id,
                            number = doc.getLong("number")?.toInt() ?: 0,
                            name = doc.getString("name") ?: "",
                            position = doc.getString("position") ?: "",
                            age = doc.getLong("age")?.toInt() ?: 0,
                            image = doc.getString("image") ?: "",
                            height = doc.getLong("height") ?.toInt() ?: 0,

                        )
                    } catch (e: Exception) {
                        Log.e("MatchDetails", "Error parsing player data", e)
                        null
                    }
                }
                team2Adapter.submitList(players)
            }
            .addOnFailureListener { e ->
                Log.e("MatchDetails", "Error loading team 2 players", e)
            }

    }

    class PlayerAdapter : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {
        private val players = mutableListOf<Player>()

        fun submitList(newPlayers: List<Player>) {
            players.clear()
            players.addAll(newPlayers)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.team_players, parent, false)
            return PlayerViewHolder(view)
        }

        override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
            val player = players[position]
            holder.playerNumber.text = player.number.toString()
            holder.playerName.text = player.name
            // Add other fields if needed
        }

        override fun getItemCount(): Int = players.size

        class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val playerNumber: TextView = itemView.findViewById(R.id.playerNumber)
            val playerName: TextView = itemView.findViewById(R.id.playerName)
            // Add other views if needed
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    // Contrast color calculation function
// Contrast color calculation function
    private fun getContrastColor(color: Int): Int {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return if (darkness < 0.5) Color.BLACK else Color.WHITE
    }
}