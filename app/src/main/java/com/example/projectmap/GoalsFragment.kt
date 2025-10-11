package com.example.projectmap

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class Goal(
    val id: String,
    val goalName: String,
    val targetAmount: Long,
    val currentAmount: Long,
    val deadline: Timestamp,
    val status: String
)

class GoalsFragment : Fragment(R.layout.fragment_goals) {

    private val firestore = FirebaseFirestore.getInstance()
    private var userId: String? = null
    private lateinit var goalsAdapter: GoalsAdapter
    private val goalsList = mutableListOf<Goal>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("MyAppPrefs", 0)
        userId = prefs.getString("userId", null)

        val topAppBar = view.findViewById<MaterialToolbar>(R.id.topAppBar)
        val rvGoals = view.findViewById<RecyclerView>(R.id.rvGoals)
        val fabAddGoal = view.findViewById<FloatingActionButton>(R.id.fabAddGoal)

        // Back button
        topAppBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        rvGoals.layoutManager = LinearLayoutManager(requireContext())
        goalsAdapter = GoalsAdapter(goalsList) { goal ->
            showGoalOptionsDialog(goal)
        }
        rvGoals.adapter = goalsAdapter

        if (userId != null) {
            loadGoals()
        } else {
            Toast.makeText(requireContext(), "Belum login!", Toast.LENGTH_SHORT).show()
        }

