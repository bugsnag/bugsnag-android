package com.bugsnag.android.example;

import static android.widget.Toast.LENGTH_SHORT;

import com.bugsnag.android.BeforeNotify;
import com.bugsnag.android.BreadcrumbType;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Callback;
import com.bugsnag.android.Error;
import com.bugsnag.android.MetaData;
import com.bugsnag.android.Report;
import com.bugsnag.android.Severity;
import com.bugsnag.android.other.CrashyClass;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExampleActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bugsnag.setAutoCaptureSessions(true);
        setContentView(R.layout.main);
        setupToolbarLogo();
        performAdditionalBugsnagSetup();

        findViewById(R.id.btn_fatal_crash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                crashUnhandled(view);
            }
        });
    }

    private void performAdditionalBugsnagSetup() {
        // Execute some code before every bugsnag notification
        Bugsnag.beforeNotify(new BeforeNotify() {
            @Override
            public boolean run(Error error) {
                System.out.println(String.format("In beforeNotify - %s", error.getExceptionName()));
                return true; // if you do not wish to send this error, return false here.
            }
        });

        // Set the global user information
        Bugsnag.setUser("123456", "joebloggs@example.com", "Joe Bloggs");

        // Add some global metaData
        Bugsnag.addToTab("user", "age", 31);

        // Mark the following packages as part of your app
        Bugsnag.setProjectPackages("com.bugsnag.android.example", "com.bugsnag.android.other");
    }

    /**
     * Throws an unhandled Exception. Bugsnag will automatically capture any uncaught exceptions
     * in your app and send an error report.
     */
    public void crashUnhandled(View view) {
        throw CrashyClass.crash("Fatal Crash");
    }

    /**
     * You can call {@link Bugsnag#notify(Throwable)} to send an error report for exceptions
     * which are already handled by your app.
     */
    public void crashHandled(View view) {
        try {
            throw new RuntimeException("Non-Fatal Crash");
        } catch (RuntimeException exception) {
            Bugsnag.notify(exception);
        }
        displayToastNotification();
    }

    /**
     * The severity of error reports can be altered. This can be useful for capturing handled
     * exceptions which occur often but are not visible to the user.
     */
    public void crashWithCustomSeverity(View view) {
        RuntimeException exception = new RuntimeException("Error Report with altered Severity");
        Bugsnag.notify(exception, Severity.INFO);
        displayToastNotification();
    }

    /**
     * User details can be added globally, which will then appear in all error reports sent
     * to the Bugsnag dashboard.
     */
    public void crashWithUserDetails(View view) {
        Bugsnag.setUser("123456", "joebloggs@example.com", "Joe Bloggs");
        RuntimeException exception = new RuntimeException("Error Report with User Info");
        Bugsnag.notify(exception);
        displayToastNotification();
    }

    /**
     * Additional metadata can be attached to crash reports. This can be achieved by calling
     * {@link Bugsnag#notify(Throwable, MetaData)}, as shown below, or registering a global callback
     * with {@link Bugsnag#beforeNotify(BeforeNotify)} that adds metadata to the report.
     */
    public void crashWithMetadata(View view) {
        RuntimeException exception = new RuntimeException("Error report with Additional Metadata");
        MetaData metaData = generateUserMetaData();

        Bugsnag.notify(exception, Severity.ERROR, metaData);
        displayToastNotification();
    }

    /**
     * Breadcrumbs help track down the cause of an error, by displaying events that happened leading
     * up to a crash. You can log your own breadcrumbs which will display on the Bugsnag Dashboard -
     * activity lifecycle callbacks and system intents are also captured automatically.
     */
    public void crashWithBreadcrumbs(View view) {
        Bugsnag.leaveBreadcrumb("LoginButtonClick");

        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("reason", "Incorrect password");
        Bugsnag.leaveBreadcrumb("WebAuthFailure", BreadcrumbType.ERROR, metadata);

        RuntimeException exception = new RuntimeException("Error Report with Breadcrumbs");
        Bugsnag.notify(exception);
        displayToastNotification();
    }

    /**
     * When sending a handled error, a callback can be registered, which allows the Error Report
     * to be modified before it is sent.
     */
    public void crashWithCallback(View view) {
        RuntimeException exception = new RuntimeException("Customized Error Report");

        Bugsnag.notify(exception, new Callback() {
            @Override
            public void beforeNotify(Report report) { // modify the report
                report.getError().setMetaData(generateUserMetaData());
            }
        });
        displayToastNotification();
    }

    private void displayToastNotification() {
        Toast.makeText(this, "Error Report Sent!", LENGTH_SHORT).show();
    }

    private void setupToolbarLogo() {
        ActionBar supportActionBar = getSupportActionBar();

        if (supportActionBar != null) {
            supportActionBar.setDisplayShowHomeEnabled(true);
            supportActionBar.setIcon(R.drawable.ic_bugsnag_svg);
            supportActionBar.setTitle(null);
        }
    }

    /**
     * Reads the android bugsnag docs
     * 
     * @param view the XML layout view
     */
    public void readDocs(View view) {
        Uri uri = Uri.parse("https://docs.bugsnag.com/platforms/android/sdk/");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    private MetaData generateUserMetaData() {
        Map<String, String> userDetails = new HashMap<>();
        userDetails.put("playerName", "Joe Bloggs the Invincible");

        MetaData metaData = new MetaData();
        metaData.addToTab("CustomMetaData", "HasLaunchedGameTutorial", true);
        metaData.addToTab("CustomMetaData", "UserDetails", userDetails);

        List<String> completedLevels =
            Arrays.asList("Level 1 - The Beginning", "Level 2 - Tower Defence");
        metaData.addToTab("CustomMetaData", "CompletedLevels", completedLevels);
        return metaData;
    }

    public void sendErrorWithCallback(Callback callback) {
        Bugsnag.notify(new RuntimeException(), callback);
    }
}
