package com.example.calcul_mental;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MODE_JEU = "mode_jeu";
    public static final String MODE_ENDLESS = "endless";
    public static final String MODE_CLASSIQUE = "classique";
    public static final String MODE_DIFFICILE = "difficile";

    public static final String PREFS_PROFIL = "profil_joueur";
    public static final String PREF_CLASSIQUE_TERMINE = "classique_termine";
    public static final String PREF_DIFFICILE_TERMINE = "difficile_termine";
    public static final String PREF_ENDLESS_48 = "endless_48";

    private TextView textViewEtoiles;
    private Button boutonEndless;
    private Button boutonClassique;
    private Button boutonDifficile;
    private Button boutonHighscore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textViewEtoiles = findViewById(R.id.textViewEtoiles);
        boutonEndless = findViewById(R.id.bouton_endless);
        boutonClassique = findViewById(R.id.bouton_classique);
        boutonDifficile = findViewById(R.id.bouton_difficile);
        boutonHighscore = findViewById(R.id.bouton_historique);

        boutonEndless.setOnClickListener(v -> lancerJeu(MODE_ENDLESS));
        boutonClassique.setOnClickListener(v -> lancerJeu(MODE_CLASSIQUE));
        boutonDifficile.setOnClickListener(v -> lancerJeu(MODE_DIFFICILE));
        boutonHighscore.setOnClickListener(v -> startActivity(new Intent(this, HistoriqueActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mettreAJourProfil();
    }

    private void mettreAJourProfil() {
        SharedPreferences prefs = getSharedPreferences(PREFS_PROFIL, MODE_PRIVATE);
        boolean classiqueTermine = prefs.getBoolean(PREF_CLASSIQUE_TERMINE, false);
        boolean difficileTermine = prefs.getBoolean(PREF_DIFFICILE_TERMINE, false);
        boolean endless48 = prefs.getBoolean(PREF_ENDLESS_48, false);

        int nbEtoiles = 0;
        if (classiqueTermine) nbEtoiles++;
        if (difficileTermine) nbEtoiles++;
        if (endless48) nbEtoiles++;

        StringBuilder etoiles = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            etoiles.append(i < nbEtoiles ? "⭐" : "☆");
        }
        textViewEtoiles.setText(getString(R.string.format_etoiles, etoiles.toString()));

        boutonDifficile.setEnabled(classiqueTermine);
        boutonDifficile.setText(classiqueTermine
                ? getString(R.string.text_bouton_difficile)
                : getString(R.string.text_bouton_difficile_bloque));
    }

    private void lancerJeu(String mode) {
        Intent intent = new Intent(this, CalculatriceActivity.class);
        intent.putExtra(EXTRA_MODE_JEU, mode);
        startActivity(intent);
    }
}
