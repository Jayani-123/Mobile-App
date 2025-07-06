package au.edu.utas.jayanie.afl

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.edu.utas.jayanie.afl.databinding.ActivityHistoryBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class History : AppCompatActivity() {
    private lateinit var ui: ActivityHistoryBinding
    private val db = FirebaseFirestore.getInstance()
    private val matches = mutableListOf<Match>()
    private lateinit var searchView: SearchView
    private var fullMatchList: MutableList<Match> = mutableListOf()
    private var winnerText: String = "Winner"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ui = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(ui.root)
        // Toolbar setup
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "History"

        ui.recyclerViewMatches.layoutManager = LinearLayoutManager(this)
        ui.recyclerViewMatches.adapter = MatchAdapter(matches)

        fetchMatches()

        searchView = ui.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterMatches(newText.orEmpty())
                return true
            }
        })


    }

    private fun fetchMatches() {
        db.collection("matches")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                matches.clear()
                for (doc in snapshot.documents) {
                    val match = Match(
                        id = doc.id,
                        createdAt = doc.getTimestamp("createdAt"),
                        date = doc.getString("date") ?: "",
                        matchName = doc.getString("matchName") ?: "",
                        venue = doc.getString("venue") ?: "",
                        team1Logo = doc.getString("team1Logo").toString(),
                        team2Logo = doc.getString("team2Logo").toString()

                    )
                    fullMatchList.add(match)

                }
                matches.addAll(fullMatchList)
                ui.recyclerViewMatches.adapter?.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Log.e("MatchListActivity", "Error fetching matches", it)
            }
    }

    private fun filterMatches(query: String) {
        val filteredList = fullMatchList.filter {
            it.matchName.contains(query, ignoreCase = true)
        }
        matches.clear()
        matches.addAll(filteredList)
        ui.recyclerViewMatches.adapter?.notifyDataSetChanged()
    }

    inner class MatchAdapter(private val matches: List<Match>) :
        RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

        inner class MatchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val txtMatchName: TextView = view.findViewById(R.id.matchName)
            val txtDate: TextView = view.findViewById(R.id.date)
            val txtVenue: TextView = view.findViewById(R.id.venue)
            val imgTeam1: ImageView = view.findViewById(R.id.imageTeam1)
            val imgTeam2: ImageView = view.findViewById(R.id.imageTeam2)
            val rootView: View = view
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_match_list, parent, false)
            return MatchViewHolder(view)
        }

        override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
            val match = matches[position]

            holder.txtMatchName.text = match.matchName
            holder.txtDate.text = "Date: ${match.date}"
            holder.txtVenue.text = "Venue: ${match.venue}"


            decodeBase64ToImage(match.team1Logo, holder.imgTeam1)
            decodeBase64ToImage(match.team2Logo, holder.imgTeam2)

            // Set click listener for the entire item
            holder.rootView.setOnClickListener {
                Intent(holder.rootView.context, Score::class.java).apply {
                    putExtra("MATCH_ID", match.id)
                    putExtra("WINNER_TEXT", winnerText)
                }.also { holder.rootView.context.startActivity(it) }
            }
        }

        override fun getItemCount(): Int = matches.size
    }

    private fun formatTimestamp(timestamp: com.google.firebase.Timestamp?): String {
        return timestamp?.toDate()?.let {
            SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault()).format(it)
        } ?: "Unknown"
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
