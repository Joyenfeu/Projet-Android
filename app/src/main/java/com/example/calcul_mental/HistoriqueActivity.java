package com.example.calcul_mental;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

public class HistoriqueActivity extends AppCompatActivity {
    private LinearLayout conteneurScores;
    private TextView textViewAucunScore;
    private Button boutonRetourMenu;
    private ScoreDatabaseHelper scoreDatabaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_historique);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        conteneurScores = findViewById(R.id.conteneurScores);
        textViewAucunScore = findViewById(R.id.textViewAucunScore);
        boutonRetourMenu = findViewById(R.id.boutonRetourMenu);
        scoreDatabaseHelper = new ScoreDatabaseHelper(this);

        boutonRetourMenu.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        afficherScores();
    }

    private void afficherScores() {
        List<ScoreDatabaseHelper.Score> scores = scoreDatabaseHelper.recupererTopScores(10);
        conteneurScores.removeAllViews();

        if (scores.isEmpty()) {
            textViewAucunScore.setVisibility(View.VISIBLE);
            return;
        }

        textViewAucunScore.setVisibility(View.GONE);
        for (int i = 0; i < scores.size(); i++) {
            ScoreDatabaseHelper.Score score = scores.get(i);
            TextView ligneScore = new TextView(this);
            ligneScore.setText(getString(R.string.format_ligne_score, i + 1, score.getNom(), score.getScore()));
            ligneScore.setTextSize(20);
            ligneScore.setPadding(0, 16, 0, 16);
            conteneurScores.addView(ligneScore);
        }
    }
}
