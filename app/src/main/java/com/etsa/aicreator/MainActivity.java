package com.etsa.aicreator;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
 @Override public void onCreate(Bundle b) {
  super.onCreate(b);
  setContentView(R.layout.activity_main);
  EditText input=findViewById(R.id.promptInput);
  Button button=findViewById(R.id.createButton);
  TextView output=findViewById(R.id.outputText);
  button.setOnClickListener(v -> {
   String prompt=input.getText().toString().trim();
   if(prompt.isEmpty()){output.setText("Describe a game first.");return;}
   output.setText("GAME CREATION REQUEST\n\n"+prompt+"\n\nAndroid creator foundation is working. AI coding, internet research, image references and project generation will be connected next.");
  });
 }
}
