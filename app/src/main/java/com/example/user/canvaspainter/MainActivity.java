package com.example.user.canvaspainter;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.drive.CreateFileActivityOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityOptions;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    Panel mPanel;
    Dialog dialog;
    String strFileName = "";

    private static final String TAG = "** google-drive";

    private static final int REQUEST_CODE_SIGN_IN = 0;
    private static final int REQUEST_CODE_CREATOR = 2;
    protected static final int REQUEST_CODE_OPEN_ITEM = 1;

    final String STR_GOOGLE_DRIVE = "googleDrive";
    final String STR_FILE_NAME = "fileName";

    private String STR_GOOGLE_DRIVE_URL = " https://drive.google.com/uc?export=download&id="; //"http://drive.google.com/open?id=";

    private GoogleSignInClient mGoogleSignInClient;
    private DriveClient mDriveClient;
    private DriveResourceClient mDriveResourceClient;

    private TaskCompletionSource<DriveId> mOpenItemTaskSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);

        //畫布
        mPanel = new Panel(this);
        setContentView(mPanel);

        //for google drive
        IntentFilter filter = new IntentFilter();
        filter.addAction(STR_GOOGLE_DRIVE);
        registerReceiver(receiver, filter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about:
                mPanel.resetCanvas();
                break;
            case R.id.menu_save:

//                strFileName = intent.getStringExtra(STR_FILE_NAME);
                signIn();
                break;
            case R.id.menu_qrcode:
                onDriveClientReady();
                break;
            case R.id.menu_quit:
                this.finish();
                break;
            case R.id.menu_backgourd_color:
                break;
            case R.id.menu_pen_color:
                dialog = new Dialog(this);
//              openDialog();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    public void onSave(View view) {

        EditText fileName = (EditText)dialog.findViewById(R.id.txtName);
        mPanel.savePicture(fileName.getText().toString());
        dialog.dismiss();

    }

    public void onCancel(View view) {
        dialog.cancel();
    }

    public void openDialog() {
        dialog.setTitle("save file");
        dialog.setContentView(R.layout.layout_color_palette);
        dialog.show();
    }

    //=============Google Drive=================

    /** Start sign in activity. */
    private void signIn() {
        Log.i(TAG, "Start sign in");
        mGoogleSignInClient = buildGoogleSignInClient();
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    /** Build a Google SignIn client. */
    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_FILE)
                        .build();
        return GoogleSignIn.getClient(this, signInOptions);
    }

    /** Create a new file and save it to Drive. */
    private void saveFileToDrive(final String fileName) {
        // Start by creating a new contents, and setting a callback.
        Log.i(TAG, "Creating new contents.");
        final Bitmap image = mPanel.vBitmap;    //mBitmapToSave;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
//        final Bitmap image = BitmapFactory.decodeFile(fileName, options);

        mDriveResourceClient
                .createContents()
                .continueWithTask(
                        new Continuation<DriveContents, Task<Void>>() {
                            @Override
                            public Task<Void> then(@NonNull Task<DriveContents> task) throws Exception {
                                return createFileIntentSender(task.getResult(), image, fileName);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Failed to create new contents.", e);
                            }
                        });



    }

    /**
     * Creates an {@link IntentSender} to start a dialog activity with configured {@link
     * CreateFileActivityOptions} for user to create a new photo in Drive.
     */
    private Task<Void> createFileIntentSender(DriveContents driveContents, Bitmap image, String fileName) {
        Log.i(TAG, "New contents created.");
        // Get an output stream for the contents.
        OutputStream outputStream = driveContents.getOutputStream();
        // Write the bitmap data from it.
        ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);
        try {
            outputStream.write(bitmapStream.toByteArray());
        } catch (IOException e) {
            Log.w(TAG, "Unable to write file contents.", e);
        }

        // Create the initial metadata - MIME type and title.
        // Note that the user will be able to change the title later.
        MetadataChangeSet metadataChangeSet =
                new MetadataChangeSet.Builder()
                        .setMimeType("image/jpeg")
                        .setTitle(fileName)
                        .build();
        // Set up options to configure and display the create file activity.
        CreateFileActivityOptions createFileActivityOptions =
                new CreateFileActivityOptions.Builder()
                        .setInitialMetadata(metadataChangeSet)
                        .setInitialDriveContents(driveContents)
                        .build();

        return mDriveClient
                .newCreateFileActivityIntentSender(createFileActivityOptions)
                .continueWith(
                        new Continuation<IntentSender, Void>() {
                            @Override
                            public Void then(@NonNull Task<IntentSender> task) throws Exception {
                                startIntentSenderForResult(task.getResult(), REQUEST_CODE_CREATOR, null, 0, 0, 0);

                                return null;
                            }
                        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                Log.i(TAG, "Sign in request code");
                // Called after user is signed in.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Signed in successfully.");
                    // Use the last signed in account here since it already have a Drive scope.
                    mDriveClient = Drive.getDriveClient(this, GoogleSignIn.getLastSignedInAccount(this));
                    if (mDriveClient == null)
                        Log.i(TAG, "mDriveClient == null.");
                    // Build a drive resource client.
                    mDriveResourceClient =
                            Drive.getDriveResourceClient(this, GoogleSignIn.getLastSignedInAccount(this));

                    if (mDriveResourceClient == null)
                        Log.i(TAG, "mDriveResourceClient == null.");

                    saveFileToDrive(strFileName);

                } else
                    Log.i(TAG, "Signed in failed.");
                break;
            case REQUEST_CODE_CREATOR:
                Log.i(TAG, "creator request code");
                // Called after a file is saved to Drive.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Image successfully saved.");
                }
                break;

            case REQUEST_CODE_OPEN_ITEM:
                if (resultCode == RESULT_OK) {
                    DriveId driveId = data.getParcelableExtra(
                            OpenFileActivityOptions.EXTRA_RESPONSE_DRIVE_ID);
                    mOpenItemTaskSource.setResult(driveId);
                } else {
                    mOpenItemTaskSource.setException(new RuntimeException("Unable to open file"));
                }
                break;

        }
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(STR_GOOGLE_DRIVE)) {
                strFileName = intent.getStringExtra(STR_FILE_NAME);
                signIn();

            }
        }
    };

    /**
     * Prompts the user to select a folder using OpenFileActivity.
     *
     * @param openOptions Filter that should be applied to the selection
     * @return Task that resolves with the selected item's ID.
     */
    private Task<DriveId> pickItem(OpenFileActivityOptions openOptions) {
        Log.e(TAG, "pickItem");
        mOpenItemTaskSource = new TaskCompletionSource<>();
        if (mDriveClient == null)
            Log.e(TAG, "mDriveClient is null");
        mDriveClient
                .newOpenFileActivityIntentSender(openOptions)
                .continueWith((Continuation<IntentSender, Void>) task -> {
                    startIntentSenderForResult(
                            task.getResult(), REQUEST_CODE_OPEN_ITEM, null, 0, 0, 0);
                    return null;
                });

        return mOpenItemTaskSource.getTask();
    }

    protected Task<DriveId> pickPicFile() {
        Log.e(TAG, "pick pic file");
        OpenFileActivityOptions openOptions =
                new OpenFileActivityOptions.Builder()
                        .setSelectionFilter(Filters.eq(SearchableField.MIME_TYPE, "image/png"))
                        .setActivityTitle(getString(R.string.select_file))
                        .build();
        return pickItem(openOptions);
    }

    protected void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    protected void onDriveClientReady() {
        Log.e(TAG, "onDriveClientReady");

        pickPicFile()
                .addOnSuccessListener(this,
                        driveId -> retrieveContents(driveId.asDriveFile()))
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "No file selected", e);
                    showMessage(getString(R.string.file_not_selected));
                    finish();
                });
    }

    private void retrieveContents(DriveFile file) {
        Log.e(TAG, "retrieveContents");
        // [START open_file]
        Task<DriveContents> openFileTask =
                mDriveResourceClient.openFile(file, DriveFile.MODE_READ_ONLY);
        // [END open_file]
        // [START read_contents]
        openFileTask
                .continueWithTask(task -> {
                    DriveContents contents = task.getResult();
                    // Process contents...
                    // [START_EXCLUDE]
                    // [START read_as_string]
                    showMessage(getString(R.string.content_loaded));

//                    Log.e("** list driver id", task.getResult().getDriveId().toString());
                    mPanel.showQRCodeDlg(STR_GOOGLE_DRIVE_URL+task.getResult().getDriveId().getResourceId());
                    // [END read_as_string]
                    // [END_EXCLUDE]
                    // [START discard_contents]
                    Task<Void> discardTask = mDriveResourceClient.discardContents(contents);
                    // [END discard_contents]
                    return discardTask;
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                    // [START_EXCLUDE]
                    Log.e(TAG, "Unable to read contents", e);
                    showMessage(getString(R.string.read_failed));
                    finish();
                    // [END_EXCLUDE]
                });
        // [END read_contents]
    }

}