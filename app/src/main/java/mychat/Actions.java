package mychat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;


public class Actions extends Activity {
    private ButtonClickListener buttonClickListener = new ButtonClickListener();
    private String username;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actions);

        findViewById(R.id.configureChannel).setOnClickListener(buttonClickListener);
        findViewById(R.id.joinChannel).setOnClickListener(buttonClickListener);

        Bundle extras = getIntent().getExtras();
        username = extras.getString("username");
    }

    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), Welcome.class);
        startActivityForResult(intent, RESULT_FIRST_USER);
    }

    private class ButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Intent intent;
            switch (view.getId()) {
                case R.id.configureChannel:
                    intent = new Intent(getApplicationContext(), SetChannel.class);
                    intent.putExtra("username", username);
                    startActivityForResult(intent, RESULT_FIRST_USER);
                    break;
                case R.id.joinChannel:
                    intent = new Intent(getApplicationContext(), JoinChannel.class);
                    intent.putExtra("username", "OOOOOOOOO");
                    startActivityForResult(intent, RESULT_FIRST_USER);
                    break;
            }
        }
    }
}
