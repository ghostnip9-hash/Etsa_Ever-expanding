package com.etsa.aicreator;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
 private TextView status;
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  setContentView(R.layout.activity_main);
  status=findViewById(R.id.statusText);
  findViewById(R.id.createGameButton).setOnClickListener(v->showCreateGame());
  findViewById(R.id.projectsButton).setOnClickListener(v->status.setText("Projects: project manager will be added next."));
  findViewById(R.id.aiCoderButton).setOnClickListener(v->status.setText("AI Coder: online coding model connection comes next."));
  findViewById(R.id.webResearchButton).setOnClickListener(v->status.setText("Web Research: internet search integration comes next."));
  findViewById(R.id.imageRefsButton).setOnClickListener(v->status.setText("Image References: picture search and reference board comes next."));
  findViewById(R.id.techniquesButton).setOnClickListener(v->status.setText("Techniques Library: saved coding methods will appear here."));
  findViewById(R.id.settingsButton).setOnClickListener(v->status.setText("Settings: AI connection, storage and build options."));
 }
 private void showCreateGame(){
  EditText input=new EditText(this);
  input.setHint("Describe the game you want to create...");
  input.setMinLines(5);
  new AlertDialog.Builder(this)
   .setTitle("Create Game")
   .setView(input)
   .setPositiveButton("Create",(d,w)->{
    String p=input.getText().toString().trim();
    if(p.isEmpty()) status.setText("Describe a game first.");
    else status.setText("GAME CREATION REQUEST\n\n"+p+"\n\nPrompt captured. Real AI generation will be connected next.");
   })
   .setNegativeButton("Cancel",null)
   .show();
 }
}
