package com.codetest.taxidriver

import android.animation.Animator
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender.OnFinished
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import androidx.core.app.ActivityOptionsCompat
import com.codetest.taxidriver.databinding.ActivityLoginBinding
import com.codetest.taxidriver.model.Person
import com.codetest.taxidriver.utils.Constant
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSinInClient: GoogleSignInClient
    private val RC_SIGN_IN = 100
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var progressDialog: ProgressDialog

    //shared preferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)

        sharedPreferences = getSharedPreferences("shared_pref", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        // initialize progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setCancelable(false)

        //initialize firebase auth
        auth = Firebase.auth

        //initialize google login
        initializeGoogleSignIn()

        setContentView(binding.root)

        binding.loginBtn.setOnClickListener{
            logInWithGoogle()
        }

    }

    private fun showProgressDialog(){
        progressDialog.setMessage("Logging in...")
        progressDialog.show()
    }

    private fun hideProgressDialog(){
        if (progressDialog != null && progressDialog.isShowing){
            progressDialog.dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        //check whether the user is signed in or not
        val isLoggedIn = sharedPreferences.getBoolean(Constant.LOGGED_IN_CONSTANT,false)
        if (isLoggedIn){
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun initializeGoogleSignIn() {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSinInClient = GoogleSignIn.getClient(this,signInOptions)
    }

    private fun logInWithGoogle() {

        //show progress dialog
        showProgressDialog()

        val intent = googleSinInClient.signInIntent
        startActivityForResult(intent,RC_SIGN_IN)
    }

    private fun revealAnimation(onFinished: () -> Unit) {
        binding.reveal.visibility = View.VISIBLE

        val cx = binding.reveal.width
        val cy = binding.reveal.height

        val x = (binding.reveal.width/2)
        val y = (binding.reveal.height/2)

        val finalRadius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()

        val revealAnim = ViewAnimationUtils
            .createCircularReveal(binding.reveal,x,y, 56f, finalRadius)

        revealAnim.addListener(object : Animator.AnimatorListener{
            override fun onAnimationStart(p0: Animator?) {

            }

            override fun onAnimationEnd(p0: Animator?) {
                onFinished()
            }

            override fun onAnimationCancel(p0: Animator?) {

            }

            override fun onAnimationRepeat(p0: Animator?) {

            }

        })

        revealAnim.setDuration(400)
        revealAnim.start()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        println("requestCode: $requestCode")
        if (requestCode == RC_SIGN_IN){
            val accountTask = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = accountTask.getResult(ApiException::class.java)
                firebaseAuthWithGoogleAccount(account)
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    private fun goToMainActivity(person: Person){
        //hide progress dialog
        hideProgressDialog()

        //reveal anim
        revealAnimation({
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this)
            val intent = Intent(this,MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra(Constant.PERSON,person)
            startActivity(intent,options.toBundle())
            binding.reveal.visibility = View.INVISIBLE
        })
    }

    private fun firebaseAuthWithGoogleAccount(account: GoogleSignInAccount) {
        val person = Person(account.displayName.toString(),account.photoUrl.toString(), account.email.toString())

        val credential = GoogleAuthProvider.getCredential(account.idToken,null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this){ task ->
                if (task.isSuccessful){

                    // saved signed in state to shared preferences
                    editor.putBoolean(Constant.LOGGED_IN_CONSTANT,true)
                    editor.commit()

                    //successfully signed in
                    goToMainActivity(person)
                }
                else{
                    // signed in failed
                    showLogInFailedSnackBar()
                }
            }
    }

    private fun showLogInFailedSnackBar() {
        val builder = MaterialAlertDialogBuilder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.loading_failed_dialog,null,false)
        builder.setView(view)
        val dialog = builder.create()
        val retryBtn = view.findViewById(R.id.retry_btn) as MaterialCardView

        retryBtn.setOnClickListener {
            logInWithGoogle()

            if (dialog.isShowing)
                dialog.dismiss()
        }
        dialog.show()
    }
}