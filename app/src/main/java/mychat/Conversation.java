package mychat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.util.Log;
import java.util.List;

public class Conversation extends Activity implements Observer {
    private static final String TAG = "chat.UseActivity";
    private Button leaveChan;
    private String username;
    Communication.UseChannelState status;
    String name;


    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.use);

        Bundle extras = getIntent().getExtras();
        username = extras.getString("username");
        Log.i(TAG, "()()()()()()()........." + username);


        status = (Communication.UseChannelState)extras.get("status");
        name = (String) extras.get("name");

        mHistoryList = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view.findViewById(android.R.id.text1);
                tv.setTextColor(Color.DKGRAY);
                return view;
            }
        };
        ListView hlv = (ListView) findViewById(R.id.useHistoryList);
        hlv.setAdapter(mHistoryList);

        EditText messageBox = (EditText)findViewById(R.id.useMessage);
        messageBox.setSingleLine();
        messageBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String message = view.getText().toString();
                    Log.i(TAG, "useMessage.onEditorAction(): got message " + message + ")");

                    mChatApplication.newLocalUserMessage(message);
                    view.setText("");
                }
                return true;
            }
        });

        leaveChan = (Button)findViewById(R.id.useLeave);
        leaveChan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switch (view.getId()) {
                        case R.id.useLeave:
                            mChatApplication.useLeaveChannel();
                            mChatApplication.useSetChannelName("Not set");
                            Log.i(TAG, "()()()()()() " + username);
                            Intent intent = new Intent(getApplicationContext(), JoinChannel.class);
                            intent.putExtra("username", username);
                            startActivityForResult(intent, RESULT_FIRST_USER);
                    }
                }
        });

        mChannelName = (TextView)findViewById(R.id.useChannelName);
        mChannelName.setText(name);

        mChatApplication = (Main)getApplication();
        mChatApplication.checkin();
        mChatApplication.setNickname(username);

        updateChannelState();
        updateHistory();

        mChatApplication.addObserver(this);

    }

    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), JoinChannel.class);
        intent.putExtra("username", username);
        startActivityForResult(intent, RESULT_FIRST_USER);
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        mChatApplication = (Main)getApplication();
        mChatApplication.deleteObserver(this);
        super.onDestroy();
    }

    public synchronized void update(Observable o, Object arg) {
        Log.i(TAG, "update(" + arg + ")");
        String qualifier = (String)arg;

        if (qualifier.equals(Main.APPLICATION_QUIT_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_APPLICATION_QUIT_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(Main.HISTORY_CHANGED_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_HISTORY_CHANGED_EVENT);
            mHandler.sendMessage(message);
        }

        if (qualifier.equals(Main.USE_CHANNEL_STATE_CHANGED_EVENT)) {
			updateChannelState();
            Intent intent = new Intent(getApplicationContext(), JoinChannel.class);
            startActivityForResult(intent, RESULT_FIRST_USER);
        }

        if (qualifier.equals(Main.ALLJOYN_ERROR_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_ALLJOYN_ERROR_EVENT);
            mHandler.sendMessage(message);
        }
    }

    private void updateHistory() {
        Log.i(TAG, "updateHistory()");
        mHistoryList.clear();
        List<String> messages = mChatApplication.getHistory();
        for (String message : messages) {
            mHistoryList.add(message);
        }
        mHistoryList.notifyDataSetChanged();
    }

    private void updateChannelState() {
        Log.i(TAG, "updateHistory()");
        Communication.UseChannelState channelState = mChatApplication.useGetChannelState();
        String name = mChatApplication.useGetChannelName();
        
        if (name == null) {
            name = "Not set";
        }
    }

    private void alljoynError() {
        if (mChatApplication.getErrorModule() == Main.Module.GENERAL ||
            mChatApplication.getErrorModule() == Main.Module.USE) {
        }
    }

    private static final int HANDLE_APPLICATION_QUIT_EVENT = 0;
    private static final int HANDLE_HISTORY_CHANGED_EVENT = 1;
    private static final int HANDLE_CHANNEL_STATE_CHANGED_EVENT = 2;
    private static final int HANDLE_ALLJOYN_ERROR_EVENT = 3;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case HANDLE_APPLICATION_QUIT_EVENT:
                {
                    Log.i(TAG, "mHandler.handleMessage(): HANDLE_APPLICATION_QUIT_EVENT");
                    finish();
                }
                break;
            case HANDLE_HISTORY_CHANGED_EVENT:
                {
                    Log.i(TAG, "mHandler.handleMessage(): HANDLE_HISTORY_CHANGED_EVENT");
                    updateHistory();
                    break;
                }

            case HANDLE_CHANNEL_STATE_CHANGED_EVENT:
                {
                    Log.i(TAG, "mHandler.handleMessage(): HANDLE_CHANNEL_STATE_CHANGED_EVENT");
                    updateChannelState();
                    break;
                }
				case HANDLE_ALLJOYN_ERROR_EVENT:
                {
                    Log.i(TAG, "mHandler.handleMessage(): HANDLE_ALLJOYN_ERROR_EVENT");
                    alljoynError();
                    break;
                }
            default:
                break;
            }
        }
    };

    private Main mChatApplication = null;
    private ArrayAdapter<String> mHistoryList;
    private TextView mChannelName;
}
