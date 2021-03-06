/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package local.nicolas.letsfan.auth.util.signincontainer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import local.nicolas.letsfan.auth.AuthUI;
import local.nicolas.letsfan.auth.IdpResponse;
import local.nicolas.letsfan.auth.provider.AuthCredentialHelper;
import local.nicolas.letsfan.auth.provider.GoogleProvider;
import local.nicolas.letsfan.auth.provider.IdpProvider;
import local.nicolas.letsfan.auth.provider.IdpProvider.IdpCallback;
import local.nicolas.letsfan.auth.ui.BaseFragment;
import local.nicolas.letsfan.auth.ui.ExtraConstants;
import local.nicolas.letsfan.auth.ui.FlowParameters;
import local.nicolas.letsfan.auth.ui.FragmentHelper;
import local.nicolas.letsfan.auth.ui.TaskFailureLogger;
import local.nicolas.letsfan.auth.ui.idp.CredentialSignInHandler;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class IdpSignInContainer extends BaseFragment implements IdpCallback {
    private static final String TAG = "IDPSignInContainer";
    private static final int RC_WELCOME_BACK_IDP = 4;

    private IdpProvider mIdpProvider;
    @Nullable private SaveSmartLock mSaveSmartLock;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSaveSmartLock = mHelper.getSaveSmartLockInstance(getActivity());
        String email = getArguments().getString(ExtraConstants.EXTRA_EMAIL);
        String provider = getArguments().getString(ExtraConstants.EXTRA_PROVIDER);
        AuthUI.IdpConfig providerConfig = null;
        for (AuthUI.IdpConfig config : mHelper.getFlowParams().providerInfo) {
            if (config.getProviderId().equalsIgnoreCase(provider)) {
                providerConfig = config;
                break;
            }
        }

        if (providerConfig == null) {
            // we don't have a provider to handle this
            finish(Activity.RESULT_CANCELED, new Intent());
            return;
        }

        if (provider.equalsIgnoreCase(GoogleAuthProvider.PROVIDER_ID)) {
            mIdpProvider = new GoogleProvider(getActivity(), providerConfig, email);
        }

        mIdpProvider.setAuthenticationCallback(this);
        mIdpProvider.startLogin(getActivity());
    }

    @Override
    public void onSuccess(final IdpResponse response) {
        Intent data = new Intent();
        data.putExtra(ExtraConstants.EXTRA_IDP_RESPONSE, response);
        AuthCredential credential = AuthCredentialHelper.getAuthCredential(response);
        final FirebaseAuth firebaseAuth = mHelper.getFirebaseAuth();
        Task<AuthResult> authResultTask = firebaseAuth.signInWithCredential(credential);
        authResultTask
                .addOnFailureListener(
                        new TaskFailureLogger(TAG, "Failure authenticating with credential"))
                .addOnCompleteListener(new CredentialSignInHandler(
                        getActivity(),
                        mHelper,
                        mSaveSmartLock,
                        RC_WELCOME_BACK_IDP,
                        response));
    }

    @Override
    public void onFailure(Bundle extra) {
        finish(Activity.RESULT_CANCELED, new Intent());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_WELCOME_BACK_IDP) {
            finish(resultCode, data);
        } else {
            mIdpProvider.onActivityResult(requestCode, resultCode, data);
        }
    }

    public static void signIn(FragmentActivity activity,
                              FlowParameters parameters,
                              String email,
                              String provider) {
        FragmentManager fm = activity.getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(TAG);
        if (fragment == null || !(fragment instanceof IdpSignInContainer)) {
            IdpSignInContainer result = new IdpSignInContainer();

            Bundle bundle = FragmentHelper.getFlowParamsBundle(parameters);
            bundle.putString(ExtraConstants.EXTRA_EMAIL, email);
            bundle.putString(ExtraConstants.EXTRA_PROVIDER, provider);
            result.setArguments(bundle);

            try {
                fm.beginTransaction().add(result, TAG).disallowAddToBackStack().commit();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Cannot add fragment", e);
            }
        }
    }

    public static IdpSignInContainer getInstance(FragmentActivity activity) {
        Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(TAG);
        if (fragment != null && fragment instanceof IdpSignInContainer) {
            return (IdpSignInContainer) fragment;
        } else {
            return null;
        }
    }
}
