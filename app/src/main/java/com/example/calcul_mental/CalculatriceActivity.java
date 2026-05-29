package com.example.calcul_mental;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CalculatriceActivity extends AppCompatActivity {
    private static final int VIES_DEPART = 3;
    private static final int VIES_DEPART_DIFFICILE = 1;
    private static final int VIES_MAX = 4;
    private static final int TEMPS_DEPART_SECONDES = 20;
    private static final int TEMPS_MINIMUM_SECONDES = 10;
    private static final int TEMPS_NORMAL_DIFFICILE_SECONDES = 12;
    private static final int TEMPS_BOSS_SECONDES = 25;
    private static final int DELAI_PAUSE_ROUND_MS = 4000;
    private static final int DELAI_PAUSE_DEMARRAGE_MS = 2500;
    private static final int DELAI_ANIMATION_COEUR_MS = 1000;
    private static final int SCORE_ENDLESS_ETOILE = 48;

    private TextView textViewOperation;
    private TextView textViewScore;
    private TextView textViewVies;
    private TextView textViewTimer;
    private TextView textViewNiveau;
    private TextView textViewPauseRound;
    private EditText editTextReponse;
    private Button boutonValider;
    private Button boutonPasser;

    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private ScoreDatabaseHelper scoreDatabaseHelper;
    private MediaPlayer mediaPlayer;

    private int bonneReponse;
    private int score = 0;
    private int vies = VIES_DEPART;
    private int niveau = 1;
    private boolean partieTerminee = false;
    private boolean roundEnPause = false;
    private boolean questionBoss = false;
    private CountDownTimer countDownTimer;
    private int tempsQuestionSecondes = TEMPS_DEPART_SECONDES;
    private int tempsRestantSecondes = TEMPS_DEPART_SECONDES;
    private TypeOperationQuestion typeOperationCourante = TypeOperationQuestion.ADDITION;
    private TypeOperationQuestion typeOperationAnnoncee = null;
    private boolean musiqueJeuActive = false;
    private ModeJeu modeJeu = ModeJeu.ENDLESS;

    private enum ModeJeu {
        ENDLESS, CLASSIQUE, DIFFICILE
    }

    private enum TypeOperationQuestion {
        ADDITION, SOUSTRACTION, MULTIPLICATION, DIVISION, BOSS_GRANDE_OPERATION, BOSS_INCONNU, BOSS_OPERATION_DANS_OPERATION
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calculatrice);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        scoreDatabaseHelper = new ScoreDatabaseHelper(this);
        lireModeJeu();

        textViewOperation = findViewById(R.id.textViewCalcul);
        textViewScore = findViewById(R.id.textViewScore);
        textViewVies = findViewById(R.id.textViewVies);
        textViewTimer = findViewById(R.id.textViewTimer);
        textViewNiveau = findViewById(R.id.textViewNiveau);
        textViewPauseRound = findViewById(R.id.textViewPauseRound);
        editTextReponse = findViewById(R.id.editTextReponse);
        boutonValider = findViewById(R.id.boutonValider);
        boutonPasser = findViewById(R.id.boutonPasser);

        boutonValider.setOnClickListener(v -> verifierReponse());
        boutonPasser.setOnClickListener(v -> perdreUneVieEtContinuer());

        nouvellePartie();
    }

    private void lireModeJeu() {
        String mode = getIntent().getStringExtra(MainActivity.EXTRA_MODE_JEU);
        if (MainActivity.MODE_CLASSIQUE.equals(mode)) {
            modeJeu = ModeJeu.CLASSIQUE;
        } else if (MainActivity.MODE_DIFFICILE.equals(mode)) {
            modeJeu = ModeJeu.DIFFICILE;
        } else {
            modeJeu = ModeJeu.ENDLESS;
        }
    }

    private void demarrerMusiqueJeu() {
        musiqueJeuActive = true;
        lancerMusique(R.raw.musique_intro, false, () -> {
            if (!partieTerminee && musiqueJeuActive) {
                demarrerMusiqueBoucle();
            }
        });
    }

    private void demarrerMusiqueBoucle() {
        if (!musiqueJeuActive || partieTerminee) {
            return;
        }
        lancerMusique(R.raw.musique_loop, true, null);
    }

    private void demarrerMusiqueFinPartie() {
        musiqueJeuActive = false;
        lancerMusique(R.raw.musique_game_over, false, null);
    }

    private void lancerMusique(int resId, boolean enBoucle, Runnable actionFin) {
        libererLecteurActuel();
        mediaPlayer = MediaPlayer.create(this, resId);
        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.setLooping(enBoucle);
        mediaPlayer.setOnCompletionListener(mp -> {
            if (actionFin != null) {
                handler.post(actionFin);
            }
        });
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            libererLecteurActuel();
            if (musiqueJeuActive && !partieTerminee) {
                demarrerMusiqueBoucle();
            }
            return true;
        });
        try {
            mediaPlayer.start();
        } catch (IllegalStateException ignored) {
            libererLecteurActuel();
        }
    }

    private void arreterMusique() {
        musiqueJeuActive = false;
        libererLecteurActuel();
    }

    private void libererLecteurActuel() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.setOnCompletionListener(null);
                mediaPlayer.setOnErrorListener(null);
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException ignored) {
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void libererMusique() {
        musiqueJeuActive = false;
        libererLecteurActuel();
    }

    private void nouvellePartie() {
        arreterTimer();
        handler.removeCallbacksAndMessages(null);
        score = 0;
        niveau = 1;
        vies = modeJeu == ModeJeu.DIFFICILE ? VIES_DEPART_DIFFICILE : VIES_DEPART;
        tempsRestantSecondes = getTempsNormalQuestion();
        partieTerminee = false;
        roundEnPause = false;
        questionBoss = false;
        textViewPauseRound.setVisibility(android.view.View.GONE);
        activerSaisie(true);
        demarrerMusiqueJeu();
        afficherPauseAvantQuestion(getString(R.string.message_debut_partie_mode, getNomModeJeu()), true);
        majAffichageScoreViesTimer();
    }

    private String getNomModeJeu() {
        switch (modeJeu) {
            case CLASSIQUE:
                return getString(R.string.nom_mode_classique);
            case DIFFICILE:
                return getString(R.string.nom_mode_difficile);
            case ENDLESS:
            default:
                return getString(R.string.nom_mode_endless);
        }
    }

    private void activerSaisie(boolean actif) {
        editTextReponse.setEnabled(actif);
        boutonValider.setEnabled(actif);
        boutonPasser.setEnabled(actif);
    }

    private int getNiveauDifficulte() {
        if (modeJeu == ModeJeu.DIFFICILE) {
            return 5;
        }
        return Math.min(5, 1 + ((niveau - 1) / 5));
    }

    private boolean estMode30Rounds() {
        return modeJeu == ModeJeu.CLASSIQUE || modeJeu == ModeJeu.DIFFICILE;
    }

    private boolean estNiveauBoss() {
        if (estMode30Rounds()) {
            return niveau == 10 || niveau == 20 || niveau == 30;
        }
        return niveau % 16 == 0;
    }

    private boolean estNiveauMiniBoss() {
        return modeJeu == ModeJeu.CLASSIQUE && (niveau == 5 || niveau == 15 || niveau == 25);
    }

    private void genererCalcul() {
        roundEnPause = false;
        activerSaisie(true);
        textViewPauseRound.setVisibility(android.view.View.GONE);

        questionBoss = estNiveauBoss();
        if (questionBoss) {
            genererCalculBoss();
        } else {
            genererCalculNormal();
        }

        editTextReponse.setText("");
        editTextReponse.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(editTextReponse, InputMethodManager.SHOW_IMPLICIT);
        }

        demarrerTimerQuestion();
        majAffichageScoreViesTimer();
    }

    private void genererCalculNormal() {
        List<TypeOperationQuestion> operations = getOperationsDisponibles();

        if (typeOperationAnnoncee != null && operations.contains(typeOperationAnnoncee)) {
            typeOperationCourante = typeOperationAnnoncee;
            typeOperationAnnoncee = null;
        } else {
            typeOperationCourante = operations.get(random.nextInt(operations.size()));
        }

        int difficulte = getNiveauDifficulte();
        int a;
        int b;

        switch (typeOperationCourante) {
            case SOUSTRACTION:
                if (difficulte >= 5) {
                    a = random.nextInt(1001);
                    b = Math.max(0, a - random.nextInt(151));
                } else {
                    a = random.nextInt(101);
                    b = random.nextInt(a + 1);
                }
                bonneReponse = a - b;
                textViewOperation.setText(getString(R.string.format_operation, a, "-", b));
                break;
            case MULTIPLICATION:
                int maxMultiplication = difficulte >= 5 ? 30 : 12;
                a = random.nextInt(maxMultiplication + 1);
                b = random.nextInt(maxMultiplication + 1);
                bonneReponse = a * b;
                textViewOperation.setText(getString(R.string.format_operation, a, "×", b));
                break;
            case DIVISION:
                if (difficulte >= 5) {
                    b = random.nextInt(20) + 1;
                    bonneReponse = random.nextInt((1000 / b) + 1);
                    a = b * bonneReponse;
                } else {
                    b = random.nextInt(12) + 1;
                    bonneReponse = random.nextInt(13);
                    a = b * bonneReponse;
                }
                textViewOperation.setText(getString(R.string.format_operation, a, "÷", b));
                break;
            case ADDITION:
            default:
                int maxAddition = difficulte >= 5 ? 500 : 50;
                a = random.nextInt(maxAddition + 1);
                b = random.nextInt(maxAddition + 1);
                bonneReponse = a + b;
                textViewOperation.setText(getString(R.string.format_operation, a, "+", b));
                break;
        }
    }

    private List<TypeOperationQuestion> getOperationsDisponibles() {
        List<TypeOperationQuestion> operations = new ArrayList<>();
        operations.add(TypeOperationQuestion.ADDITION);

        if (modeJeu == ModeJeu.DIFFICILE) {
            operations.add(TypeOperationQuestion.SOUSTRACTION);
            operations.add(TypeOperationQuestion.MULTIPLICATION);
            operations.add(TypeOperationQuestion.DIVISION);
            return operations;
        }

        if (modeJeu == ModeJeu.CLASSIQUE) {
            if (niveau >= 5) operations.add(TypeOperationQuestion.SOUSTRACTION);
            if (niveau >= 15) operations.add(TypeOperationQuestion.MULTIPLICATION);
            if (niveau >= 25) operations.add(TypeOperationQuestion.DIVISION);
            return operations;
        }

        int difficulte = getNiveauDifficulte();
        if (difficulte >= 2) operations.add(TypeOperationQuestion.SOUSTRACTION);
        if (difficulte >= 3) operations.add(TypeOperationQuestion.MULTIPLICATION);
        if (difficulte >= 4) operations.add(TypeOperationQuestion.DIVISION);
        return operations;
    }

    private void genererCalculBoss() {
        int typeBoss;
        if (modeJeu == ModeJeu.CLASSIQUE) {
            if (niveau == 10) {
                typeBoss = 1;
            } else if (niveau == 20) {
                typeBoss = 2;
            } else {
                typeBoss = 0;
            }
        } else {
            typeBoss = random.nextInt(3);
        }

        switch (typeBoss) {
            case 0:
                typeOperationCourante = TypeOperationQuestion.BOSS_GRANDE_OPERATION;
                int a = random.nextInt(151) + 50;
                int b = random.nextInt(81) + 20;
                int c = random.nextInt(31) + 5;
                int d = random.nextInt(6) + 2;
                bonneReponse = a + b - (c * d);
                textViewOperation.setText(getString(R.string.format_boss_grande_operation, a, b, c, d));
                break;
            case 1:
                typeOperationCourante = TypeOperationQuestion.BOSS_INCONNU;
                int inconnu = random.nextInt(301);
                int ajout = random.nextInt(151) + 25;
                int total = inconnu + ajout;
                bonneReponse = inconnu;
                textViewOperation.setText(getString(R.string.format_boss_inconnu, ajout, total));
                break;
            default:
                typeOperationCourante = TypeOperationQuestion.BOSS_OPERATION_DANS_OPERATION;
                int x1 = random.nextInt(101);
                int x2 = random.nextInt(101);
                int y1 = random.nextInt(21);
                int y2 = random.nextInt(11);
                int x = x1 + x2;
                int y = y1 * y2;
                bonneReponse = x + y;
                textViewOperation.setText(getString(R.string.format_boss_operation_dans_operation, x1, x2, y1, y2));
                break;
        }
    }

    private int getTempsNormalQuestion() {
        if (modeJeu == ModeJeu.DIFFICILE) {
            return TEMPS_NORMAL_DIFFICILE_SECONDES;
        }
        return Math.max(TEMPS_MINIMUM_SECONDES, TEMPS_DEPART_SECONDES - score);
    }

    private void demarrerTimerQuestion() {
        arreterTimer();
        tempsQuestionSecondes = questionBoss ? TEMPS_BOSS_SECONDES : getTempsNormalQuestion();
        tempsRestantSecondes = tempsQuestionSecondes;
        majAffichageScoreViesTimer();

        countDownTimer = new CountDownTimer(tempsQuestionSecondes * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                tempsRestantSecondes = (int) Math.ceil(millisUntilFinished / 1000.0);
                majAffichageScoreViesTimer();
            }

            @Override
            public void onFinish() {
                tempsRestantSecondes = 0;
                majAffichageScoreViesTimer();
                if (!partieTerminee && !roundEnPause) {
                    Toast.makeText(CalculatriceActivity.this, R.string.temps_ecoule, Toast.LENGTH_SHORT).show();
                    perdreUneVieEtContinuer();
                }
            }
        }.start();
    }

    private void arreterTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void verifierReponse() {
        if (partieTerminee || roundEnPause) {
            return;
        }

        String saisie = editTextReponse.getText().toString().trim();
        if (saisie.isEmpty() || saisie.equals("-")) {
            editTextReponse.setError(getString(R.string.erreur_reponse_vide));
            return;
        }

        int reponseUtilisateur;
        try {
            reponseUtilisateur = Integer.parseInt(saisie);
        } catch (NumberFormatException e) {
            editTextReponse.setError(getString(R.string.erreur_reponse_invalide));
            return;
        }

        if (reponseUtilisateur == bonneReponse) {
            Toast.makeText(this, R.string.reponse_correcte, Toast.LENGTH_SHORT).show();
            reponseCorrecte();
        } else {
            Toast.makeText(this, getString(R.string.reponse_fausse, bonneReponse), Toast.LENGTH_SHORT).show();
            perdreUneVieEtContinuer();
        }
    }

    private void reponseCorrecte() {
        arreterTimer();
        int ancienneDifficulte = getNiveauDifficulte();
        boolean bossVaincu = questionBoss;
        score++;

        if (estMode30Rounds() && score >= 30) {
            terminerPartie(true);
            return;
        }

        niveau = score + 1;
        boolean difficulteAugmente = getNiveauDifficulte() > ancienneDifficulte;

        if (bossVaincu && modeJeu != ModeJeu.DIFFICILE && vies < VIES_MAX) {
            vies++;
            Toast.makeText(this, R.string.boss_vaincu_coeur, Toast.LENGTH_SHORT).show();
        }

        majAffichageScoreViesTimer();
        afficherPauseAvantQuestion(creerMessagePause(difficulteAugmente, bossVaincu), false);
    }

    private void perdreUneVieEtContinuer() {
        if (partieTerminee || roundEnPause) {
            return;
        }

        arreterTimer();
        afficherAnimationViePerdue();
    }

    private void afficherAnimationViePerdue() {
        int viesAvantPerte = vies;
        roundEnPause = true;
        activerSaisie(false);
        textViewOperation.setText("");
        editTextReponse.setText("");

        String message = getString(R.string.message_vie_perdue) + "\n\n"
                + getString(R.string.format_pause_round, afficherCoeurs(viesAvantPerte, true), niveau, getNiveauDifficulte());
        textViewPauseRound.setText(message);
        textViewPauseRound.setAlpha(0f);
        textViewPauseRound.setVisibility(android.view.View.VISIBLE);
        textViewPauseRound.animate().alpha(1f).setDuration(250).start();

        handler.postDelayed(() -> {
            if (partieTerminee) {
                return;
            }
            vies = Math.max(0, viesAvantPerte - 1);
            majAffichageScoreViesTimer();

            if (vies <= 0) {
                terminerPartie(false);
            } else {
                afficherPauseAvantQuestion(getString(R.string.message_vie_perdue), false);
            }
        }, DELAI_ANIMATION_COEUR_MS);
    }

    private String creerMessagePause(boolean difficulteAugmente, boolean bossVaincu) {
        StringBuilder message = new StringBuilder();
        if (bossVaincu) {
            message.append(getString(R.string.message_boss_vaincu)).append("\n");
        }
        if (difficulteAugmente) {
            message.append(getString(R.string.message_level_up)).append("\n");
        }
        if (estNiveauMiniBoss()) {
            message.append(getString(R.string.message_miniboss)).append("\n");
        }
        message.append(getString(R.string.message_round_suivant));
        return message.toString();
    }

    private void afficherPauseAvantQuestion(String messageSpecial, boolean demarrage) {
        if (partieTerminee) {
            return;
        }
        arreterTimer();
        roundEnPause = true;
        activerSaisie(false);

        String prochainMessageOperation;
        if (estNiveauBoss()) {
            typeOperationAnnoncee = null;
            prochainMessageOperation = getString(R.string.message_type_boss);
        } else {
            typeOperationAnnoncee = choisirTypeOperationNormale();
            prochainMessageOperation = getString(R.string.message_type_operation, getNomTypeOperation(typeOperationAnnoncee));
        }

        StringBuilder message = new StringBuilder();
        if (messageSpecial != null && !messageSpecial.isEmpty()) {
            message.append(messageSpecial).append("\n\n");
        }
        message.append(getString(R.string.format_pause_round, afficherCoeurs(), niveau, getNiveauDifficulte()));
        if (estNiveauMiniBoss()) {
            message.append("\n").append(getMessageMiniBoss());
        }
        if (estNiveauBoss()) {
            message.append("\n").append(getString(R.string.message_entree_boss));
        }
        message.append("\n\n").append(prochainMessageOperation);

        textViewPauseRound.setText(message.toString());
        textViewPauseRound.setAlpha(0f);
        textViewPauseRound.setVisibility(android.view.View.VISIBLE);
        textViewPauseRound.animate().alpha(1f).setDuration(300).start();
        textViewOperation.setText("");
        editTextReponse.setText("");

        handler.postDelayed(this::genererCalcul, demarrage ? DELAI_PAUSE_DEMARRAGE_MS : DELAI_PAUSE_ROUND_MS);
    }

    private String getMessageMiniBoss() {
        if (niveau == 5) {
            return getString(R.string.message_miniboss_soustraction);
        }
        if (niveau == 15) {
            return getString(R.string.message_miniboss_multiplication);
        }
        if (niveau == 25) {
            return getString(R.string.message_miniboss_division);
        }
        return getString(R.string.message_miniboss);
    }

    private String afficherCoeurs() {
        return afficherCoeurs(vies, false);
    }

    private String afficherCoeurs(int nombreVies, boolean coeurPerduEnGris) {
        StringBuilder coeurs = new StringBuilder();
        int maxAffiche = modeJeu == ModeJeu.DIFFICILE ? 1 : VIES_MAX;
        for (int i = 0; i < maxAffiche; i++) {
            if (coeurPerduEnGris && i == nombreVies - 1) {
                coeurs.append("🩶");
            } else if (i < nombreVies) {
                coeurs.append("❤️");
            } else {
                coeurs.append("♡");
            }
        }
        return coeurs.toString();
    }

    private TypeOperationQuestion choisirTypeOperationNormale() {
        List<TypeOperationQuestion> operations = getOperationsDisponibles();
        return operations.get(random.nextInt(operations.size()));
    }

    private String getNomTypeOperation(TypeOperationQuestion typeOperation) {
        switch (typeOperation) {
            case SOUSTRACTION:
                return getString(R.string.operation_soustrait);
            case MULTIPLICATION:
                return getString(R.string.operation_multiplie);
            case DIVISION:
                return getString(R.string.operation_divise);
            case ADDITION:
            default:
                return getString(R.string.operation_additionne);
        }
    }

    private void majAffichageScoreViesTimer() {
        textViewScore.setText(getString(R.string.format_score, score));
        textViewVies.setText(getString(R.string.format_vies, vies));
        textViewTimer.setText(getString(R.string.format_timer, tempsRestantSecondes));
        textViewNiveau.setText(getString(R.string.format_niveau, niveau));
        invalidateOptionsMenu();
    }

    private void terminerPartie(boolean victoire) {
        arreterTimer();
        partieTerminee = true;
        roundEnPause = false;
        activerSaisie(false);
        textViewPauseRound.setVisibility(android.view.View.GONE);
        debloquerEtoilesSiBesoin(victoire);
        demarrerMusiqueFinPartie();
        afficherDialogueNom(victoire);
    }

    private void debloquerEtoilesSiBesoin(boolean victoire) {
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_PROFIL, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (victoire && modeJeu == ModeJeu.CLASSIQUE) {
            editor.putBoolean(MainActivity.PREF_CLASSIQUE_TERMINE, true);
        }
        if (victoire && modeJeu == ModeJeu.DIFFICILE) {
            editor.putBoolean(MainActivity.PREF_DIFFICILE_TERMINE, true);
        }
        if (modeJeu == ModeJeu.ENDLESS && score >= SCORE_ENDLESS_ETOILE) {
            editor.putBoolean(MainActivity.PREF_ENDLESS_48, true);
        }
        editor.apply();
    }

    private void afficherDialogueNom(boolean victoire) {
        EditText inputNom = new EditText(this);
        inputNom.setHint(R.string.hint_nom_joueur);
        inputNom.setSingleLine(true);
        inputNom.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        int titre = victoire ? R.string.titre_victoire : R.string.titre_fin_partie;
        int message = victoire ? R.string.message_victoire : R.string.message_fin_partie;

        new AlertDialog.Builder(this)
                .setTitle(titre)
                .setMessage(getString(message, score))
                .setView(inputNom)
                .setCancelable(false)
                .setPositiveButton(R.string.bouton_enregistrer, (dialog, which) -> {
                    String nom = inputNom.getText().toString().trim();
                    if (nom.isEmpty()) {
                        nom = getString(R.string.nom_anonyme);
                    }
                    scoreDatabaseHelper.ajouterScore(nom + " - " + getNomModeJeu(), score);
                    arreterMusique();
                    startActivity(new Intent(this, HistoriqueActivity.class));
                    finish();
                })
                .setNegativeButton(R.string.bouton_rejouer, (dialog, which) -> nouvellePartie())
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.monmenu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem itemVies = menu.findItem(R.id.menu_vies);
        MenuItem itemScore = menu.findItem(R.id.menu_score);
        MenuItem itemTimer = menu.findItem(R.id.menu_timer);
        MenuItem itemNiveau = menu.findItem(R.id.menu_niveau);
        if (itemVies != null) itemVies.setTitle(getString(R.string.format_vies, vies));
        if (itemScore != null) itemScore.setTitle(getString(R.string.format_score, score));
        if (itemTimer != null) itemTimer.setTitle(getString(R.string.format_timer, tempsRestantSecondes));
        if (itemNiveau != null) itemNiveau.setTitle(getString(R.string.format_niveau, niveau));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_nouvelle_partie) {
            nouvellePartie();
            return true;
        }
        if (id == R.id.menu_highscores) {
            arreterMusique();
            startActivity(new Intent(this, HistoriqueActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        arreterTimer();
        arreterMusique();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!partieTerminee && !roundEnPause && textViewOperation != null && countDownTimer == null
                && textViewOperation.getText() != null && textViewOperation.getText().length() > 0) {
            demarrerTimerQuestion();
            demarrerMusiqueBoucle();
        }
    }

    @Override
    protected void onDestroy() {
        arreterTimer();
        handler.removeCallbacksAndMessages(null);
        libererMusique();
        super.onDestroy();
    }
}
