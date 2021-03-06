package local.nicolas.letsfan;

import local.nicolas.letsfan.auth.AuthUI;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static android.app.PendingIntent.getActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import local.nicolas.letsfan.auth.ui.ResultCodes;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // class variables
    private static final String TAG = "MainActivity";

    // Google and Firebase
    private FirebaseAuth mFirebaseAuth;
    private static final int RC_SIGN_IN = 9001;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference userRef;
    private DatabaseReference inviRef;

    private TextView mNavUserName;
    private TextView mNavUserMail;
    private ImageView mUserAvatar;
    private Button mNavSignInButton;
    private Toolbar toolbar;
    private FloatingActionButton fab;
    private DrawerLayout drawer;
    private NavigationView navigationView;

    private ActionBarDrawerToggle toggle;
    private View mRootView;

    private User currentUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Authentication
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        userRef = mFirebaseDatabase.getReference("users");
        inviRef = mFirebaseDatabase.getReference("invitations");

        // UI binding
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        fab = (FloatingActionButton) findViewById(R.id.floatingActionButton2);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        mRootView = navigationView.getRootView();

        setSupportActionBar(toolbar);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, CreateInvitationActivity.class));
            }
        });

        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                updateNav();

                return;
            }
        };
        drawer.addDrawerListener(toggle);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @OnClick(R.id.sign_in_button)
    public void signIn(View view) {
        List<AuthUI.IdpConfig> providerList = new ArrayList();
        providerList.add(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build());
        providerList.add(new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build());
        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                        .setTheme(R.style.AppTheme)
                        .setLogo(R.drawable.ic_restaurant_menu_black_24dp)
                        .setProviders(providerList)
                        .setTosUrl("https://github.com/SPEITCoder/let-s-fan")
                        .setIsSmartLockEnabled(true)
                        .build(),
                RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check which request we're responding to
        if (requestCode == RC_SIGN_IN) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                updateNav();
                Snackbar.make(mRootView, "Signed in successfully.", Snackbar.LENGTH_LONG).show();
                Query currentUserQuery = userRef.equalTo(mFirebaseAuth.getCurrentUser().getUid());
                if ( !currentUserQuery.equals(mFirebaseAuth.getCurrentUser().getUid())) {
                    // prompt to create new user
                    Log.d(TAG, "onActivityResult: Create user in database.");
                    // temporary solution
                    String localNickName = mFirebaseAuth.getCurrentUser().getDisplayName();
                    String localEmail = mFirebaseAuth.getCurrentUser().getEmail();
                    currentUser = new User(localNickName, "Nicolas", "YING", localEmail, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, true);
                    currentUser.createUserInDatabase(mFirebaseDatabase, mFirebaseAuth.getCurrentUser().getUid());
                }
                return;
            } else if (resultCode == RESULT_CANCELED) {
                Snackbar.make(mRootView, "Signed in cancelled.", Snackbar.LENGTH_LONG).show();
                return;
            } else if (resultCode == ResultCodes.RESULT_NO_NETWORK) {
                Snackbar.make(mRootView, "Signed in failed for no internet.", Snackbar.LENGTH_LONG).show();
                return;
            } else {
                Snackbar.make(mRootView, "Unknown response.", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        toggle.syncState();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        // Bind UI
        mNavUserName = (TextView) findViewById(R.id.nav_head_user_name);
        mNavUserMail = (TextView) findViewById(R.id.nav_header_user_mail);
        mUserAvatar = (ImageView) findViewById(R.id.userAvatar);
        mNavSignInButton = (Button) findViewById(R.id.sign_in_button);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        navigationView.getMenu().clear();
        navigationView.inflateMenu(R.menu.activity_main_drawer);
//
//        if (mFirebaseAuth.getCurrentUser() == null) {
//            MenuItem temp = navigationView.getMenu().getItem(R.id.nav_initiate_invitation);
//            temp.setCheckable(false);
//            navigationView.getMenu().getItem(R.id.nav_manage_user_profile).setCheckable(false);
//            navigationView.getMenu().getItem(R.id.nav_section_sign_out).setCheckable(false);
//        } else {
//            navigationView.getMenu().getItem(R.id.nav_initiate_invitation).setCheckable(true);
//            navigationView.getMenu().getItem(R.id.nav_manage_user_profile).setCheckable(true);
//            navigationView.getMenu().getItem(R.id.nav_section_sign_out).setCheckable(true);
//        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_initiate_invitation) {

        } else if (id == R.id.nav_open_invitation_list) {

        } else if (id == R.id.nav_manage_user_profile) {

        } else if (id == R.id.nav_settings) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_help) {

        } else if (id == R.id.nav_sign_out) {

            signOut();
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @OnClick(R.id.nav_sign_out)
    public void signOut() {
        AuthUI.getInstance().signOut(this).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    updateNav();
                } else {
                    Snackbar.make(mRootView, "Sign out failed.", Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void updateNav() {
        if(mFirebaseAuth.getCurrentUser() != null) {
            if (mFirebaseAuth.getCurrentUser().getPhotoUrl() != null) {
                Glide.with(this)
                        .load(mFirebaseAuth.getCurrentUser().getPhotoUrl())
                        .fitCenter()
                        .into(mUserAvatar);
            }

            mNavUserMail.setText(
                    TextUtils.isEmpty(mFirebaseAuth.getCurrentUser().getEmail()) ? "No email" : mFirebaseAuth.getCurrentUser().getEmail());
            mNavUserName.setText(
                    TextUtils.isEmpty(mFirebaseAuth.getCurrentUser().getDisplayName()) ? "No display name" : mFirebaseAuth.getCurrentUser().getDisplayName());

            mNavUserMail.setVisibility(View.VISIBLE);
            mNavSignInButton.setVisibility(View.GONE);
            mNavUserName.setVisibility(View.VISIBLE);
            mUserAvatar.setVisibility(View.VISIBLE);
        } else {
            mNavUserMail.setVisibility(View.GONE);
            mNavSignInButton.setVisibility(View.VISIBLE);
            mNavUserName.setVisibility(View.GONE);
            mUserAvatar.setVisibility(View.GONE);

            mNavSignInButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    signIn(v);
                    drawer.closeDrawer(GravityCompat.START);
                }
            });
        }
    }


    @Override
    public void onStop() {
        super.onStop();
    }
}
