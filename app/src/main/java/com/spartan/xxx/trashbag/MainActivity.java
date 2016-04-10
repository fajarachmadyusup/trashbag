package com.spartan.xxx.trashbag;

import android.app.ProgressDialog;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ConnectionCallbacks, OnConnectionFailedListener {

    private static final int SIGN_IN_CODE = 0;
    private static final int PROFILE_PIC_SIZE = 0;
    GoogleApiClient google_api_client;
    GoogleApiAvailability google_api_availability;
    SignInButton signIn_btn;
    private boolean is_signInBtn_clicked;
    private boolean is_intent_inprogress;
    private ConnectionResult connection_result;
    private int request_code;
    ProgressDialog progress_dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //membuat objek Google API Client
        buidNewGoogleApiClient();

        setContentView(R.layout.activity_main);
        custimizeSignBtn();
        setBtnClickListeners();
        progress_dialog = new ProgressDialog(this);
        progress_dialog.setMessage("Signing in....");
    }

    /*create and  initialize GoogleApiClient object to use Google Plus Api.
     While initializing the GoogleApiClient object, request the Plus.SCOPE_PLUS_LOGIN scope.
   */
    //untuk membuat objek Google API Client
    private void buidNewGoogleApiClient() {

        google_api_client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();
    }

    /*Customize sign-in button. The sign-in button can be displayed in
      multiple sizes and color schemes. It can also be contextually
      rendered based on the requested scopes. For example. a red button may
      be displayed when Google+ scopes are requested, but a white button
      may be displayed when only basic profile is requested. Try adding the
      Plus.SCOPE_PLUS_LOGIN scope to see the  difference.
    */
    //melakukan costumize pada button, disini menggunakan button dari API Google
    private void custimizeSignBtn() {

        signIn_btn = (SignInButton) findViewById(R.id.sign_in_button);
        signIn_btn.setSize(SignInButton.SIZE_STANDARD);
        signIn_btn.setScopes(new Scope[]{Plus.SCOPE_PLUS_LOGIN});

    }

    /*
      Set on click Listeners on the sign-in sign-out and disconnect buttons
    */
    //menjadi pendeteksi apakah button sedang diklik
    private void setBtnClickListeners() {
        // Button listeners
        signIn_btn.setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);
    }

    //method ketika program dimulai
    protected void onStart() {
        super.onStart();
        // Add this line to initiate connection
        //membuat koneksi dengan Google API Client
        google_api_client.connect();
    }


    //method ketika program dihentikan
    protected void onStop() {
        super.onStop();

        //memeriksa apakah program sedang terkoneksi dengan Google API Client atau tidak
        if (google_api_client.isConnected()) {
            //jika terkoneksi, maka koneksi akan diputuskan
            google_api_client.disconnect();
        }
    }

    //method ketika program kembali berjalan, setelah sebelumnya dijeda sejenak
    protected void onResume() {
        super.onResume();
        //memastikan jika program sedang terkoneksi dengan Google API Client
        if (google_api_client.isConnected()) {
            //jika sebelumnya sudah terkoneksi, maka koneksi akan dilanjutkan
            google_api_client.connect();
        }
    }

    //method ketika program telah tekoneksi dengan Google API Client
    //ini Method API Google
    //ini akan dieksekusi dan nilai parameternya diisi di Server
    @Override
    public void onConnected(Bundle arg0) {
        is_signInBtn_clicked = false;
        // Get user's information and set it into the layout
        getProfileInfo();
        // Update the UI after signin
        changeUI(true);

    }

    //method ketika program gagal melakukan koneksi dengan Google API Client
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!result.hasResolution()) {
            google_api_availability.getErrorDialog(this, result.getErrorCode(), request_code).show();
            return;
        }
        //jika tidak ada intent yang sedang diproses
        if (!is_intent_inprogress) {

            connection_result = result;

            //jika signInBtn sudah diklik
            if (is_signInBtn_clicked) {
                //berikan pesan error
                resolveSignInError();
            }
        }

    }

    //jika koneksi ditunda
    @Override
    public void onConnectionSuspended(int arg0) {
        //tetap konek tapi, tampilan tak berubah
        google_api_client.connect();
        changeUI(false);
    }

    /**
     * Sign-in into the Google + account
    */
    private void gPlusSignIn() {
        //jika program tidak sedang menghubungkan ke Google API Client
        if (!google_api_client.isConnecting()) {
            Log.d("user connected", "connected");
            is_signInBtn_clicked = true;
            progress_dialog.show();
            resolveSignInError();

        }
    }

    /**
     Method to resolve any signin errors
    */
    private void resolveSignInError() {
        if (connection_result.hasResolution()) {
            try {
                is_intent_inprogress = true;
                connection_result.startResolutionForResult(this, SIGN_IN_CODE);
                Log.d("resolve error", "sign in error resolved");
            } catch (IntentSender.SendIntentException e) {
                Log.d("tes", "tes");
                is_intent_inprogress = false;
                google_api_client.connect();
            }
        }
    }

    /**
     Sign-out from Google+ account
    */
    private void gPlusSignOut() {
        //jika program telah terhubung dengan Google API Client
        if (google_api_client.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(google_api_client);
            google_api_client.disconnect();
            google_api_client.connect();
            changeUI(false);
        }
    }

    /**
     * Revoking access from Google+ account
     * */
    private void gPlusRevokeAccess() {
        if (google_api_client.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(google_api_client);
            Plus.AccountApi.revokeAccessAndDisconnect(google_api_client)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status arg0) {
                            Log.d("MainActivity", "User access revoked!");
                            buidNewGoogleApiClient();
                            google_api_client.connect();
                            changeUI(false);
                        }

                    });
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                Toast.makeText(this, "start sign process", Toast.LENGTH_SHORT).show();
                gPlusSignIn();
                break;
            case R.id.sign_out_button:
                Toast.makeText(this, "Sign Out from G+", Toast.LENGTH_LONG).show();
                gPlusSignOut();

                break;
            case R.id.disconnect_button:
                Toast.makeText(this, "Revoke Access from G+", Toast.LENGTH_LONG).show();
                gPlusRevokeAccess();

                break;
        }
    }

    /*
     Show and hide of the Views according to the user login status
    */
    private void changeUI(boolean signedIn) {
        if (signedIn) {
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
        } else {

            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }

    /**
     * get user's information name, email, profile pic,Date of birth,tag line and about me
     * */
    private void getProfileInfo() {

        try {

            if (Plus.PeopleApi.getCurrentPerson(google_api_client) != null) {
                Person currentPerson = Plus.PeopleApi.getCurrentPerson(google_api_client);
                setPersonalInfo(currentPerson);

            } else {
                Toast.makeText(getApplicationContext(),
                        "No Personal info mention", Toast.LENGTH_LONG).show();

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /*
     set the User information into the views defined in the layout
    */
    private void setPersonalInfo(Person currentPerson){

        String personName = currentPerson.getDisplayName();
        String personPhotoUrl = currentPerson.getImage().getUrl();
        String email = Plus.AccountApi.getAccountName(google_api_client);
        TextView   user_name = (TextView) findViewById(R.id.userName);
        user_name.setText("Name: "+personName);
        TextView gemail_id = (TextView)findViewById(R.id.emailId);
        gemail_id.setText("Email Id: " +email);
        TextView dob = (TextView)findViewById(R.id.dob);
        dob.setText("DOB: "+currentPerson.getBirthday());
        TextView tag_line = (TextView)findViewById(R.id.tag_line);
        tag_line.setText("Tag Line: " +currentPerson.getTagline());
        TextView about_me = (TextView)findViewById(R.id.about_me);
        about_me.setText("About Me: "+currentPerson.getAboutMe());
        setProfilePic(personPhotoUrl);
        progress_dialog.dismiss();
        Toast.makeText(this, "Person information is shown!", Toast.LENGTH_LONG).show();
    }

    /*
     By default the profile pic url gives 50x50 px image.
     If you need a bigger size image we have to change the query parameter value from 50 to the size you want
    */
    private void setProfilePic(String profile_pic){
        profile_pic = profile_pic.substring(0,
                profile_pic.length() - 2)
                + PROFILE_PIC_SIZE;
        ImageView user_picture = (ImageView)findViewById(R.id.profile_pic);
        new LoadProfilePic(user_picture).execute(profile_pic);
    }


    /**
     Perform background operation asynchronously, to load user profile picture with new dimensions from the modified url
     * */
    private class LoadProfilePic extends AsyncTask<String, Void, Bitmap> {
        ImageView bitmap_img;

        public LoadProfilePic(ImageView bitmap_img) {
            this.bitmap_img = bitmap_img;
        }

        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            Bitmap new_icon = null;
            try {
                InputStream in_stream = new java.net.URL(url).openStream();
                new_icon = BitmapFactory.decodeStream(in_stream);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return new_icon;
        }

        protected void onPostExecute(Bitmap result_img) {

            bitmap_img.setImageBitmap(result_img);
        }
    }
}
