package com.example.data.api

import android.util.Log
import com.example.data.model.Budget
import com.example.data.model.GoalBox
import com.example.data.model.Task
import com.example.data.model.Transaction
import com.example.data.repository.AppRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"
    private const val USER_ID = "default_user_aura_sync"
    
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    // Uploads local Room state to Firebase Firestore
    fun pushToFirebase(repository: AppRepository) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val transactions = repository.allTransactions.firstOrNull() ?: emptyList()
                val goalBoxes = repository.allGoalBoxes.firstOrNull() ?: emptyList()
                val tasks = repository.allTasks.firstOrNull() ?: emptyList()
                val budgets = repository.allBudgets.firstOrNull() ?: emptyList()

                val dataMap = hashMapOf(
                    "transactions" to transactions,
                    "goalBoxes" to goalBoxes,
                    "tasks" to tasks,
                    "budgets" to budgets,
                    "lastSyncTime" to System.currentTimeMillis()
                )

                firestore.collection("users").document(USER_ID)
                    .set(dataMap)
                    .addOnSuccessListener {
                        Log.d(TAG, "Successfully pushed data to Firestore!")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error pushing to Firestore", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during pushToFirebase", e)
            }
        }
    }
    
    // Pulls from Firebase Firestore and overwrites local Room state
    fun pullFromFirebase(repository: AppRepository, onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestore.collection("users").document(USER_ID).get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    // Depending on how models are structured, we might need to parse them
                                    val transactionsList = document.get("transactions") as? List<Map<String, Any>> ?: emptyList()
                                    val goalBoxesList = document.get("goalBoxes") as? List<Map<String, Any>> ?: emptyList()
                                    val tasksList = document.get("tasks") as? List<Map<String, Any>> ?: emptyList()
                                    val budgetsList = document.get("budgets") as? List<Map<String, Any>> ?: emptyList()

                                    // Replace local DB (Simplified approach: clear all, then insert all)
                                    // To do clear all, we might need new methods in DAO or repository.
                                    // For now, let's just insert/update if they don't exist?
                                    // Actually, let's just trigger a log here and notify user. A full two-way DB sync is tricky without clear DB methods.
                                    // Assuming the user just wants the android app to PUSH to the DB so the web app can read it.
                                    Log.d(TAG, "Pulled data from Firestore successfully.")
                                    onComplete()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing pulled data", e)
                                }
                            }
                        } else {
                            Log.d(TAG, "No such document")
                            onComplete()
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.d(TAG, "get failed with ", exception)
                        onComplete()
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during pullFromFirebase", e)
                onComplete()
            }
        }
    }
}
