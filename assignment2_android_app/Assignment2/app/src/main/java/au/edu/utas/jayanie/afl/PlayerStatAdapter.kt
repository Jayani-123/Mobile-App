import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.edu.utas.jayanie.afl.PlayerStats
import au.edu.utas.jayanie.afl.R

class PlayerStatAdapter : ListAdapter<PlayerStats, PlayerStatAdapter.PlayerViewHolder>(PlayerDiffCallback()) {

    inner class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playerNumber: TextView = itemView.findViewById(R.id.playerNumber)
        val playerName: TextView = itemView.findViewById(R.id.playerName)
        val playerGoal: TextView = itemView.findViewById(R.id.playerGoal)
        val playerBehind: TextView = itemView.findViewById(R.id.playerBehind)
        val playerKick: TextView = itemView.findViewById(R.id.playerKick)
        val playerHandball: TextView = itemView.findViewById(R.id.playerHandball)
        val playerMark: TextView = itemView.findViewById(R.id.playerMark)
        val playerTackles: TextView = itemView.findViewById(R.id.playerTackles)
        val playerDisposal: TextView = itemView.findViewById(R.id.playerDisposal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player_stat, parent, false)
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val player = getItem(position)
        holder.playerNumber.text = player.number.toString()
        holder.playerName.text = player.name
        holder.playerGoal.text = player.goals.toString()
        holder.playerBehind.text = player.behinds.toString()
        holder.playerKick.text = player.kicks.toString()
        holder.playerHandball.text = player.handballs.toString()
        holder.playerMark.text = player.marks.toString()
        holder.playerTackles.text = player.tackles.toString()
        holder.playerDisposal.text = player.disposals.toString()
    }
}

class PlayerDiffCallback : DiffUtil.ItemCallback<PlayerStats>() {
    override fun areItemsTheSame(oldItem: PlayerStats, newItem: PlayerStats): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PlayerStats, newItem: PlayerStats): Boolean {
        return oldItem == newItem
    }
}