        fabAddGoal.setOnClickListener {
            showAddGoalDialog()
        }
    }

    private fun loadGoals() {
        firestore.collection("Goals")
            .whereEqualTo("user_id", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    goalsList.clear()
                    for (doc in snapshots) {
                        val goal = Goal(
                            id = doc.id,
                            goalName = doc.getString("goal_name") ?: "",
                            targetAmount = doc.getLong("target_amount") ?: 0,
                            currentAmount = doc.getLong("current_amount") ?: 0,
                            deadline = doc.getTimestamp("deadline") ?: Timestamp.now(),
                            status = doc.getString("status") ?: "active"
                        )
                        goalsList.add(goal)
                    }
                    goalsAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun showGoalOptionsDialog(goal: Goal) {
        val options = arrayOf("Edit", "Hapus", "Tambah Tabungan", "Batal")
        AlertDialog.Builder(requireContext())
            .setTitle(goal.goalName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditGoalDialog(goal)
                    1 -> showDeleteConfirmation(goal)
                    2 -> showAddSavingToGoal(goal)
                }
            }
            .show()
    }

    private fun showEditGoalDialog(goal: Goal) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_goal, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Target Tabungan")
            .setView(dialogView)
            .create()

        val etGoalName = dialogView.findViewById<EditText>(R.id.etGoalName)
        val etTargetAmount = dialogView.findViewById<EditText>(R.id.etTargetAmount)
        val etDeadline = dialogView.findViewById<EditText>(R.id.etDeadline)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAddGoal)

        btnAdd.text = "Update"

        // Pre-fill with existing data
        etGoalName.setText(goal.goalName)
        etTargetAmount.setText(goal.targetAmount.toString())

        val selectedCalendar = Calendar.getInstance()
        selectedCalendar.time = goal.deadline.toDate()
        etDeadline.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedCalendar.time))

        etDeadline.setOnClickListener {
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedCalendar.set(year, month, day)
                    etDeadline.setText("$day/${month + 1}/$year")
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.datePicker.minDate = System.currentTimeMillis()
            datePicker.show()
        }

        btnAdd.setOnClickListener {
            val goalName = etGoalName.text.toString().trim()
            val targetAmountStr = etTargetAmount.text.toString().trim()

            if (goalName.isEmpty() || targetAmountStr.isEmpty()) {
                Toast.makeText(requireContext(), "Lengkapi semua field!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val targetAmount = targetAmountStr.toLongOrNull()
            if (targetAmount == null || targetAmount <= 0) {
                Toast.makeText(requireContext(), "Jumlah harus lebih dari 0!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = hashMapOf<String, Any>(
                "goal_name" to goalName,
                "target_amount" to targetAmount,
                "deadline" to Timestamp(selectedCalendar.time)
            )

            firestore.collection("Goals").document(goal.id)
                .update(updates)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Target berhasil diupdate!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }

    private fun showDeleteConfirmation(goal: Goal) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Target?")
            .setMessage("Yakin ingin menghapus \"${goal.goalName}\"?")
            .setPositiveButton("Hapus") { _, _ ->
                firestore.collection("Goals").document(goal.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Target dihapus!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showAddSavingToGoal(goal: Goal) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_simple_amount, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Tambah ke ${goal.goalName}")
            .setView(dialogView)
            .create()

        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAdd)

        btnAdd.setOnClickListener {
            val amountStr = etAmount.text.toString().trim()

            if (amountStr.isEmpty()) {
                Toast.makeText(requireContext(), "Masukkan jumlah!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toLongOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(requireContext(), "Jumlah harus lebih dari 0!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Add transaction
            val transaction = hashMapOf(
                "user_id" to userId!!,
                "type" to "saving",
                "goal_id" to goal.id,
                "category" to goal.goalName,
                "amount" to amount,
                "date" to Timestamp(Date()),
                "created_at" to Timestamp(Date()),
                "note" to "Tabungan untuk ${goal.goalName}"
            )

            firestore.collection("Transactions")
                .add(transaction)
                .addOnSuccessListener {
                    // Update goal's current_amount
                    val goalRef = firestore.collection("Goals").document(goal.id)
                    firestore.runTransaction { trans ->
                        val goalSnapshot = trans.get(goalRef)
                        val currentAmount = goalSnapshot.getLong("current_amount") ?: 0L
                        trans.update(goalRef, "current_amount", currentAmount + amount)
                    }.addOnSuccessListener {
                        Toast.makeText(requireContext(), "Tabungan ditambahkan!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }

    private fun showAddGoalDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_goal, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Tambah Target Tabungan")
            .setView(dialogView)
            .create()

        val etGoalName = dialogView.findViewById<EditText>(R.id.etGoalName)
        val etTargetAmount = dialogView.findViewById<EditText>(R.id.etTargetAmount)
        val etDeadline = dialogView.findViewById<EditText>(R.id.etDeadline)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAddGoal)

        val selectedCalendar = Calendar.getInstance()
        etDeadline.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedCalendar.time))

        etDeadline.setOnClickListener {
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedCalendar.set(year, month, day)
                    etDeadline.setText("$day/${month + 1}/$year")
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.datePicker.minDate = System.currentTimeMillis()
            datePicker.show()
        }

        btnAdd.setOnClickListener {
            val goalName = etGoalName.text.toString().trim()
            val targetAmountStr = etTargetAmount.text.toString().trim()

            if (goalName.isEmpty()) {
                Toast.makeText(requireContext(), "Masukkan nama target!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (targetAmountStr.isEmpty()) {
                Toast.makeText(requireContext(), "Masukkan jumlah target!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val targetAmount = targetAmountStr.toLongOrNull()
            if (targetAmount == null || targetAmount <= 0) {
                Toast.makeText(requireContext(), "Jumlah harus lebih dari 0!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val goal = hashMapOf(
                "user_id" to userId!!,
                "goal_name" to goalName,
                "target_amount" to targetAmount,
                "current_amount" to 0L,
                "deadline" to Timestamp(selectedCalendar.time),
                "status" to "active",
                "created_at" to Timestamp(Date())
            )

            firestore.collection("Goals")
                .add(goal)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Target tabungan ditambahkan!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }
}

class GoalsAdapter(
    private val goals: List<Goal>,
    private val onItemClick: (Goal) -> Unit
) : RecyclerView.Adapter<GoalsAdapter.GoalViewHolder>() {

    class GoalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvGoalName: TextView = view.findViewById(R.id.tvGoalName)
        val tvProgress: TextView = view.findViewById(R.id.tvProgress)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val tvMotivation: TextView = view.findViewById(R.id.tvMotivation)
        val tvDeadline: TextView = view.findViewById(R.id.tvDeadline)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): GoalViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goal, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]
        val context = holder.itemView.context

        holder.tvGoalName.text = goal.goalName

        val progress = if (goal.targetAmount > 0) {
            ((goal.currentAmount.toDouble() / goal.targetAmount) * 100).toInt()
        } else 0

        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        val currentFormatted = formatter.format(goal.currentAmount).replace("Rp", "Rp ")
        val targetFormatted = formatter.format(goal.targetAmount).replace("Rp", "Rp ")

        holder.tvProgress.text = "$currentFormatted / $targetFormatted ($progress%)"
        holder.progressBar.progress = progress

        // Calculate days remaining
        val now = Calendar.getInstance().time
        val deadline = goal.deadline.toDate()
        val daysRemaining = TimeUnit.MILLISECONDS.toDays(deadline.time - now.time)

        val deadlineStr = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(deadline)
        holder.tvDeadline.text = "Deadline: $deadlineStr"

        // Motivational messages
        val motivation = when {
            progress >= 100 -> "🎉 Target tercapai! Selamat!"
            progress >= 75 && daysRemaining > 30 -> "💪 Hebat! Hampir sampai target!"
            progress >= 50 && daysRemaining > 30 -> "👍 Bagus! Terus semangat menabung!"
            progress >= 25 && daysRemaining > 30 -> "🌟 Kamu bisa! Terus konsisten!"
            daysRemaining <= 7 && progress < 100 -> "⏰ Deadline tinggal $daysRemaining hari! Ayo semangat!"
            daysRemaining <= 30 && progress < 50 -> "⚠️ Tingkatkan tabunganmu! Deadline mendekat!"
            daysRemaining <= 0 && progress < 100 -> "⏱️ Deadline terlewat. Perpanjang atau sesuaikan target?"
            else -> "💰 Ayo mulai menabung untuk mencapai targetmu!"
        }

        holder.tvMotivation.text = motivation

        // Click listener for CRUD
        holder.itemView.setOnClickListener {
            onItemClick(goal)
        }
    }

    override fun getItemCount() = goals.size
}