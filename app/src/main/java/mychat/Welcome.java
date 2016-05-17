package mychat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class Welcome extends Activity {
    private ButtonClickListener buttonClickListener = new ButtonClickListener();
    EditText username = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);

        findViewById(R.id.submit_button).setOnClickListener(buttonClickListener);
        username = (EditText) findViewById(R.id.username);

    }


    private class ButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.submit_button:
                    Intent intent = new Intent(getApplicationContext(), Actions.class);
                    intent.putExtra("username",username.getText().toString());
                    startActivityForResult(intent, RESULT_FIRST_USER);
                    break;
            }
        }
    }
}
