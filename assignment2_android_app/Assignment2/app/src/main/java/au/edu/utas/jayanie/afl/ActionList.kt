package au.edu.utas.jayanie.afl

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import au.edu.utas.jayanie.afl.databinding.ActivityActionListBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ActionList : AppCompatActivity() {
    private lateinit var binding: ActivityActionListBinding
    private var matchId: String = ""
    private var matchName: String = "Match in Progress"
    private var team1Name: String = "Team 1"
    private var team2Name: String = "Team 2"
    private var winnerText: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityActionListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get match details from intent
        matchId = intent.getStringExtra("MATCH_ID") ?: ""
        matchName = intent.getStringExtra("MATCH_NAME") ?: "Match in Progress"
        team1Name = intent.getStringExtra("team1Name") ?: "Team 1"
        team2Name = intent.getStringExtra("team2Name") ?: "Team 2"

        // Set match name
            //binding.textMatch.text = matchName

        // Toolbar setup
        setSupportActionBar(findViewById(R.id.toolbar))

        winnerText= intent.getStringExtra("WINNER_TEXT") ?: ""
        if (winnerText.isEmpty()) {
            finish()
            return
        }

        supportActionBar?.apply {
            title = "Actions"
            setDisplayHomeAsUpEnabled(true)
        }

        // Button click listeners
        binding.Score.setOnClickListener {
            startActivity(Intent(this, Score::class.java).apply {
                putExtra("MATCH_ID", matchId)
                putExtra("WINNER_TEXT", winnerText)
            })
        }

        binding.Player.setOnClickListener {
            startActivity(Intent(this, ComparePlayers::class.java).apply {
                putExtra("MATCH_ID", matchId)
                putExtra("WINNER_TEXT", winnerText)
            })
        }

         // Set up ViewPager and TabLayout
        val adapter = ActionPagerAdapter(
            this,
            matchId,
            matchName,
            team1Name,
            team2Name
        )
        binding.viewPager.adapter = adapter

        // Connect TabLayout with ViewPager
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = "Q${position + 1}"
        }.attach()

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_list_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_share -> {
                shareActions()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareActions() {
        if (matchId.isEmpty()) {
            Toast.makeText(this, "No match selected", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseFirestore.getInstance()
            .collection("matches")
            .document(matchId)
            .collection("actions")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                // Build the share text
                val shareText = buildString {
                    append("$matchName - All Quarters Actions\n\n")
                    append("Quarter\tTime\tTeam\tPlayer\tAction\n")
                    append("----------------------------------------\n")

                    querySnapshot.documents.forEach { doc ->
                        val action = doc.toObject(MatchAction::class.java)
                        action?.let {
                            append("${it.quarter}\t${it.gameTimeFormatted}\t${it.team}\t${it.playerName} (${it.playerNumber})\t${it.actionType}\n")
                        }
                    }
                }

                // Create share intent
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_SUBJECT, "$matchName - Match Actions")
                }

                // Start the share activity
                startActivity(Intent.createChooser(shareIntent, "Share Match Actions"))
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading actions: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    inner class ActionPagerAdapter(
        activity: AppCompatActivity,
        private val matchId: String,
        private val matchName: String,
        private val team1Name: String,
        private val team2Name: String
    ) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return ActionFragment.newInstance(
                quarter = "Quarter ${position + 1}",
                matchId = matchId,
                matchName = matchName,
                team1Name = team1Name,
                team2Name = team2Name
            )
        }
    }
}