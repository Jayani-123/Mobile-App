package au.edu.utas.jayanie.afl

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import au.edu.utas.jayanie.afl.databinding.ActivityComparePlayersBinding
import com.google.firebase.firestore.FirebaseFirestore

class ComparePlayers : AppCompatActivity(), PlayerListDialogFragment.OnPlayerSelectedListener {

    private lateinit var binding: ActivityComparePlayersBinding
    private lateinit var db: FirebaseFirestore
    private var currentMatchId: String = ""
    private var currentSelectedButton: Int = 1 // 1 for addBtn1, 2 for addBtn2
    private var winnerText: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()



        binding = ActivityComparePlayersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Toolbar setup
        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.apply {
            title = "Player Compare"
            setDisplayHomeAsUpEnabled(true)
        }
        db = FirebaseFirestore.getInstance()


        currentMatchId = intent.getStringExtra("MATCH_ID") ?: ""
        if (currentMatchId.isEmpty()) {
            Toast.makeText(this, "No match ID provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        winnerText= intent.getStringExtra("WINNER_TEXT") ?: ""
        if (winnerText.isEmpty()) {
            finish()
            return
        }
        binding.addBtn1.setOnClickListener {
            showPlayerSelectionDialog(1)
        }

        binding.addBtn2.setOnClickListener {
            showPlayerSelectionDialog(2)
        }

        binding.Score.setOnClickListener {
            startActivity(Intent(this, Score::class.java).apply {
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
        binding.btnStat.setOnClickListener {
            startActivity(Intent(this, PlayerStat::class.java).apply {
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

    private fun showPlayerSelectionDialog(buttonId: Int) {
        if (currentMatchId.isEmpty()) {
            Toast.makeText(this, "No match selected", Toast.LENGTH_SHORT).show()
            return
        }
        currentSelectedButton = buttonId
        val dialog = PlayerListDialogFragment.newInstance(currentMatchId)
        dialog.setOnPlayerSelectedListener(this)
        dialog.show(supportFragmentManager, "PlayerListDialog")
    }

    override fun onPlayerSelected(player: Player) {
        if (player.id.isEmpty()) {
            Toast.makeText(this, "Player data incomplete", Toast.LENGTH_SHORT).show()
            return
        }

        // Determine which collection to query based on ID
        val collection = if (player.id.contains("team1")) "playersTeam1" else "playersTeam2"

        db.collection("matches").document(currentMatchId)
            .collection(collection)
            .document(player.id)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val fullPlayer = Player(
                        id = document.id,
                        number = document.getLong("number")?.toInt() ?: 0,
                        name = document.getString("name") ?: "",
                        teamName = document.getString("teamName") ?: "",
                        position = document.getString("position") ?: "",
                        age = document.getLong("age")?.toInt() ?: 0,
                        height = document.getLong("height")?.toInt() ?: 0,
                        image = document.getString("image") ?: ""
                    )
                    if (currentSelectedButton == 1) {
                        // Update UI with player data
                        binding.player1Name.text = "${fullPlayer.number} - ${fullPlayer.name}"
                        binding.TeamName1.text = fullPlayer.teamName
                        binding.txtPlayer1Position.text = fullPlayer.position
                        binding.txtPlayer1Age.text = fullPlayer.age.toString()
                        binding.txtPlayer1Height.text = fullPlayer.height.toString()
                        decodeBase64ToImage(fullPlayer.image, binding.imgPlayer1)
                        fetchPlayerActions(fullPlayer, true)

                    }else {
                        // Update Player 2 UI
                        binding.player2Name.text = "${fullPlayer.number} - ${fullPlayer.name}"
                        binding.teamName2.text = fullPlayer.teamName
                        binding.txtPlayer2Position.text = fullPlayer.position
                        binding.txtPlayer2Age.text = fullPlayer.age.toString()
                        binding.txtPlayer2Height.text = fullPlayer.height.toString()
                        decodeBase64ToImage(fullPlayer.image, binding.imgPlayer2)
                        fetchPlayerActions(fullPlayer, false)
                    }

                } else {
                    Toast.makeText(this, "Player not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading player", Toast.LENGTH_SHORT).show()
                Log.e("ComparePlayers", "Error fetching player", e)
            }
    }

    private fun fetchPlayerActions(player: Player,isPlayer1: Boolean) {
        db.collection("matches").document(currentMatchId)
            .collection("actions")
            .whereEqualTo("playerNumber", player.number)
            .whereEqualTo("team", player.teamName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                var goals = 0
                var behinds = 0
                var disposals = 0
                var tackles = 0
                var marks = 0
                var kicks = 0

                for (document in querySnapshot.documents) {
                    when (document.getString("actionType")) {
                        "Goal" -> goals++
                        "Behind" -> behinds++
                        "Disposal" -> disposals++
                        "Tackle" -> tackles++
                        "Mark" -> marks++
                        "Kick" -> kicks++
                    }
                }

                // Update the correct player's UI
                if (isPlayer1) {
                    binding.txtPlayer1Goals.text = goals.toString()
                    binding.txtPlayer1Behinds.text = behinds.toString()
                    binding.txtPlayer1Disposals.text = disposals.toString()
                    binding.txtPlayer1Tackles.text = tackles.toString()
                    binding.txtPlayer1Marks.text = marks.toString()
                    binding.txtPlayer1Kicks.text = kicks.toString()
                } else {
                    binding.txtPlayer2Goals.text = goals.toString()
                    binding.txtPlayer2Behinds.text = behinds.toString()
                    binding.txtPlayer2Disposals.text = disposals.toString()
                    binding.txtPlayer2Tackles.text = tackles.toString()
                    binding.txtPlayer2Marks.text = marks.toString()
                    binding.txtPlayer2Kicks.text = kicks.toString()
                }

                // Compare and highlight after both players are loaded
                if (binding.txtPlayer1Goals.text.isNotEmpty() &&
                    binding.txtPlayer2Goals.text.isNotEmpty()) {
                    compareAndHighlightStats()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading player actions", Toast.LENGTH_SHORT).show()
                Log.e("ComparePlayers", "Error fetching actions", e)
            }
    }

    private fun compareAndHighlightStats() {
        // Compare goals
        highlightIfGreater(
            binding.txtPlayer1Goals,
            binding.txtPlayer2Goals,
            binding.txtPlayer1Goals.text.toString().toIntOrNull() ?: 0,
            binding.txtPlayer2Goals.text.toString().toIntOrNull() ?: 0
        )

        // Compare behinds
        highlightIfGreater(
            binding.txtPlayer1Behinds,
            binding.txtPlayer2Behinds,
            binding.txtPlayer1Behinds.text.toString().toIntOrNull() ?: 0,
            binding.txtPlayer2Behinds.text.toString().toIntOrNull() ?: 0
        )

        // Compare disposals
        highlightIfGreater(
            binding.txtPlayer1Disposals,
            binding.txtPlayer2Disposals,
            binding.txtPlayer1Disposals.text.toString().toIntOrNull() ?: 0,
            binding.txtPlayer2Disposals.text.toString().toIntOrNull() ?: 0
        )

        // Compare tackles
        highlightIfGreater(
            binding.txtPlayer1Tackles,
            binding.txtPlayer2Tackles,
            binding.txtPlayer1Tackles.text.toString().toIntOrNull() ?: 0,
            binding.txtPlayer2Tackles.text.toString().toIntOrNull() ?: 0
        )

        // Compare marks
        highlightIfGreater(
            binding.txtPlayer1Marks,
            binding.txtPlayer2Marks,
            binding.txtPlayer1Marks.text.toString().toIntOrNull() ?: 0,
            binding.txtPlayer2Marks.text.toString().toIntOrNull() ?: 0
        )

        // Compare marks
        highlightIfGreater(
            binding.txtPlayer1Kicks,
            binding.txtPlayer2Kicks,
            binding.txtPlayer1Kicks.text.toString().toIntOrNull() ?: 0,
            binding.txtPlayer2Kicks.text.toString().toIntOrNull() ?: 0
        )
    }

    private fun highlightIfGreater(
        player1View: TextView,
        player2View: TextView,
        player1Value: Int,
        player2Value: Int
    ) {
        // Reset backgrounds first
        player1View.setBackgroundColor(Color.TRANSPARENT)
        player2View.setBackgroundColor(Color.TRANSPARENT)

        // Highlight the greater value
        when {
            player1Value > player2Value -> {
                player1View.setBackgroundColor(Color.YELLOW)

            }
            player2Value > player1Value -> {
                player2View.setBackgroundColor(Color.YELLOW)

            }
            // Equal values - no highlight
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
            }
        } else {
            imageView.setImageResource(R.drawable.afl_icon)
        }
    }
    }
