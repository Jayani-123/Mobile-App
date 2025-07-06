package au.edu.utas.jayanie.afl

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.edu.utas.jayanie.afl.databinding.FragmentQuarterBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import com.google.firebase.firestore.Query

class QuarterFragment : Fragment() {
    private var _binding: FragmentQuarterBinding? = null
    private val binding get() = _binding!!
    private var quarterTitle: String? = null
    private val totalTimeInMillis = 20 * 60 * 1000L // 20 minutes
    private var timer: CountDownTimer? = null
    private var isPaused = false
    private var timeElapsed: Long = 0L
    private var isGameRunning = false

    private lateinit var team1Adapter: PlayerAdapter
    private lateinit var team2Adapter: PlayerAdapter
    private var currentMatchId: String? = null
    private var selectedAction: String? = null
    private var lastActionType: String? = null
    private var lastActionTeam: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            quarterTitle = it.getString(ARG_QUARTER)
            currentMatchId = it.getString(ARG_MATCH_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuarterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentMatchId = arguments?.getString(ARG_MATCH_ID)

        // Initialize RecyclerView and adapters
        team1Adapter = PlayerAdapter { player ->
            if (selectedAction != null) {
                recordAction(selectedAction!!, player)
            } else {
                showSnackbar("Please select an action first")
            }
        }

        binding.team1Players.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = team1Adapter
        }

        team2Adapter = PlayerAdapter { player ->
            if (selectedAction != null) {
                recordAction(selectedAction!!, player)
            } else {
                showSnackbar("Please select an action first")
            }
        }

