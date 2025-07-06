package au.edu.utas.jayanie.afl

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import au.edu.utas.jayanie.afl.databinding.ActivityMainBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {
    private lateinit var ui: ActivityMainBinding
    private var matchId: String? = null // Made nullable to handle cases where no match exists
    private var matchName: String = ""
    private var team1Name: String = ""
    private var team2Name: String = ""
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        toolbar.setNavigationOnClickListener {
            Toast.makeText(applicationContext, "Navigation icon clicked", Toast.LENGTH_SHORT).show()
        }

        fetchLastMatch()

        ui.btnCreate.setOnClickListener {
            startActivity(Intent(this, CreateMatch::class.java))
        }


        ui.btnRecord.setOnClickListener {
            if (matchId != null) {
                Intent(this, RecordMatch::class.java).apply {
                    putExtra("MATCH_ID", matchId)
                    putExtra("MATCH_NAME", matchName)
                    putExtra("team1Name", team1Name)
                    putExtra("team2Name", team2Name)
                    startActivity(this)
                }
            } else {
                Toast.makeText(this, "No match available to record", Toast.LENGTH_SHORT).show()
            }
        }
        ui.btnHistory.setOnClickListener {
            startActivity(Intent(this, History::class.java))
        }

        ui.btnTeamrank.setOnClickListener {
            startActivity(Intent(this, TeamRank::class.java))
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.Item1 -> {
                Toast.makeText(this, "Item 1 clicked", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.Item2 -> {
                Toast.makeText(this, "Item 2 clicked", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.Item3 -> {
                Toast.makeText(this, "Item 3 clicked", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun fetchLastMatch() {
        db.collection("matches")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    matchId = document.id
                    matchName = document.getString("matchName") ?: "Unnamed Match"
                    team1Name = document.getString("team1Name") ?: "Team 1"
                    team2Name = document.getString("team2Name") ?: "Team 2"


                } else {
                    Log.d("FetchLastMatch", "No matches found")
                   Toast.makeText(this, "No matches found. Create one first!", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("FetchLastMatch", "Error fetching last match", e)
                Toast.makeText(this, "Failed to load match. Check connection.", Toast.LENGTH_LONG).show()
            }
    }
}