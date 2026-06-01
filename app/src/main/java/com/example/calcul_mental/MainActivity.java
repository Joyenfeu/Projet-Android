package com.example.calcul_mental;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
    public static final String MODE_CHRONO_FREDBEAR = "chrono_fredbear";
    public static final String MODE_CHRONO_NIGHTMARE = "chrono_nightmare";
    public static final String MODE_CHRONO_ENDLESS = "chrono_endless";

    public static final String PREFS_PROFIL = "profil_joueur";
    public static final String PREF_CLASSIQUE_TERMINE = "classique_termine";
    public static final String PREF_DIFFICILE_TERMINE = "difficile_termine";
    public static final String PREF_ENDLESS_48 = "endless_48";
    public static final String PREF_CHRONO_FREDBEAR_TERMINE = "chrono_fredbear_termine";
    public static final String PREF_CHRONO_NIGHTMARE_TERMINE = "chrono_nightmare_termine";
    public static final String PREF_CHRONO_ENDLESS_80 = "chrono_endless_80";

    private TextView textViewEtoiles;
    private TextView textViewEtoilesChrono;
    private LinearLayout panelModesVies;
    private LinearLayout panelModesChrono;
    private Button boutonEndless;
    private Button boutonClassique;
    private Button boutonDifficile;
    private Button boutonHighscore;
    private Button boutonChronoFredbear;
    private Button boutonChronoNightmare;
    private Button boutonChronoEndless;
    private Button boutonHighscoreChrono;
    private Button boutonPageDroite;
    private Button boutonPageGauche;

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
        textViewEtoilesChrono = findViewById(R.id.textViewEtoilesChrono);
        panelModesVies = findViewById(R.id.panelModesVies);
        panelModesChrono = findViewById(R.id.panelModesChrono);
        boutonEndless = findViewById(R.id.bouton_endless);
        boutonClassique = findViewById(R.id.bouton_classique);
        boutonDifficile = findViewById(R.id.bouton_difficile);
        boutonHighscore = findViewById(R.id.bouton_historique);
        boutonChronoFredbear = findViewById(R.id.bouton_chrono_fredbear);
        boutonChronoNightmare = findViewById(R.id.bouton_chrono_nightmare);
        boutonChronoEndless = findViewById(R.id.bouton_chrono_endless);
        boutonHighscoreChrono = findViewById(R.id.bouton_historique_chrono);
        boutonPageDroite = findViewById(R.id.bouton_page_droite);
        boutonPageGauche = findViewById(R.id.bouton_page_gauche);

        boutonEndless.setOnClickListener(v -> lancerJeu(MODE_ENDLESS));
        boutonClassique.setOnClickListener(v -> lancerJeu(MODE_CLASSIQUE));
        boutonDifficile.setOnClickListener(v -> lancerJeu(MODE_DIFFICILE));
        boutonHighscore.setOnClickListener(v -> ouvrirHighscores(ScoreDatabaseHelper.CATEGORIE_STANDARD));

        boutonChronoFredbear.setOnClickListener(v -> lancerJeu(MODE_CHRONO_FREDBEAR));
        boutonChronoNightmare.setOnClickListener(v -> lancerJeu(MODE_CHRONO_NIGHTMARE));
        boutonChronoEndless.setOnClickListener(v -> lancerJeu(MODE_CHRONO_ENDLESS));
        boutonHighscoreChrono.setOnClickListener(v -> ouvrirHighscores(ScoreDatabaseHelper.CATEGORIE_CHRONO));

        boutonPageDroite.setOnClickListener(v -> afficherPageChrono(true));
        boutonPageGauche.setOnClickListener(v -> afficherPageChrono(false));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mettreAJourProfil();
    }

    private void afficherPageChrono(boolean chrono) {
        panelModesVies.setVisibility(chrono ? View.GONE : View.VISIBLE);
        panelModesChrono.setVisibility(chrono ? View.VISIBLE : View.GONE);
        textViewEtoiles.setVisibility(chrono ? View.GONE : View.VISIBLE);
        textViewEtoilesChrono.setVisibility(chrono ? View.VISIBLE : View.GONE);
        boutonPageDroite.setVisibility(chrono ? View.GONE : View.VISIBLE);
        boutonPageGauche.setVisibility(chrono ? View.VISIBLE : View.GONE);
    }

    private void mettreAJourProfil() {
        SharedPreferences prefs = getSharedPreferences(PREFS_PROFIL, MODE_PRIVATE);
        boolean classiqueTermine = prefs.getBoolean(PREF_CLASSIQUE_TERMINE, false);
        boolean difficileTermine = prefs.getBoolean(PREF_DIFFICILE_TERMINE, false);
        boolean endless48 = prefs.getBoolean(PREF_ENDLESS_48, false);
        boolean fredbearTermine = prefs.getBoolean(PREF_CHRONO_FREDBEAR_TERMINE, false);
        boolean nightmareTermine = prefs.getBoolean(PREF_CHRONO_NIGHTMARE_TERMINE, false);
        boolean chronoEndless80 = prefs.getBoolean(PREF_CHRONO_ENDLESS_80, false);

        textViewEtoiles.setText(creerEtoilesProfil(
                R.string.format_etoiles,
                new boolean[]{classiqueTermine, difficileTermine, endless48},
                new int[]{Color.rgb(255, 193, 7), Color.rgb(220, 40, 40), Color.rgb(30, 120, 255)}));

        textViewEtoilesChrono.setText(creerEtoilesProfil(
                R.string.format_etoiles_chrono,
                new boolean[]{fredbearTermine, nightmareTermine, chronoEndless80},
                new int[]{Color.rgb(255, 193, 7), Color.BLACK, Color.rgb(30, 120, 255)}));

        boutonDifficile.setEnabled(classiqueTermine);
        boutonDifficile.setText(classiqueTermine
                ? getString(R.string.text_bouton_difficile)
                : getString(R.string.text_bouton_difficile_bloque));

        boutonChronoNightmare.setEnabled(fredbearTermine);
        boutonChronoNightmare.setText(fredbearTermine
                ? getString(R.string.text_bouton_chrono_nightmare)
                : getString(R.string.text_bouton_chrono_nightmare_bloque));
    }

    private SpannableString creerEtoilesProfil(int formatResId, boolean[] debloquees, int[] couleurs) {
        StringBuilder etoiles = new StringBuilder();
        for (boolean debloquee : debloquees) {
            etoiles.append(debloquee ? "★" : "☆");
        }
        String base = getString(formatResId, etoiles.toString());
        SpannableString spannable = new SpannableString(base);
        int debutEtoiles = Math.max(0, base.length() - 3);
        for (int i = 0; i < 3; i++) {
            int position = debutEtoiles + i;
            if (position >= base.length()) continue;
            spannable.setSpan(new ForegroundColorSpan(debloquees[i] ? couleurs[i] : Color.GRAY), position, position + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    private void lancerJeu(String mode) {
        Intent intent = new Intent(this, CalculatriceActivity.class);
        intent.putExtra(EXTRA_MODE_JEU, mode);
        startActivity(intent);
    }

    private void ouvrirHighscores(String categorie) {
        Intent intent = new Intent(this, HistoriqueActivity.class);
        intent.putExtra(HistoriqueActivity.EXTRA_CATEGORIE_SCORE, categorie);
        startActivity(intent);
    }
}
