package au.edu.utas.jayanie.afl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ActionAdapter(
    private val team1Name: String,
    private val team2Name: String,
    private var actions: List<ActionItem> = emptyList()) :
    RecyclerView.Adapter<ActionAdapter.ActionViewHolder>()
{

    class ActionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val team1Player: TextView = itemView.findViewById(R.id.team1Player)
        val actionType: TextView = itemView.findViewById(R.id.actionType)
        val time: TextView = itemView.findViewById(R.id.time)
        val team2Player: TextView = itemView.findViewById(R.id.team2Player)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_action, parent, false)
        return ActionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
        val action = actions[position]

        // Reset both TextViews
        holder.team1Player.text = ""
        holder.team2Player.text = ""


// Set the player name on the correct side based on team
        if (action.team == team1Name) {
            holder.team1Player.text = action.playerName

        } else if (action.team == team2Name) {
            holder.team2Player.text = action.playerName

        }

        holder.actionType.text = action.actionType
        holder.time.text = action.time
    }


    fun updateActions(newActions: List<ActionItem>) {
        actions = newActions
        notifyDataSetChanged()
    }

    override fun getItemCount() = actions.size
}