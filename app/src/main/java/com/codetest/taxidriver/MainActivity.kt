package com.codetest.taxidriver

import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.content.pm.PackageManager
import android.database.MatrixCursor
import android.os.Bundle
import android.os.Handler
import android.provider.BaseColumns
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.animation.AnimationUtils
import android.widget.CursorAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.codetest.taxidriver.adapter.CustomerAdapter
import com.codetest.taxidriver.databinding.ActivityMainBinding
import com.codetest.taxidriver.model.Customer
import com.codetest.taxidriver.model.CustomerFullModel
import com.codetest.taxidriver.model.Person
import com.codetest.taxidriver.utils.Constant
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.platform.MaterialFadeThrough
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.ktx.Firebase
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var preferences: SharedPreferences
    private lateinit var editor: Editor
    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var searchView: SearchView
    private lateinit var cursorAdapter: SimpleCursorAdapter
    private lateinit var customers: MutableList<Customer>
    private lateinit var adapter: CustomerAdapter
    private lateinit var person: Person
    private lateinit var currentUser: FirebaseUser
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private var driverName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        with(window){
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            enterTransition = MaterialFadeThrough()
            exitTransition = MaterialFadeThrough()
        }

        setContentView(binding.root)

        requestPermission()

        //initialize firestore
        initializeFireStore()

        //load customers from firestore
        loadDataFromFireStore()

        val drawerLayout = binding.drawerLayout
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //navigation view menu item click action
        navigationViewMenuItemAction()

        //load account data
        initializeAccountProfile()

        //swipe refresh layout action
        swipeRefreshLayoutAction()

        // shared preferences initialization
        preferences = getSharedPreferences("shared_pref", Context.MODE_PRIVATE)
        editor = preferences.edit()
    }

    private fun initializeFireStore() {
        val signInOptions = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, signInOptions)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        currentUser = Firebase.auth.currentUser!!
        driverName = currentUser.displayName.toString()
    }

    private fun navigationViewMenuItemAction() {
        binding.navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.log_out -> logOutAccount()
            }
            true
        }
    }

    private fun swipeRefreshLayoutAction() {
        binding.swipeRefreshLayout.isEnabled = true
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = true
            loadDataFromFireStore()
        }
    }

    private fun showLoading() {
        binding.swipeRefreshLayout.isEnabled = false
        binding.loadingLayout.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.loadingLayout.visibility = View.GONE
    }

    private fun loadDataFromFireStore() {
        // show loading layout before customers are loaded
        showLoading()

        customers = mutableListOf<Customer>()
        customers.clear()

        db.collection("customers")
            .get()
            .addOnCompleteListener {
                println("it.isSuccessful: ${it.isSuccessful}")
                if (it.isSuccessful) {
                    for (customer in it.result) {
                        println(customer.data.get("name"))
                        val data = customer.data
                        val name = data.get("name").toString()
                        val phone = data.get("phone").toString()
                        val latLog = data.get("latlog") as GeoPoint
                        val customerPhoto = data.get("customerphoto").toString()
                        customers.add(Customer(name, phone, latLog, customerPhoto))
                    }

                    //check refresh layout
                    if (binding.swipeRefreshLayout.isRefreshing)
                        binding.swipeRefreshLayout.isRefreshing = false

                    // get current location and pass it to adapter to show in items
                    Constant.getCurrentLocation(this, {
                        hideLoading()
                        setUpCustomerRecyclerView(customers, it)
                    })


                }
            }
            .addOnFailureListener {
                //stop refreshing
                if (binding.swipeRefreshLayout.isRefreshing)
                    binding.swipeRefreshLayout.isRefreshing = false

                hideLoading()
                showErrorSnackBar()
            }
    }

    private fun showErrorSnackBar() {
        val snackBar = Snackbar.make(
            window.decorView,
            "Fetching data from server failed!",
            Snackbar.LENGTH_SHORT
        )
        snackBar.setAction("try again", object : View.OnClickListener {
            override fun onClick(p0: View?) {
                loadDataFromFireStore()
            }
        })
    }

    private fun setUpCustomerRecyclerView(
        customers: MutableList<Customer>,
        currentLocation: LatLng
    ) {

        //set recyclerview layout animation
        val layoutAnim = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_anim)
        binding.customerRv.layoutAnimation = layoutAnim
        binding.customerRv.itemAnimator = DefaultItemAnimator()

        adapter = CustomerAdapter(this, driverName, currentLocation)
        binding.customerRv.layoutManager = LinearLayoutManager(this)
        binding.customerRv.setHasFixedSize(true)
        binding.customerRv.adapter = adapter

        //sort customers by distance
        customers.sortBy {
            Constant.getDistance(currentLocation,LatLng(it.latLog.latitude,it.latLog.longitude))
        }
        adapter.submitList(customers)

        //search view action
        val from = arrayOf(SearchManager.SUGGEST_COLUMN_TEXT_1)
        val to = intArrayOf(android.R.id.text1)
        cursorAdapter = SimpleCursorAdapter(
            this,
            android.R.layout.simple_list_item_1,
            null,
            from,
            to,
            CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        )
    }

    private fun initializeAccountProfile() {
        try {
            person = intent.extras!!.getSerializable(Constant.PERSON) as Person
        } catch (e: Exception) {
            person = Person(
                currentUser.displayName.toString(), currentUser.photoUrl.toString(),
                currentUser.email.toString()
            )
        }
        val navigationView = binding.navigationView
        val header = navigationView.getHeaderView(0)
        val profileImg = header.findViewById(R.id.profile_img) as ImageView
        val name = header.findViewById(R.id.account_name) as TextView
        val email = header.findViewById(R.id.account_email) as TextView

        profileImg.load(person.profileUrl) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }
        name.text = person.name
        email.text = person.email
    }

    private fun logOutAccount() {
        // close drawer
        binding.drawerLayout.close()

        Constant.showMaterialDialog(
            this,
            "Log Out",
            "Are you sure you want to log out?",
            "no",
            "yes",
            null,
            object : DialogInterface.OnClickListener {
                override fun onClick(p0: DialogInterface?, p1: Int) {
                    // log out account
                    googleSignInClient.signOut().addOnCompleteListener {
                        if (it.isSuccessful) {
                            //sign out firebase
                            firebaseAuth.signOut()

                            // saved log out state to shared preferences
                            editor.putBoolean(Constant.LOGGED_IN_CONSTANT, false)
                            editor.commit()

                            //exit app
                            finishAffinity()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Sign out failed!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        )

    }

    private fun searchViewAction() {
        val cursor = MatrixCursor(arrayOf(BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1))

        // add person names to cursor
        for (customer in customers) {
            cursor.addRow(arrayOf(customers.indexOf(customer), customer.name))
        }

        try {
            cursorAdapter.changeCursor(cursor)
            searchView.suggestionsAdapter = cursorAdapter
            searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
                override fun onSuggestionSelect(position: Int): Boolean {
                    println(position)
                    return true
                }

                override fun onSuggestionClick(position: Int): Boolean {
                    filter(customers.get(position).name)
                    searchView.setQuery(customers.get(position).name, true)

                    return true
                }

            })

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    filter(query)
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    filter(newText)
                    return true
                }

            })
        } catch (e: Exception) {
        }
    }

    private fun filter(newText: String?) {
        val filteredList = mutableListOf<Customer>()

        for (cus in customers) {
            if (cus.name.lowercase(Locale.getDefault())
                    .contains(newText!!.lowercase(Locale.getDefault()))
            ) {
                filteredList.add(cus)
            }
        }

        if (filteredList.isEmpty()) {
            Toast.makeText(this, "$newText is not found", Toast.LENGTH_SHORT).show()
        }

        adapter.submitList(filteredList)

    }

    private fun showExitDialog() {
        Constant.showMaterialDialog(
            this,
            "Exit",
            "Are you sure you want to exit?",
            "No",
            "Yes",
            null,
            object : DialogInterface.OnClickListener {
                override fun onClick(p0: DialogInterface?, p1: Int) {
                    finishAffinity()
                }
            }
        )
    }

    override fun onBackPressed() {
        //exit
        showExitDialog()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.option_menu, menu)
        searchView = menu!!.findItem(R.id.search).actionView as SearchView
        return true
    }

    private fun requestPermission(){
        if (!Constant.isLocationEnabled(this)){
            Constant.requestPermission(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size > 0){
            if(requestCode == 100){
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    loadDataFromFireStore()
                }
                else{
                    Constant.showMaterialDialog(
                        this,
                        "Location Permission Required",
                        "You have to allow location permission to check the distance between you and your customer.",
                        "",
                        "Exit",
                        null,
                        object : DialogInterface.OnClickListener{
                            override fun onClick(p0: DialogInterface?, p1: Int) {
                                finishAffinity()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            true
        }
        when (item.itemId) {
            R.id.search -> searchViewAction()
        }
        return super.onOptionsItemSelected(item)
    }
}