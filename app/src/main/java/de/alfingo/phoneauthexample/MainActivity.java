package de.alfingo.phoneauthexample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_CODE = 1337;

    private static final String KEY_PHONE_NUMBER = "VERIFICATION_PROGRESS";
    private static final String KEY_VERIFICATION_ID = "VERIFICATION_ID";
    private static final String KEY_RESEND_TOKEN = "RESEND_TOKEN";

    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = getCallbacks();

    FirebaseAuth mAuth;

    private String mPhoneNumber;
    private String mVerificationId;

    private TextView mErrorTextView;
    private EditText mPhoneNumberEditText;
    private EditText mVerificationCodeEditText;
    private Button mConfirmButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mErrorTextView = findViewById(R.id.tv_error);
        mVerificationCodeEditText = ((TextInputLayout) findViewById(R.id.til_code))
                .getEditText();
        mPhoneNumberEditText = ((TextInputLayout) findViewById(R.id.til_number))
                .getEditText();
        mConfirmButton = findViewById(R.id.btn_confirm);

        mAuth = FirebaseAuth.getInstance();

        // restores values from state
        if (savedInstanceState != null) {
            mPhoneNumber = savedInstanceState.getString(KEY_PHONE_NUMBER);
            mVerificationId = savedInstanceState.getString(KEY_VERIFICATION_ID);
        }

        // fills in the phone number
        if (mPhoneNumber != null) {
            mPhoneNumberEditText.setText(mPhoneNumber);
        } else {
            if (ActivityCompat
                    .checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},
                            REQUEST_CODE);
                }
            } else {
                fillInNumber();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            fillInNumber();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mPhoneNumber != null && mVerificationId == null)
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    mPhoneNumber,
                    60,
                    TimeUnit.SECONDS,
                    this,
                    mCallbacks
            );
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_PHONE_NUMBER, mPhoneNumber);
        outState.putString(KEY_VERIFICATION_ID, mVerificationId);
    }

    /**
     * Fills the phone number information.
     */
    private void fillInNumber() {
        TelephonyManager tMgr = (TelephonyManager)
                this.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED) {
            @SuppressWarnings("ConstantConditions") @SuppressLint("HardwareIds")
            String number = tMgr.getLine1Number();
            mPhoneNumberEditText.setText(number);
        }
    }

    public void onConfirmationClick(View clickedView) {
        if (mPhoneNumber != null && mVerificationId != null) {
            String code = mVerificationCodeEditText.getText().toString().trim();
            if (!code.isEmpty()) {
                PhoneAuthCredential credential = PhoneAuthProvider
                        .getCredential(mVerificationId, code);
                signInWithPhoneAuthCredential(credential);
            } else {
                Toast.makeText(this, "Write the code first!",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (mPhoneNumber == null) {
            String phoneNumber = mPhoneNumberEditText.getText().toString().trim();
            if (!phoneNumber.isEmpty()) {
                mPhoneNumber = phoneNumber;
                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                        mPhoneNumber,
                        60,
                        TimeUnit.SECONDS,
                        this,
                        mCallbacks
                );
                mPhoneNumberEditText.setEnabled(false);
            } else {
                Toast.makeText(this, "Please write a phone number first.",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Be patient.", Toast.LENGTH_SHORT).show();
        }
    }

    public void onRestartClick(View clickedView) {
        Intent restartIntent = new Intent(this, MainActivity.class);
        startActivity(restartIntent);
        finish();
    }


    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        // FIXME: 30.09.2017 Returns that no account could be found. ERROR.

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");

                            mErrorTextView.setText(R.string.everything_okay);
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() != null)
                                mErrorTextView.setText(task.getException().getLocalizedMessage());

                            mVerificationCodeEditText.setEnabled(true);
                        }
                    }
                });
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks getCallbacks() {
        return new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted:" + credential);

                mVerificationCodeEditText.setText(credential.getSmsCode());
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Log.w(TAG, "onVerificationFailed", e);

                mErrorTextView.setText(e.getLocalizedMessage());

                mPhoneNumber = null;
                mVerificationId = null;
            }

            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                Log.d(TAG, "onCodeSent:" + verificationId);

                mVerificationCodeEditText.setEnabled(true);
                mConfirmButton.setText(R.string.confirm_code);

                mVerificationId = verificationId;
            }
        };
    }


}
