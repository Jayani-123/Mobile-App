package au.edu.utas.jayanie.afl

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class PlayerListDialogFragment : DialogFragment() {
    private lateinit var db: FirebaseFirestore
    private var currentMatchId: String = ""
    private val players = mutableListOf<Player>()
    private var listener: OnPlayerSelectedListener? = null

    private lateinit var adapter: PlayerAdapter
    private val originalPlayers = mutableListOf<Player>()
    private val filteredPlayers = mutableListOf<Player>()

    interface OnPlayerSelectedListener {
        fun onPlayerSelected(player: Player)
    }

    fun setOnPlayerSelectedListener(listener: OnPlayerSelectedListener) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        currentMatchId = arguments?.getString("matchId") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_player_list_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.playerRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = PlayerAdapter(filteredPlayers) { selectedPlayer ->
            listener?.onPlayerSelected(selectedPlayer)
            dismiss()
        }
        recyclerView.adapter = adapter

        view.findViewById<SearchView>(R.id.searchView).setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterPlayers(newText.orEmpty())
                return true
            }
        })

        view.findViewById<Button>(R.id.closeButton).setOnClickListener {
            dismiss()
        }

        if (currentMatchId.isNotEmpty()) {
            fetchPlayersFromBothTeams()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.8).toInt()
        )
    }

    private fun fetchPlayersFromBothTeams() {
        db.collection("matches").document(currentMatchId).get()
            .addOnSuccessListener { matchSnapshot ->
                val team1Name = matchSnapshot.getString("team1Name") ?: "Team 1"
                val team2Name = matchSnapshot.getString("team2Name") ?: "Team 2"

                val team1Task = db.collection("matches").document(currentMatchId)
                    .collection("playersTeam1").get()
                val team2Task = db.collection("matches").document(currentMatchId)
                    .collection("playersTeam2").get()

                Tasks.whenAllSuccess<QuerySnapshot>(team1Task, team2Task)
                    .addOnSuccessListener { results ->
                        players.clear()
                        results.forEachIndexed { index, querySnapshot ->
                            val teamName = if (index == 0) team1Name else team2Name
                            querySnapshot.documents.forEach { doc ->
                                val player = Player(
                                    id = doc.id,
                                    number = doc.getLong("number")?.toInt() ?: 0,
                                    name = doc.getString("name") ?: "",
                                    teamName = doc.getString("teamName") ?: "",
                                    position = doc.getString("position") ?: "",
                                    age = doc.getLong("age")?.toInt() ?: 0,
                                    height = doc.getLong("height")?.toInt() ?: 0,
                                    image = doc.getString("image") ?: ""
                                )
                                if (!players.contains(player)) {
                                    players.add(player)
                                }
                            }
                        }
                        players.sortBy { it.number }
                        updatePlayerList()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error loading players", Toast.LENGTH_SHORT).show()
                        Log.e("PlayerDialog", "Error fetching players", e)
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading match data", Toast.LENGTH_SHORT).show()
                Log.e("PlayerDialog", "Error fetching match", e)
            }
    }

    private fun updatePlayerList() {
        activity?.runOnUiThread {
            originalPlayers.clear()
            originalPlayers.addAll(players)
            filterPlayers("")
        }
    }

    private fun filterPlayers(query: String) {
        filteredPlayers.clear()
        if (query.isEmpty()) {
            filteredPlayers.addAll(originalPlayers)
        } else {
            filteredPlayers.addAll(originalPlayers.filter {
                it.name.contains(query, ignoreCase = true)
            })
        }
        adapter.notifyDataSetChanged()
    }

    companion object {
        fun newInstance(matchId: String): PlayerListDialogFragment {
            val fragment = PlayerListDialogFragment()
            val args = Bundle()
            args.putString("matchId", matchId)
            fragment.arguments = args
            return fragment
        }
    }

    private inner class PlayerAdapter(
        private val players: List<Player>,
        private val onPlayerClick: (Player) -> Unit
    ) : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return PlayerViewHolder(view)
        }

        override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
            holder.bind(players[position])
        }

        override fun getItemCount() = players.size

        inner class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textView: TextView = itemView.findViewById(android.R.id.text1)
            fun bind(player: Player) {
                textView.text = "${player.number} - ${player.name} (${player.teamName})"
                itemView.setOnClickListener { onPlayerClick(player) }
            }
        }
    }
}
