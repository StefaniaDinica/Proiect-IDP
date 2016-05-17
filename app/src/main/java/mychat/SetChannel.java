package mychat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import android.util.Log;

public class SetChannel extends Activity implements Observer {
    private static final String TAG = "chat.HostActivity";
    private ButtonClickListener buttonClickListener = new ButtonClickListener();
    private String username;

    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_chan);

        Bundle extras = getIntent().getExtras();
        username = extras.getString("username");

        mChannelName = (TextView)findViewById(R.id.hostChannelName);
        mChannelName.setText("");

        mChannelStatus = (TextView)findViewById(R.id.hostChannelStatus);
        mChannelStatus.setText("Idle");

        mChatApplication = (Main)getApplication();
        mChatApplication.checkin();

        startChannel = (Button) findViewById(R.id.startChannel);
        stopChannel = (Button) findViewById(R.id.stopChannel);
        okButton = (Button) findViewById(R.id.okButton);
        startChannel.setOnClickListener(buttonClickListener);
        stopChannel.setOnClickListener(buttonClickListener);
        okButton.setOnClickListener(buttonClickListener);
        channelName = (EditText) findViewById(R.id.username);

        updateChannelState();
        mChatApplication.addObserver(this);
    }

    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), Actions.class);
        intent.putExtra("username", username);
        startActivityForResult(intent, RESULT_FIRST_USER);
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        mChatApplication = (Main)getApplication();
        mChatApplication.deleteObserver(this);
        super.onDestroy();
    }

    private Main mChatApplication = null;

    public synchronized void update(Observable o, Object arg) {
        Log.i(TAG, "update(" + arg + ")");
        String qualifier = (String)arg;

        if (qualifier.equals(Main.APPLICATION_QUIT_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_APPLICATION_QUIT_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(Main.HOST_CHANNEL_STATE_CHANGED_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_CHANNEL_STATE_CHANGED_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(Main.ALLJOYN_ERROR_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_ALLJOYN_ERROR_EVENT);
            mHandler.sendMessage(message);
        }
    }

    private void updateChannelState() {
        Communication.HostChannelState channelState = mChatApplication.hostGetChannelState();
        String name = mChatApplication.hostGetChannelName();
        boolean haveName = true;
        if (name == null) {
            haveName = false;
            name = "Not set";
        }
        mChannelName.setText(name);

        switch (channelState) {
        case IDLE:
            mChannelStatus.setText("Idle");
            break;
        case NAMED:
            mChannelStatus.setText("Named");
            break;
        case BOUND:
            mChannelStatus.setText("Bound");
            break;
        case ADVERTISED:
            mChannelStatus.setText("Advertised");
            break;
        case CONNECTED:
            mChannelStatus.setText("Connected");
            break;
        default:
            mChannelStatus.setText("Unknown");
            break;
        }

        if (channelState == Communication.HostChannelState.IDLE) {
            if (haveName) {
                startChannel.setEnabled(true);
            } else {
                startChannel.setEnabled(false);
            }
            stopChannel.setEnabled(false);
        } else {
            startChannel.setEnabled(false);
            stopChannel.setEnabled(true);
        }
    }

    private TextView mChannelName;
    private TextView mChannelStatus;
    private Button startChannel;
    private Button stopChannel;
    private Button okButton;
    private EditText channelName;

    private static final int HANDLE_APPLICATION_QUIT_EVENT = 0;
    private static final int HANDLE_CHANNEL_STATE_CHANGED_EVENT = 1;
    private static final int HANDLE_ALLJOYN_ERROR_EVENT = 2;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case HANDLE_APPLICATION_QUIT_EVENT:
                {
                    Log.i(TAG, "mHandler.handleMessage(): HANDLE_APPLICATION_QUIT_EVENT");
                    finish();
                }
                break;
            case HANDLE_CHANNEL_STATE_CHANGED_EVENT:
                {
                    Log.i(TAG, "mHandler.handleMessage(): HANDLE_CHANNEL_STATE_CHANGED_EVENT");
                    updateChannelState();
                }
                break;
            default:
                break;
            }
        }
    };


    private class ButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.startChannel:
                    mChatApplication.hostStartChannel();
                    break;
                case R.id.stopChannel:
                    mChatApplication.hostStopChannel();
                    break;
                case R.id.okButton:
                    String name = channelName.getText().toString();
                    mChatApplication.hostSetChannelName(name);
                    mChatApplication.hostInitChannel();
            }
        }
    }
}