        binding.team2Players.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = team2Adapter
        }

        // Set up timer UI
        binding.textClock.text = "00:00"
        binding.progressBar.max = (totalTimeInMillis / 1000).toInt()
        binding.progressBar.progress = 0

        // Set button click listeners
        binding.playBtn.setOnClickListener {
            startTimer()
            isGameRunning = true
            loadLastAction() // Refresh action states when game starts
        }
        binding.pauseBtn.setOnClickListener {
            pauseTimer()
            isGameRunning = false
        }
        binding.stopBtn.setOnClickListener {
            stopTimer()
            isGameRunning = false
        }

        // Set up action button click listeners with validation
        binding.kickBtn.setOnClickListener {
            if (checkGameState()) {
                selectedAction = "Kick"
                showSelectedAction("Kick")
            }
        }
        binding.tackleBtn.setOnClickListener {
            if (checkGameState()) {
                selectedAction = "Tackle"
                showSelectedAction("Tackle")
            }
        }
        binding.handballBtn.setOnClickListener {
            if (checkGameState()) {
                selectedAction = "Handball"
                showSelectedAction("Handball")
            }
        }
        binding.goalBtn.setOnClickListener {
            if (checkGameState()) {
                selectedAction = "Goal"
                showSelectedAction("Goal")
            }
        }
        binding.markBtn.setOnClickListener {
            if (checkGameState()) {
                selectedAction = "Mark"
                showSelectedAction("Mark")
            }
        }
        binding.behindBtn.setOnClickListener {
            if (checkGameState()) {
                selectedAction = "Behind"
                showSelectedAction("Behind")
            }
        }

        // Set long click listeners for tooltips
        binding.goalBtn.setOnLongClickListener {
            if (!binding.goalBtn.isEnabled) {
                showSnackbar("Goal can only be scored after a kick by your team")
                true
            } else {
                false
            }
        }

        binding.behindBtn.setOnLongClickListener {
            if (!binding.behindBtn.isEnabled) {
                showSnackbar("Behind can only be scored after a kick or handball by your team")
                true
            } else {
                false
            }
        }

        loadTeamColors()
        loadTeam1Players()
        loadTeam2Players()
        loadLastAction() // Load the last action to initialize button states
    }

    private fun loadLastAction() {
        val matchId = currentMatchId ?: return

        FirebaseFirestore.getInstance()
            .collection("matches")
            .document(matchId)
            .collection("actions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val lastAction = querySnapshot.documents[0]
                    lastActionType = lastAction.getString("actionType")
                    lastActionTeam = lastAction.getString("team")
                } else {
                    lastActionType = null
                    lastActionTeam = null
                }
                updateActionButtons()
            }
            .addOnFailureListener { e ->
                Log.e("QuarterFragment", "Error loading last action", e)
            }
    }

    private fun updateActionButtons() {
        val team1Name = binding.Team1.text.toString()
        val team2Name = binding.Team2.text.toString()

        // Goal can only be scored after a kick by the same team
        val canTeam1ScoreGoal = lastActionType == "Kick" && lastActionTeam == team1Name
        val canTeam2ScoreGoal = lastActionType == "Kick" && lastActionTeam == team2Name
        val canScoreGoal = canTeam1ScoreGoal || canTeam2ScoreGoal

        // Behind can be scored after a kick or handball by either team
        val canScoreBehind = lastActionType == "Kick" || lastActionType == "Handball"

        binding.goalBtn.isEnabled = canScoreGoal && isGameRunning
        binding.behindBtn.isEnabled = canScoreBehind && isGameRunning

        // Visual feedback for disabled buttons
        val disabledColor = Color.LTGRAY
        val enabledColor = ContextCompat.getColor(requireContext(), R.color.white)

        binding.goalBtn.setBackgroundColor(if (canScoreGoal && isGameRunning) enabledColor else disabledColor)
        binding.behindBtn.setBackgroundColor(if (canScoreBehind && isGameRunning) enabledColor else disabledColor)
    }

    private fun startTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(totalTimeInMillis - timeElapsed, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeElapsed = totalTimeInMillis - millisUntilFinished
                updateGameTimeDisplay(timeElapsed)
                binding.progressBar.progress = (timeElapsed / 1000).toInt()
            }

            override fun onFinish() {
                timeElapsed = totalTimeInMillis
                updateGameTimeDisplay(timeElapsed)
                binding.progressBar.progress = binding.progressBar.max
                isGameRunning = false
                binding.gameStatus.text = "Game: Finished"
                updateActionButtons()
            }
        }.start()
        isGameRunning = true
        binding.gameStatus.text = "Game: Running"
        updateActionButtons()
    }

    private fun updateGameTimeDisplay(millisElapsed: Long) {
        val secondsElapsed = millisElapsed / 1000
        val minutes = secondsElapsed / 60
        val seconds = secondsElapsed % 60
        binding.textClock.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun pauseTimer() {
        if (!isPaused) {
            timer?.cancel()
            isPaused = true
            binding.gameStatus.text = "Game: Paused"
            updateActionButtons()
        }
    }

    private fun stopTimer() {
        timer?.cancel()
        timeElapsed = 0
        updateGameTimeDisplay(0)
        binding.progressBar.progress = 0
        isGameRunning = false
        binding.gameStatus.text = "Game: Stopped"
        updateActionButtons()
    }

    private fun checkGameState(): Boolean {
        return when {
            !isGameRunning -> {
                showSnackbar("Please start the game first")
                false
            }
            else -> true
        }
    }

    private fun loadTeamColors() {
        val matchId = currentMatchId ?: return

        FirebaseFirestore.getInstance()
            .collection("matches")
            .document(matchId)
            .get()
            .addOnSuccessListener { document ->
                val team1Color = document.getString("team1color") ?: "#FF0000"
                val team2Color = document.getString("team2color") ?: "#0000FF"
                val team1Name = document.getString("team1Name") ?: "team1"
                val team2Name = document.getString("team2Name") ?: "team2"
                binding.Team1.text = team1Name
                binding.Team2.text = team2Name
                binding.team1bg.setBackgroundColor(Color.parseColor(team1Color))
                binding.team2bg.setBackgroundColor(Color.parseColor(team2Color))
                updateActionButtons() // Update buttons after team names are loaded
            }
    }

    private fun loadTeam1Players() {
        val matchId = currentMatchId ?: run {
            Log.e("QuarterFragment", "No matchId in arguments")
            return
        }

        FirebaseFirestore.getInstance()
            .collection("matches")
            .document(matchId)
            .collection("playersTeam1")
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
                            height = doc.getLong("height")?.toInt() ?: 0,
                        )
                    } catch (e: Exception) {
                        Log.e("QuarterFragment", "Error parsing player ${doc.id}", e)
                        null
                    }
                }
                team1Adapter.submitList(players)
            }
            .addOnFailureListener { e ->
                Log.e("QuarterFragment", "Error loading team 1 players", e)
                showSnackbar("Error loading team 1 players")
            }
    }

    private fun loadTeam2Players() {
        val matchId = currentMatchId ?: run {
            Log.e("QuarterFragment", "No matchId in arguments")
            return
        }

        FirebaseFirestore.getInstance()
            .collection("matches")
            .document(matchId)
            .collection("playersTeam2")
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
                            height = doc.getLong("height")?.toInt() ?: 0,
                        )
                    } catch (e: Exception) {
                        Log.e("QuarterFragment", "Error parsing player ${doc.id}", e)
                        null
                    }
                }
                team2Adapter.submitList(players)
            }
            .addOnFailureListener { e ->
                Log.e("QuarterFragment", "Error loading team 2 players", e)
                showSnackbar("Error loading team 2 players")
            }
    }

    private fun showSelectedAction(action: String) {
        showSnackbar("Selected action: $action - Now select a player")
    }

    private fun recordAction(actionType: String, player: Player) {
        if (!isGameRunning) {
            showSnackbar("Game is not running - action not recorded")
            return
        }

        val matchId = currentMatchId ?: run {
            showSnackbar("No match selected")
            return
        }

        // Determine which team the player belongs to
        val (teamCollection, teamName) = when {
            team1Adapter.currentList.any { it.id == player.id } -> {
                "playersTeam1" to binding.Team1.text.toString()
            }
            team2Adapter.currentList.any { it.id == player.id } -> {
                "playersTeam2" to binding.Team2.text.toString()
            }
            else -> {
                showSnackbar("Player not found in any team")
                return
            }
        }

        saveAction(actionType, player, teamCollection, teamName, matchId)
    }

    private fun saveAction(
        actionType: String,
        player: Player,
        teamCollection: String,
        teamName: String,
        matchId: String
    ) {
        val quarter = quarterTitle ?: "Q1"
        val minutes = (timeElapsed / 60000).toInt()
        val seconds = ((timeElapsed % 60000) / 1000).toInt()
        val formattedTime = String.format("%02d:%02d", minutes, seconds)

        val playerRef = FirebaseFirestore.getInstance()
            .collection("matches")
            .document(matchId)
            .collection(teamCollection)
            .document(player.id)

        val action = MatchAction(
            matchId = matchId,
            actionType = actionType,
            playerRef = playerRef.toString(),
            playerName = player.name,
            playerNumber = player.number,
            team = teamName,
            quarter = quarter,
            gameTime = timeElapsed,
            gameTimeFormatted = formattedTime,
            timestamp = DateFormat.format("yyyy-MM-dd HH:mm:ss", Date()).toString()
        )

        FirebaseFirestore.getInstance()
            .collection("matches")
            .document(matchId)
            .collection("actions")
            .add(action)
            .addOnSuccessListener {
                // Update last action info
                lastActionType = actionType
                lastActionTeam = teamName
                updateActionButtons()
                showSnackbar("$actionType recorded for ${player.name}!")
            }
            .addOnFailureListener { e ->
                showSnackbar("Error saving action: ${e.message}")
                Log.e("QuarterFragment", "Error saving action", e)
            }

        selectedAction = null
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        _binding = null
    }

    class PlayerAdapter(
        private val onPlayerSelected: (Player) -> Unit
    ) : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {
        private val players = mutableListOf<Player>()

        val currentList: List<Player>
            get() = players

        fun submitList(newPlayers: List<Player>) {
            players.clear()
            players.addAll(newPlayers)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.add_action_list, parent, false)
            return PlayerViewHolder(view, onPlayerSelected)
        }

        override fun getItemCount(): Int = players.size

        override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
            holder.bind(players[position])
        }

        class PlayerViewHolder(
            itemView: View,
            private val onPlayerSelected: (Player) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {
            private val playerName: TextView = itemView.findViewById(R.id.playerName)

            fun bind(player: Player) {
                playerName.text = "${player.number}. ${player.name}"
                itemView.setOnClickListener { onPlayerSelected(player) }
            }
        }
    }

    companion object {
        private const val ARG_QUARTER = "quarter"
        private const val ARG_MATCH_ID = "matchId"
        private const val ARG_MATCH_NAME = "matchName"

        @JvmStatic
        fun newInstance(quarter: String, matchId: String, matchName: String) =
            QuarterFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_QUARTER, quarter)
                    putString(ARG_MATCH_ID, matchId)
                    putString(ARG_MATCH_NAME, matchName)
                }
            }
    }
}