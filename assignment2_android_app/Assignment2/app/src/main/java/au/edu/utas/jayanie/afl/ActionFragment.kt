package au.edu.utas.jayanie.afl

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import au.edu.utas.jayanie.afl.databinding.FragmentActionBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ActionFragment : Fragment() {
    private var _binding: FragmentActionBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: ActionAdapter
    // Properties to store fragment arguments
    private var quarter: String? = null
    private var matchId: String? = null
    private var matchName: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve arguments passed from newInstance
        arguments?.let {
            quarter = it.getString("QUARTER")
            matchId = it.getString("MATCH_ID")
            matchName = it.getString("MATCH_NAME")

        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

       loadTeamNames()


    }
    private fun loadTeamNames() {
        val matchId = matchId ?: return

        FirebaseFirestore.getInstance()
            .collection("matches")
            .document(matchId)
            .get()
            .addOnSuccessListener { document ->
                val team1Name = document.getString("team1Name") ?: "team1"
                val team2Name = document.getString("team2Name") ?: "team2"
                binding.team1Label.text = team1Name
                binding.team2Label.text = team2Name

                // Initialize adapter HERE
                adapter = ActionAdapter(team1Name, team2Name, emptyList())

                // Setup RecyclerView
                binding.actionsRecyclerView.apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = this@ActionFragment.adapter
                    addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
                }

                // NOW load actions (after adapter is ready)
                loadActions()
            }
    }
    private fun loadActions() {
        if (matchId == null|| quarter == null) {
            Log.e("ActionFragment", "matchId or quarter is nul")
            return
        }

        db.collection("matches")
            .document(matchId!!)
            .collection("actions")
            .whereEqualTo("quarter", quarter)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ActionFragment", "Error loading actions", error)
                    return@addSnapshotListener
                }

                val actions = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        ActionItem(
                            playerName = "${doc.getLong("playerNumber") ?: 0}. ${doc.getString("playerName") ?: "Unknown"}",
                            actionType = doc.getString("actionType") ?: "",
                            time = doc.getString("gameTimeFormatted") ?: "",
                            team = doc.getString("team") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("ActionFragment", "Error parsing action", e)
                        null
                    }
                } ?: emptyList()

                // Update the adapter with new actions
                adapter.updateActions(actions)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            quarter: String,
            matchId: String,
            matchName: String,
            team1Name: String,
            team2Name: String
        ): ActionFragment {
            return ActionFragment().apply {
                arguments = Bundle().apply {
                    putString("QUARTER", quarter)
                    putString("MATCH_ID", matchId)
                    putString("MATCH_NAME", matchName)
                    putString("TEAM1_NAME", team1Name)
                    putString("TEAM2_NAME", team2Name)
                }
            }
        }
    }
}