package au.edu.utas.jayanie.afl

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import au.edu.utas.jayanie.afl.databinding.ActivityRecordMatchBinding

class RecordMatch : AppCompatActivity() {
    private lateinit var binding: ActivityRecordMatchBinding
    private var winnerText: String = "Leading"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize ViewBinding
        binding = ActivityRecordMatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Toolbar setup
        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.apply {
            title = "Record Match"
            setDisplayHomeAsUpEnabled(true)
        }

        // Get match details from intent
        val matchId = intent.getStringExtra("MATCH_ID") ?: ""
        val matchName = intent.getStringExtra("MATCH_NAME") ?: "Match in Progress"


        binding.textMatch.text = matchName


        // Setup ViewPager with match details
        val adapter = QuartersPagerAdapter(
            supportFragmentManager,
            matchId,
            matchName   )

        // Set the adapter for the ViewPager to manage page content
        binding.viewPager.adapter = adapter

        // Link the TabLayout with the ViewPager so that tab selections and page swipes are synchronized
        binding.tablayout.setupWithViewPager(binding.viewPager)

        // Action button click listener
        binding.Action.setOnClickListener {
            Intent(this@RecordMatch, ActionList::class.java).apply {
                putExtra("MATCH_ID", matchId)
                putExtra("WINNER_TEXT", winnerText)
            }.also { startActivity(it) }
        }

        // Score button click listener
        binding.Score.setOnClickListener {
            Intent(this@RecordMatch, Score::class.java).apply {
                putExtra("MATCH_ID", matchId)
                putExtra("WINNER_TEXT", winnerText)
            }.also { startActivity(it) }
        }

        // Player button click listener
        binding.Player.setOnClickListener {
            Intent(this@RecordMatch, ComparePlayers::class.java).apply {
                putExtra("MATCH_ID", matchId)
                putExtra("WINNER_TEXT", winnerText)
            }.also { startActivity(it) }
        }
    }

    inner class QuartersPagerAdapter(
        fm: FragmentManager,
        private val matchId: String,
        private val matchName: String,

    ) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getCount(): Int = 4 // Four quarters

        override fun getItem(position: Int): Fragment {
            return QuarterFragment.newInstance(
                quarter = "Quarter ${position + 1}",
                matchId = matchId,
                matchName = matchName
            )
        }

        override fun getPageTitle(position: Int): CharSequence {
            return "Q${position + 1}"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}