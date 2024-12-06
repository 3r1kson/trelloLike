package com.example.projectmanager.firebase

import android.app.Activity
import android.util.Log
import com.example.projectmanager.activity.MainActivity
import com.example.projectmanager.activity.MyProfileActivity
import com.example.projectmanager.activity.SignInActivity
import com.example.projectmanager.activity.SignUpActivity
import com.example.projectmanager.models.User
import com.example.projectmanager.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FirestoreClass {

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

    fun loadUserData(activity: Activity) {
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
                        activity.updateNavigationUserDetails(loggedInUser)
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
}