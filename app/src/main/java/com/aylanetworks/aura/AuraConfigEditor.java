package com.aylanetworks.aura;

/*
 * Aura_Android
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.aylanetworks.aura.util.AuraConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class AuraConfigEditor extends AppCompatActivity {
    private EditText _editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_config);

        _editText = (EditText)findViewById(R.id.edittext);

        AuraConfig config = AuraConfig.getSelectedConfiguration();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(config);

        _editText.setText(json);

        Button b = (Button)findViewById(R.id.save);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveClicked();
            }
        });

        b = (Button)findViewById(R.id.cancel);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

    }

    private void saveClicked() {
        String json = _editText.getText().toString();

        AuraConfig editedConfig = null;
        Gson gson = new GsonBuilder().create();
        try {
            editedConfig = gson.fromJson(json, AuraConfig.class);
        } catch (JsonSyntaxException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        if (editedConfig != null) {
            AuraConfig.addConfig(editedConfig);
            AuraConfig.setSelectedConfiguration(editedConfig);
            setResult(RESULT_OK);
            finish();
        }
    }
}
