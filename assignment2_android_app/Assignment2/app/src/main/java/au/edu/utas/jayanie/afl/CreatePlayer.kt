package au.edu.utas.jayanie.afl
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import android.widget.SearchView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.edu.utas.jayanie.afl.databinding.ActivityCreatePlayerBinding
import au.edu.utas.jayanie.afl.databinding.PlayerListBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class CreatePlayer : AppCompatActivity() {

    private var team1LogoBase64: String? = null
    private var team1Name: String? = null
    private var team1Color: String? = null
    private var matchId: String? = null

    private var team2LogoBase64: String? = null
    private var team2Name: String? = null
    private var team2Color: String? = null

    private var currentTeam: String = "team1"

    private lateinit var ui: ActivityCreatePlayerBinding
    private val playerList = mutableListOf<Player>()
    private val filteredPlayerList = mutableListOf<Player>()
    private lateinit var adapter: PlayerAdapter
    private val db = FirebaseFirestore.getInstance()
    private var team1PlayerCount = 0
    private var team2PlayerCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ui = ActivityCreatePlayerBinding.inflate(layoutInflater)
        setContentView(ui.root)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Players"

        // Retrieve intent data
        matchId = intent.getStringExtra("MATCH_ID")
        team1LogoBase64 = intent.getStringExtra("TEAM1_LOGO")
        team1Name = intent.getStringExtra("TEAM1_NAME")
        team1Color = intent.getStringExtra("TEAM1_COLOR")
        team2LogoBase64 = intent.getStringExtra("TEAM2_LOGO")
        team2Name = intent.getStringExtra("TEAM2_NAME")
        team2Color = intent.getStringExtra("TEAM2_COLOR")

        // Initialize RecyclerView with filtered list
        adapter = PlayerAdapter(filteredPlayerList)
        ui.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@CreatePlayer)
            adapter = this@CreatePlayer.adapter
        }

        setupTeamDisplay()
        setupSearchView()
        setupButtons()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupSearchView() {
        ui.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false

            override fun onQueryTextChange(newText: String?): Boolean {
                filterPlayers(newText.orEmpty())
                return true
            }
        })
    }

    private fun filterPlayers(query: String) {
        filteredPlayerList.clear()

        if (query.isEmpty()) {
            filteredPlayerList.addAll(playerList)
        } else {
            val lowerCaseQuery = query.lowercase(Locale.getDefault())
            playerList.forEach { player ->
                if (player.name.lowercase(Locale.getDefault()).contains(lowerCaseQuery) ||
                    player.number.toString().contains(query) ||
                    player.position.lowercase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    filteredPlayerList.add(player)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun setupTeamDisplay() {
        val teamName = if (currentTeam == "team1") team1Name else team2Name
        val teamLogo = if (currentTeam == "team1") team1LogoBase64 else team2LogoBase64

        ui.team1Name.text = teamName
        teamLogo?.let { logo ->
            try {
                val imageBytes = Base64.decode(logo, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ui.logoTeam1.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error loading team logo", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun updateNextButtonState() {
        val currentTeamCount = if (currentTeam == "team1") team1PlayerCount else team2PlayerCount
        val canProceed = currentTeamCount >= 2

        ui.btnNext.isEnabled = canProceed

        if (!canProceed) {
            val teamName = if (currentTeam == "team1") team1Name else team2Name
            ui.btnNext.text = "Add at least 2 players to $teamName"
        } else {
            ui.btnNext.text = if (currentTeam == "team1") "Continue to $team2Name" else "Continue to Match"
        }
    }
    private fun setupButtons() {
        ui.addPlayerButton.setOnClickListener {
            Intent(this, AddPlayerDetails::class.java).apply {
                putExtra("matchId", matchId)
                putExtra("team1", team1Name)
                putExtra("team2", team2Name)
                startActivity(this)
            }
        }

        ui.btnNext.setOnClickListener {
            val currentTeamCount = if (currentTeam == "team1") team1PlayerCount else team2PlayerCount
            if (currentTeamCount < 2) {
                Toast.makeText(this, "Please add at least 2 players to ${if (currentTeam == "team1") team1Name else team2Name}", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentTeam == "team1") {
                currentTeam = "team2"
                setupTeamDisplay()
                loadPlayers()
            } else {
                // Check if team2 also has at least 2 players
                if (team2PlayerCount >= 2) {
                    Intent(this, MatchDetails::class.java).apply {
                        putExtra("matchId", matchId)
                        startActivity(this)
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Please add at least 2 players to $team2Name", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadPlayers()
        updateNextButtonState() // Ensure button state is updated when returning from AddPlayer
    }

    private fun loadPlayers() {
        if (matchId == null) {
            Log.e("CreatePlayer", "matchId is null")
            return
        }

        val matchRef = db.collection("matches").document(matchId!!)
        val playersCollection = "players${currentTeam.replaceFirstChar { it.uppercase() }}"

        matchRef.collection(playersCollection).get()
            .addOnSuccessListener { querySnapshot ->
                playerList.clear()
                querySnapshot.documents.forEach { document ->
                    document.toObject(Player::class.java)?.let { playerList.add(it) }
                }
                // Update the count for the current team
                if (currentTeam == "team1") {
                    team1PlayerCount = playerList.size
                } else {
                    team2PlayerCount = playerList.size
                }

                // Update filtered list with current search query
                filterPlayers(ui.searchView.query.toString())
                updateNextButtonState() // Update button state after loading
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading players: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    inner class PlayerHolder(private val binding: PlayerListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(player: Player) {
            binding.playerNumber.text = player.number.toString()
            binding.playerName.text = player.name
            binding.playerPosition.text = player.position

            binding.editButton.setOnClickListener {
                Intent(this@CreatePlayer, AddPlayerDetails::class.java).apply {
                    putExtra("matchId", matchId)
                    putExtra("team1", team1Name)
                    putExtra("team2", team2Name)
                    putExtra("team", currentTeam)
                    putExtra("playerId", player.id)
                    putExtra("name", player.name)
                    putExtra("position", player.position)
                    putExtra("number", player.number)
                    putExtra("age", player.age)
                    putExtra("height", player.height)
                    putExtra("image", player.image)
                    putExtra("teamName", player.teamName)
                    startActivity(this)
                }
            }

            binding.deleteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val playerToDelete = filteredPlayerList[position]
                    deletePlayer(playerToDelete)
                }
            }
        }
    }

    private fun deletePlayer(player: Player) {
        val matchRef = db.collection("matches").document(matchId!!)
        val playersCollection = "players${currentTeam.replaceFirstChar { it.uppercase() }}"

        db.runBatch { batch ->
            batch.delete(matchRef.collection(playersCollection).document(player.id))
            batch.update(matchRef, "${currentTeam}Players", FieldValue.arrayRemove(player.id))
        }.addOnSuccessListener {
            // Remove from both lists
            playerList.removeAll { it.id == player.id }
            filteredPlayerList.removeAll { it.id == player.id }
            adapter.notifyDataSetChanged()
            // Update the count for the current team
            if (currentTeam == "team1") {
                team1PlayerCount--
            } else {
                team2PlayerCount--
            }

            updateNextButtonState()
            Toast.makeText(this, "Player removed", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    inner class PlayerAdapter(private val players: MutableList<Player>) : RecyclerView.Adapter<PlayerHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerHolder {
            val binding = PlayerListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return PlayerHolder(binding)
        }

        override fun onBindViewHolder(holder: PlayerHolder, position: Int) {
            holder.bind(players[position])
        }

        override fun getItemCount() = players.size
    }
}