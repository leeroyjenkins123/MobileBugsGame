package com.example.mobilebugsgame

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RecordsFragment : Fragment() {

    private lateinit var viewModel: GameViewModel

    data class PlayerScore(val fullName: String, val maxScore: Int)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_records, container, false)
        val listView = view.findViewById<ListView>(R.id.listViewRecords)

        // Инициализация ViewModel
        val database = AppDatabase.getInstance(requireContext())
        val repository = GameRepository(
            database.playerDao(),
            database.gameSettingsDao(),
            database.gameResultsDao()
        )
        viewModel = ViewModelProvider(this, GameViewModelFactory(repository))[GameViewModel::class.java]

        val adapter = object : BaseAdapter() {
            private var players = listOf<PlayerScore>()

            override fun getCount(): Int = players.size
            override fun getItem(position: Int): PlayerScore = players[position]
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val itemView = convertView ?: inflater.inflate(R.layout.item_record, parent, false)
                val playerScore = players[position]
                val nameTextView = itemView.findViewById<TextView>(R.id.txtPlayerName)
                val scoreTextView = itemView.findViewById<TextView>(R.id.txtPlayerScore)
                nameTextView.text = playerScore.fullName
                scoreTextView.text = "Максимальный счет: ${playerScore.maxScore.toString()}"
                return itemView
            }

            fun submitList(newPlayers: List<PlayerScore>) {
                players = newPlayers
                notifyDataSetChanged()
            }
        }

        listView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getAllPlayers().collectLatest { players ->
                viewModel.getTopScores().collectLatest { scores ->
                    val playerScores = players.map { player ->
                        val maxScore = scores
                            .filter { it.playerId == player.id }
                            .maxByOrNull { it.score }?.score ?: 0
                        PlayerScore(player.fullName, maxScore)
                    }.sortedByDescending { it.maxScore }
                    adapter.submitList(playerScores)
                }
            }
        }

        return view
    }
}