package com.example.projectmanager.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.collection.intSetOf
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.projectmanager.R
import com.example.projectmanager.adapters.BoardsItemAdapter
import com.example.projectmanager.databinding.ActivityMainBinding
import com.example.projectmanager.firebase.FirestoreClass
import com.example.projectmanager.models.Board
import com.example.projectmanager.models.User
import com.example.projectmanager.util.Constants
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        const val MY_PROFILE_REQUEST_CODE: Int = 11
        const val CREATE_BOARD_REQUEST_CODE: Int = 12
    }

    private lateinit var mUserName: String
    private lateinit var mSharedPreferences: SharedPreferences

    var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        binding?.navView?.setNavigationItemSelectedListener(this)

        var floatingButton = findViewById<FloatingActionButton>(R.id.fab_create_board)
        floatingButton.setOnClickListener {
            var intent = Intent(this, CreateBoardActivity::class.java)
            intent.putExtra(Constants.NAME, mUserName)
            startActivityForResult(intent, CREATE_BOARD_REQUEST_CODE)
        }

        mSharedPreferences = this.getSharedPreferences(Constants.PROJECTMANAGER_PREFERENCES, Context.MODE_PRIVATE)

        val tokenUpdated = mSharedPreferences.getBoolean(Constants.FCM_TOKEN_UPDATED, false)

        if (tokenUpdated) {
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().loadUserData(this, true)
        } else {
            FirebaseInstanceId.getInstance()
                .instanceId.addOnSuccessListener(this@MainActivity) { instanceIdResult ->
                updateFCMToken(instanceIdResult.token)
            }
        }
        setupActionBar()
        FirestoreClass().loadUserData(this, true)
    }

    fun populateBoardListToUI(boardsList: ArrayList<Board>) {
        hideProgressDialog()

        var rvBoardList = findViewById<RecyclerView>(R.id.rv_boards_list)
        var tvNoBoards = findViewById<TextView>(R.id.tv_no_boards_available)

        if (boardsList.size > 0) {
            rvBoardList.visibility = View.VISIBLE
            tvNoBoards.visibility = View.GONE

            rvBoardList.layoutManager = LinearLayoutManager(this)
            rvBoardList.setHasFixedSize(true)

            val adapter = BoardsItemAdapter(this, boardsList)
            rvBoardList.adapter = adapter

            adapter.setOnClickListener(object : BoardsItemAdapter.OnClickListener {
                override fun onClick(position: Int, model: Board) {
                    var intent = Intent(this@MainActivity, TaskListActivity::class.java)
                    intent.putExtra(Constants.DOCUMENT_ID, model.documentId)
                    startActivity(intent)
                }
            })
        } else {
            rvBoardList.visibility = View.GONE
            tvNoBoards.visibility = View.VISIBLE
        }
    }

    private fun setupActionBar() {
        var toolbar: Toolbar = findViewById(R.id.toolbar_main_activity)
        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_action_navigation_menu)
        toolbar.setNavigationOnClickListener {
            toggleDrawer()
        }
    }

    private fun toggleDrawer() {
        if(binding?.drawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
            binding?.drawerLayout?.closeDrawer(GravityCompat.START)
        } else {
            binding?.drawerLayout?.openDrawer(GravityCompat.START)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if(binding?.drawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
            binding?.drawerLayout?.closeDrawer(GravityCompat.START)
        } else {
            doubleBackToExit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == MY_PROFILE_REQUEST_CODE) {
            FirestoreClass().loadUserData(this)
        } else if (resultCode == Activity.RESULT_OK && requestCode == CREATE_BOARD_REQUEST_CODE) {
            FirestoreClass().getBoardsList(this)
        } else {
            Log.e("Cancelled", "Cancelled")
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.d("Navigation", "Selected: ${item.title}")
        when(item.itemId) {
            R.id.nav_my_profile -> {
                startActivityForResult(Intent(this, MyProfileActivity::class.java), MY_PROFILE_REQUEST_CODE)
            }
            R.id.nav_sign_out -> {
                FirebaseAuth.getInstance().signOut()

                mSharedPreferences.edit().clear().apply()


                val intent = Intent(this@MainActivity, IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
        binding?.drawerLayout?.closeDrawer(GravityCompat.START)

        return true
    }

    fun updateNavigationUserDetails(user: User, readBoardsList: Boolean) {
        hideProgressDialog()
        val headerView = binding?.navView?.getHeaderView(0)
        val userImage: ImageView? = headerView?.findViewById(R.id.nav_user_image)
        val userName: TextView? = headerView?.findViewById(R.id.tv_username)

        mUserName = user.name

        Glide
            .with(this)
            .load(user.image)
            .centerInside()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(userImage!!)

        userName?.text = user.name

        if (readBoardsList) {
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().getBoardsList(this)
        }
    }

    fun tokenUpdateSuccess() {
        hideProgressDialog()
        val editor: SharedPreferences.Editor = mSharedPreferences.edit()
        editor.putBoolean(Constants.FCM_TOKEN_UPDATED, true)
        editor.apply()
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().loadUserData(this, true)
    }

    private fun updateFCMToken(token: String) {
        val userHashMap = HashMap<String, Any>()
        userHashMap[Constants.FCM_TOKEN]
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().updateUserProfileData(this, userHashMap)
    }
}