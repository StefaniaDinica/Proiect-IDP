package mychat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.List;

public class JoinChannel extends Activity implements Observer {
    ListView listView ;
    private Main mChatApplication = null;
    private static final String TAG = "chat.JoinChannel";
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.join_chan);

        Bundle extras = getIntent().getExtras();
        username = "Me"; //extras.getString("username");

        mChatApplication = (Main)getApplication();
        mChatApplication.checkin();

        listView = (ListView) findViewById(R.id.list);


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view.findViewById(android.R.id.text1);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
                tv.setTextColor(Color.DKGRAY);
                return view;
            }
        };

        List<String> channels = mChatApplication.getFoundChannels();
        for (String channel : channels) {
            int lastDot = channel.lastIndexOf('.');
            if (lastDot < 0) {
                continue;
            }
            adapter.add(channel.substring(lastDot + 1));
        }
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                String chanName = listView.getItemAtPosition(position).toString();
                Log.d(TAG, "------------------------------------------" + chanName);
                mChatApplication.useSetChannelName(chanName);
                mChatApplication.useJoinChannel();
            }
        });

        mChatApplication.addObserver(this);
    }

    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), Actions.class);
        intent.putExtra("username", username);
        startActivityForResult(intent, RESULT_FIRST_USER);
    }

    public synchronized void update(Observable o, Object arg) {
        String qualifier = (String)arg;
        if (qualifier.equals(Main.USE_CHANNEL_STATE_CHANGED_EVENT)) {
            Intent intent = new Intent(getApplicationContext(), Conversation.class);
            intent.putExtra("status", mChatApplication.useGetChannelState());
            intent.putExtra("name", mChatApplication.useGetChannelName());
            intent.putExtra("username", username);
            startActivityForResult(intent, RESULT_FIRST_USER);
        }
    }

}

