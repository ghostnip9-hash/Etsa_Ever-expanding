package com.etsa.aicreator;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class MainActivity extends AndroidApplication {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        configuration.useAccelerometer = false;
        configuration.useCompass = false;
        configuration.useGyroscope = false;
        configuration.useImmersiveMode = true;

        initialize(new EtsaWorld(), configuration);
    }
}
