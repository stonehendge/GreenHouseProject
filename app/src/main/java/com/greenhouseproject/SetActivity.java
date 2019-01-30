package com.greenhouseproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SetActivity extends AppCompatActivity {

    SharedPreferences sPref;
    EditText eTextTel;
    final String SAVED_TEL = "TNumber";
    private Context context;

    public Context getContext() { return context; }
    public void setContext(Context context_) { context = context_; }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set);

        this.context = getApplicationContext();

        eTextTel = (EditText) findViewById(R.id.editTextTel);
        Button buttonSettings = (Button) findViewById(R.id.buttonExit);


        sPref = getSharedPreferences(SAVED_TEL,MODE_PRIVATE);
        String TN = sPref.getString(SAVED_TEL, "");
        if (TN.equals("")==false)
        {
            eTextTel.setText(TN);
        }

        buttonSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                //matches 10-digit numbers only
                String regexStr = "^[0-9]{10}$";
                String str = eTextTel.getText().toString();
                if (str.matches(regexStr))
                {
                    sPref = getSharedPreferences(SAVED_TEL,MODE_PRIVATE);
                    SharedPreferences.Editor ed = sPref.edit();
                    ed.putString(SAVED_TEL, eTextTel.getText().toString());
                    ed.commit();

                    //setContext(context);

                    Intent intent = new Intent();
                    intent.setAction(MainActivity.ACTION_UPD);
                    //intent.putExtra("dataToPass", test);
                    context.getApplicationContext().sendBroadcast(intent);
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Неверный формат номера - 10 цифр", Toast.LENGTH_SHORT).show();
                    return;
                }

                finish();

            }
        });

    }
}
