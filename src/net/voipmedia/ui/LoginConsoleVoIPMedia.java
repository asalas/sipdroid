package net.voipmedia.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONObject;
import org.sipdroid.sipua.R;
import org.sipdroid.sipua.ui.Settings;
import org.sipdroid.sipua.ui.Sipdroid;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by asalas on 12/01/2016.
 */
public class LoginConsoleVoIPMedia extends Activity
{
    // CONSTANTS
    private static final String ARG_USERNAME = "Username";
    private static final String ARG_PASSWORD = "Password";

    private static final String RESPONSE_OK = "OK";

    InputStream inputStream;
    Properties consoleProps;

    private AsyncHttpClient restClient;
    private RequestParams restRequestParams;
    private Map<String, Object> subscriberConsoleMap;
    private ProgressDialog progressDialog;

    private String AUTH_USER;
    private String AUTH_PASSWORD;
    private String PATH_GET_SUBSCRIBER;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.login_console);

        try
        {
            inputStream = getResources().openRawResource(R.raw.console_voipmedia);
            consoleProps = new Properties();
            consoleProps.load(inputStream);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        this.progressDialog = new ProgressDialog(this);
        this.progressDialog.setTitle(getString(R.string.authenticating));
        this.progressDialog.setMessage(getString(R.string.authenticating));
        this.progressDialog.setCancelable(false);

        initSignInButton();
    }

    private void initSignInButton()
    {
        final Button signInButton = (Button)this.findViewById(R.id.signInButton);
        signInButton.setEnabled(true);

        signInButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                final EditText userNameField = (EditText)findViewById(R.id.userNameField);
                final EditText passwordField = (EditText)findViewById(R.id.passwordField);

                progressDialog.show();

                final String login = userNameField.getText().toString();
                final String password = passwordField.getText().toString();

                // Invoke the WS ConsoleVoIPMedia/REST
                AUTH_USER = consoleProps.getProperty("net.voipmedia.console.auth.user");
                AUTH_PASSWORD = consoleProps.getProperty("net.voipmedia.console.auth.password");
                PATH_GET_SUBSCRIBER = consoleProps.getProperty("net.voipmedia.console.path.get.subscriber");

                restRequestParams = new RequestParams();
                restRequestParams.put("username", login);
                restRequestParams.put("password", password);

                restClient = new AsyncHttpClient();
                restClient.setTimeout(4000);
                restClient.setBasicAuth(AUTH_USER, AUTH_PASSWORD);
                restClient.setAuthenticationPreemptive(true);

                // TODO: aqui tiene que estar la invocacion a Application para guardar el mapa de resultados

                restClient.get(PATH_GET_SUBSCRIBER, restRequestParams, new JsonHttpResponseHandler()
                {
                    @Override
                    public void onSuccess(int i, Header[] headers, JSONObject response)
                    {
                        // clean the form data
                        ((EditText)findViewById(R.id.userNameField)).setText("");
                        ((EditText)findViewById(R.id.passwordField)).setText("");

                        // Hide the Progress Dialog
                        progressDialog.hide();
                        try
                        {
                            String statusCode = response.getString("statusCode");
                            if (statusCode.equals(RESPONSE_OK))
                            {
                                JSONObject jsonSubscriber = response.getJSONObject("subscriber");
                                int userIdConsole;
                                try
                                {
                                    userIdConsole = jsonSubscriber.getInt(SubscriberConsole.USER_ID_CONSOLE);
                                }
                                catch(Exception e)
                                {
                                    userIdConsole = 0;
                                }
                                subscriberConsoleMap = new HashMap<String, Object>();
                                subscriberConsoleMap.put(SubscriberConsole.USER_NAME, jsonSubscriber.getString(SubscriberConsole.USER_NAME));
                                subscriberConsoleMap.put(SubscriberConsole.DOMAIN, jsonSubscriber.getString(SubscriberConsole.DOMAIN));
                                subscriberConsoleMap.put(SubscriberConsole.PASSWORD, jsonSubscriber.getString(SubscriberConsole.PASSWORD));
                                subscriberConsoleMap.put(SubscriberConsole.USER_ID_CONSOLE, userIdConsole);
                                subscriberConsoleMap.put(SubscriberConsole.PBX_HOST, jsonSubscriber.getString(SubscriberConsole.PBX_HOST));
                                subscriberConsoleMap.put(SubscriberConsole.HA1B, jsonSubscriber.getBoolean(SubscriberConsole.HA1B));

                                try
                                {
                                    Settings sipDroidSetting = new Settings();
                                    sipDroidSetting.changePreferenceByLoginConsole(subscriberConsoleMap);

                                    // Success dialog
                                    AlertDialog.Builder builderSuccessDialog = new AlertDialog.Builder(LoginConsoleVoIPMedia.this);
                                    builderSuccessDialog.setMessage(R.string.success_login_console);
                                    builderSuccessDialog.setCancelable(true);

                                    builderSuccessDialog.setPositiveButton(
                                            R.string.accept,
                                            new DialogInterface.OnClickListener()
                                            {
                                                public void onClick(DialogInterface dialog, int id)
                                                {
                                                    dialog.cancel();
                                                    Intent sipDroidIntent = new Intent().setClass(LoginConsoleVoIPMedia.this, Sipdroid.class);
                                                    startActivity(sipDroidIntent);
                                                }
                                            }
                                    );

                                    AlertDialog successDialog = builderSuccessDialog.create();
                                    successDialog.show();
                                }
                                catch (Exception e)
                                {
                                    stuffErrorDialog();
                                }
                            }
                            else
                            {
                                stuffErrorDialog();
                            }
                        }
                        catch (Exception e)
                        {
                            stuffErrorDialog();
                        }
                    }
                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable)
                    {
                        // Hide the Progress Dialog
                        progressDialog.hide();
                    }
                });
            }
        });
    }

    private void stuffErrorDialog()
    {
        AlertDialog.Builder builderErrorDialog = new AlertDialog.Builder(LoginConsoleVoIPMedia.this);
        builderErrorDialog.setMessage(R.string.error_on_login_console);
        builderErrorDialog.setCancelable(true);

        builderErrorDialog.setPositiveButton(
            R.string.accept,
            new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.cancel();
                }
            }
        );

        /* TODO: ejemplo para agregar boton de cancelar
        builder1.setNegativeButton(
                "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        */
        AlertDialog errorDialog = builderErrorDialog.create();
        errorDialog.show();
    }
}
