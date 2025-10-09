package com.example.mobilebugsgame

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PlayerSelectionDialog(
    private val onPlayerSelected: (PlayerEntity, GameSettingsEntity?) -> Unit
) : DialogFragment() {

    private lateinit var viewModel: GameViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_player_selection, null)

        val listView = view.findViewById<ListView>(R.id.listViewPlayers)

        val adapter = object : BaseAdapter() {
            private var players: List<PlayerEntity> = emptyList()

            override fun getCount(): Int = players.size
            override fun getItem(position: Int): PlayerEntity = players[position]
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val itemView = convertView ?: inflater.inflate(R.layout.item_player, parent, false)
                val player = players[position]

                val tvName = itemView.findViewById<TextView>(R.id.tvPlayerName)
                val tvDetails = itemView.findViewById<TextView>(R.id.tvPlayerDetails)

                tvName.text = player.fullName
                tvDetails.text = "${player.gender}, ${player.course}, Уровень: ${player.difficulty}"

                itemView.setOnClickListener {
                    lifecycleScope.launch {
                        viewModel.getSettingsByPlayer(player.id).collectLatest { settings ->
                            onPlayerSelected(player, settings)
                            dismiss()
                        }
                    }
                }

                return itemView
            }

            fun submitList(newPlayers: List<PlayerEntity>) {
                players = newPlayers
                notifyDataSetChanged()
            }
        }

        listView.adapter = adapter

        // Инициализация ViewModel
        val database = AppDatabase.getInstance(requireContext())
        val repository = GameRepository(
            database.playerDao(),
            database.gameSettingsDao(),
            database.gameResultsDao()
        )
        viewModel = GameViewModel(repository)

        // Загрузка игроков
        lifecycleScope.launch {
            viewModel.getAllPlayers().collectLatest { players ->
                adapter.submitList(players)
            }
        }

        builder.setView(view)
            .setTitle("Выберите игрока")
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }

        return builder.create()
    }
}