package com.example.projectmanager.firebase

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.example.projectmanager.activity.BaseActivity
import com.example.projectmanager.activity.CardDetailsActivity
import com.example.projectmanager.activity.CreateBoardActivity
import com.example.projectmanager.activity.MainActivity
import com.example.projectmanager.activity.MemberActivity
import com.example.projectmanager.activity.MyProfileActivity
import com.example.projectmanager.activity.SignInActivity
import com.example.projectmanager.activity.SignUpActivity
import com.example.projectmanager.activity.TaskListActivity
import com.example.projectmanager.models.Board
import com.example.projectmanager.models.User
import com.example.projectmanager.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FirestoreClass : BaseActivity() {

    private val mFireStore = FirebaseFirestore.getInstance()

    fun registerUser(activity: SignUpActivity, userInfo: User) {
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .set(userInfo, SetOptions.merge())
            .addOnSuccessListener {
                Log.i("FCM", "USER REGISTERED ON FIREBASE")
                activity.userRegisteredSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(
                    activity.javaClass.simpleName,
                    "Error writing document",
                    e
                )
            }
    }

    fun createBoard(activity: CreateBoardActivity, board: Board) {
        mFireStore.collection(Constants.BOARDS)
            .document()
            .set(board, SetOptions.merge())
            .addOnSuccessListener {
                Log.i("FCM", "USER REGISTERED ON FIREBASE")
                activity.boardCreatedSuccessfully()
            }
            .addOnFailureListener { e ->
                Log.e(
                    activity.javaClass.simpleName,
                    "Error creating Board",
                    e
                )
            }
    }

    fun getBoardsList(activity: MainActivity) {
        mFireStore.collection(Constants.BOARDS)
            .whereArrayContains(Constants.ASSIGNED_TO, getCurrentUserId())
            .get()
            .addOnSuccessListener {
                document ->
                Log.i(activity.javaClass.simpleName, document.documents.toString())
                val boardsList: ArrayList<Board> = ArrayList()

                for (i in document.documents) {
                    val board = i.toObject(Board::class.java)!!
                    board.documentId = i.id
                    boardsList.add(board)
                }

                activity.populateBoardListToUI(boardsList)
            }.addOnFailureListener {
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while creating board")

            }
    }

    fun addUpdateTaskList(activity: Activity, board: Board) {
        val taskListHashMap = HashMap<String, Any>()
        taskListHashMap[Constants.TASK_LIST] = board.taskList

        mFireStore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(taskListHashMap)
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName, "Task List updated Successfully")
                if (activity is TaskListActivity) {
                    activity.addUpdateTaskListSuccess()
                } else if (activity is CardDetailsActivity){
                    activity.addUpdateTaskListSuccess()
                }
            }.addOnFailureListener { e->
                if (activity is TaskListActivity) {
                    activity.hideProgressDialog()
                } else if (activity is CardDetailsActivity) {
                    activity.hideProgressDialog()
                }

                Log.e(activity.javaClass.simpleName, "Error while creating board", e)

            }
    }

    fun updateUserProfileData(activity: Activity, userHashMap: HashMap<String, Any>) {
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .update(userHashMap)
            .addOnSuccessListener {
                Toast.makeText(activity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                when(activity) {
                    is MainActivity -> activity.tokenUpdateSuccess()

                    is MyProfileActivity -> activity.profileUpdateSuccess()
                }
            }.addOnFailureListener { e ->
                when(activity) {
                    is MainActivity -> activity.hideProgressDialog()

                    is MyProfileActivity -> activity.hideProgressDialog()
                }
                Log.e(activity.javaClass.simpleName, "Error while creating a board.", e)
                Toast.makeText(activity, "Profile update error!", Toast.LENGTH_SHORT).show()
            }
    }

    fun loadUserData(activity: Activity, readBoardsList: Boolean = false) {
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .get()
            .addOnSuccessListener { document ->
                val loggedInUser = document.toObject(User::class.java)!!

                when(activity) {
                    is SignInActivity -> {
                        activity.signInSuccess(loggedInUser)
                    }
                    is MainActivity -> {
                        activity.updateNavigationUserDetails(loggedInUser, readBoardsList)
                    }
                    is MyProfileActivity -> {
                        activity.setUserDataInUI(loggedInUser)
                    }
                }

            }
            .addOnFailureListener {
                e ->
                when(activity) {
                    is SignInActivity -> {
                        activity.hideProgressDialog()
                    }
                    is MainActivity -> {
                        activity.hideProgressDialog()
                    }
                }
                Log.e(
                    "SignInUser",
                    "Error writing document",
                    e
                )
            }
    }

    fun getCurrentUserId(): String {
        var currentUser = FirebaseAuth.getInstance().currentUser
        var currentUserID = ""

        if (currentUser!=null) {
            currentUserID = currentUser.uid
        }
        return currentUserID
    }

    fun getBoardDetails(activity: TaskListActivity, documentId: String) {
        mFireStore.collection(Constants.BOARDS)
            .document(documentId)
            .get()
            .addOnSuccessListener {
                    document ->
                Log.i(activity.javaClass.simpleName, document.toString())
                var board = document.toObject(Board::class.java)
                board?.documentId = document.id
                activity.boardDetails(board!!)

            }.addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while creating board $e")

            }
    }

    fun getAssignedMemberListDetails(activity: Activity, assignedTo: ArrayList<String>) {
        mFireStore.collection(Constants.USERS)
            .whereIn(Constants.ID, assignedTo)
            .get()
            .addOnSuccessListener { document ->
                val usersList: ArrayList<User> = ArrayList()

                for (i in document) {
                    val user = i.toObject(User::class.java)
                    usersList.add(user)
                }

                if (activity is MemberActivity) {
                    activity.setupMembersList(usersList)
                } else if (activity is TaskListActivity) {
                    activity.boardMembersDetailsList(usersList)
                }

            }.addOnFailureListener { e ->
                if (activity is MemberActivity) {
                    activity.hideProgressDialog()
                } else if (activity is TaskListActivity) {
                    activity.hideProgressDialog()
                }
                Log.e(activity.javaClass.simpleName, "Error while creating a board", e)
                Toast.makeText(this, "Error while assigning Members $e", Toast.LENGTH_LONG).show()
            }
    }

    fun getMemberDetails(activity: MemberActivity, email: String) {
        mFireStore.collection(Constants.USERS)
            .whereEqualTo(Constants.EMAIL, email)
            .get()
            .addOnSuccessListener { document ->
                if (document.documents.size > 0) {
                    val user = document.documents[0].toObject(User::class.java)!!
                    activity.memberDetails(user)
                } else {
                    activity.hideProgressDialog()
                    activity.showErrorSnackBar("No such member found")
                }

            }.addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while getting user details", e)
                Toast.makeText(this, "Error while getting user details $e", Toast.LENGTH_LONG).show()
            }
    }

    fun assignMemberToBoard(activity: MemberActivity, board: Board, user: User) {

        val assignedToHashMap = HashMap<String, Any>()
        assignedToHashMap[Constants.ASSIGNED_TO] = board.assignedTo

        mFireStore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(assignedToHashMap)
            .addOnSuccessListener {
                activity.memberAssignSucces(user)
            }.addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while creating a board", e)
                Toast.makeText(this, "Error while creating a board $e", Toast.LENGTH_LONG).show()
            }
    }

}