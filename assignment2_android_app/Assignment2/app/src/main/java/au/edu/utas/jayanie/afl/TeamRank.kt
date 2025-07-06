package au.edu.utas.jayanie.afl

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.FirebaseFirestore

class TeamRank : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team_rank)
        // Toolbar setup
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Team Rank"

        recyclerView = findViewById(R.id.teamRankRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        loadTeamData()
    }

    private fun loadTeamData() {
        db.collectionGroup("actions")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val teams = mutableListOf<Team>()

                // First pass: Extract all unique team names
                val teamNames = querySnapshot.documents
                    .mapNotNull { it.getString("team") }
                    .distinct()

                // Second pass: Calculate stats for each team
                teamNames.forEach { teamName ->
                    val teamActions = querySnapshot.documents
                        .filter { it.getString("team") == teamName }

                    val goals = teamActions.count {
                        it.getString("actionType")?.equals("goal", ignoreCase = true) == true
                    }

                    val behinds = teamActions.count {
                        it.getString("actionType")?.equals("behind", ignoreCase = true) == true
                    }

                    teams.add(Team(
                        name = teamName,
                        goals = goals,
                        behinds = behinds,
                        score = (goals * 6) + behinds,

                    ))
                }

                // Sort and display
                val sortedTeams = teams.sortedByDescending { it.score }
                recyclerView.adapter = TeamRankAdapter(sortedTeams)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error loading data", e)
                // Show error message to user
            }
    }



    inner class TeamRankAdapter(private val teams: List<Team>) :
        RecyclerView.Adapter<TeamRankAdapter.TeamViewHolder>() {

        inner class TeamViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val rank: TextView = view.findViewById(R.id.rankText)
            val name: TextView = view.findViewById(R.id.teamNameText)
            val score : TextView = view.findViewById(R.id.ScoreText)

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_team_rank, parent, false)
            return TeamViewHolder(view)
        }

        override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
            val team = teams[position]
            holder.rank.text = (position + 1).toString()
            holder.name.text = team.name
            holder.score .text = team.score .toString()

        }

        override fun getItemCount() = teams.size
    }
}

data class Team(
    val name: String,
    val goals: Int,
    val behinds: Int,
    val score: Int

)