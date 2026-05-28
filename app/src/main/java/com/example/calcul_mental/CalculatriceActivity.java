package com.example.calcul_mental;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CalculatriceActivity extends AppCompatActivity {
    private static final int VIES_DEPART = 3;
    private static final int VIES_MAX = 4;
    private static final int TEMPS_DEPART_SECONDES = 20;
    private static final int TEMPS_MINIMUM_SECONDES = 10;
    private static final int TEMPS_BOSS_SECONDES = 25;
    private static final int DELAI_PAUSE_ROUND_MS = 4000;
    private static final int DELAI_PAUSE_DEMARRAGE_MS = 2500;
    private static final int DELAI_ANIMATION_COEUR_MS = 1000;
    private static final int DUREE_INTRO_MS = 275000;
    private static final int VERIFICATION_MUSIQUE_MS = 2000;

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
    private final Handler musicHandler = new Handler(Looper.getMainLooper());
    private PhaseMusique phaseMusique = PhaseMusique.ARRETEE;
    private boolean lecteurEnPreparation = false;

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
    private boolean musiqueSuspendueParPause = false;

    private enum TypeOperationQuestion {
        ADDITION, SOUSTRACTION, MULTIPLICATION, DIVISION, BOSS_GRANDE_OPERATION, BOSS_INCONNU, BOSS_OPERATION_DANS_OPERATION
    }

    private enum PhaseMusique {
        ARRETEE, INTRO, BOUCLE, GAME_OVER
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

    private void demarrerMusiqueJeu() {
        musiqueJeuActive = true;
        musiqueSuspendueParPause = false;
        phaseMusique = PhaseMusique.INTRO;
        lancerMusique(R.raw.musique_intro, false, PhaseMusique.INTRO, this::demarrerMusiqueBoucle);
        demarrerSurveillanceMusique();

        // Sécurité : sur certains téléphones, MediaPlayer peut ne pas appeler onCompletion
        // après un long MP3. On force donc le passage à la boucle à la fin de l'intro.
        musicHandler.postDelayed(() -> {
            if (musiqueJeuActive && !partieTerminee && phaseMusique == PhaseMusique.INTRO) {
                demarrerMusiqueBoucle();
            }
        }, DUREE_INTRO_MS);
    }

    private void demarrerMusiqueBoucle() {
        if (!musiqueJeuActive || partieTerminee) {
            return;
        }
        phaseMusique = PhaseMusique.BOUCLE;
        lancerMusique(R.raw.musique_loop, true, PhaseMusique.BOUCLE, null);
        demarrerSurveillanceMusique();
    }

    private void demarrerMusiqueFinPartie() {
        musiqueJeuActive = false;
        musiqueSuspendueParPause = false;
        musicHandler.removeCallbacksAndMessages(null);
        phaseMusique = PhaseMusique.GAME_OVER;
        lancerMusique(R.raw.musique_game_over, false, PhaseMusique.GAME_OVER, null);
    }

    private void lancerMusique(int resId, boolean enBoucle, PhaseMusique phaseDemandee, Runnable actionFin) {
        libererLecteurActuel();
        phaseMusique = phaseDemandee;
        lecteurEnPreparation = true;

        try {
            AssetFileDescriptor afd = getResources().openRawResourceFd(resId);
            if (afd == null) {
                lecteurEnPreparation = false;
                return;
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            mediaPlayer.setLooping(enBoucle);
            mediaPlayer.setOnPreparedListener(mp -> {
                lecteurEnPreparation = false;
                try {
                    mp.start();
                } catch (IllegalStateException e) {
                    relancerMusiqueSiNecessaire();
                }
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                if (actionFin != null && phaseMusique == phaseDemandee) {
                    actionFin.run();
                }
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                lecteurEnPreparation = false;
                relancerMusiqueSiNecessaire();
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            lecteurEnPreparation = false;
            libererLecteurActuel();
            relancerMusiqueSiNecessaire();
        }
    }

    private void demarrerSurveillanceMusique() {
        musicHandler.removeCallbacksAndMessages(null);
        musicHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!musiqueJeuActive || partieTerminee) {
                    return;
                }

                boolean doitRelancer = mediaPlayer == null && !lecteurEnPreparation;
                if (mediaPlayer != null && !lecteurEnPreparation) {
                    try {
                        if (phaseMusique == PhaseMusique.INTRO && mediaPlayer.getCurrentPosition() >= DUREE_INTRO_MS - 500) {
                            demarrerMusiqueBoucle();
                            return;
                        }
                        doitRelancer = !mediaPlayer.isPlaying();
                    } catch (IllegalStateException e) {
                        doitRelancer = true;
                    }
                }

                if (doitRelancer) {
                    relancerMusiqueSiNecessaire();
                }

                musicHandler.postDelayed(this, VERIFICATION_MUSIQUE_MS);
            }
        }, VERIFICATION_MUSIQUE_MS);
    }

    private void relancerMusiqueSiNecessaire() {
        if (!musiqueJeuActive || partieTerminee) {
            return;
        }

        // Si l'intro plante ou se termine bizarrement, on passe à la boucle plutôt
        // que de recommencer la musique depuis zéro en plein milieu de partie.
        demarrerMusiqueBoucle();
    }

    private void arreterMusique() {
        musiqueJeuActive = false;
        musiqueSuspendueParPause = false;
        phaseMusique = PhaseMusique.ARRETEE;
        musicHandler.removeCallbacksAndMessages(null);
        libererLecteurActuel();
    }

    private void libererLecteurActuel() {
        lecteurEnPreparation = false;
        if (mediaPlayer != null) {
            try {
                mediaPlayer.setOnPreparedListener(null);
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
        arreterMusique();
    }

    private void suspendreMusiquePourPause() {
        if (!musiqueJeuActive || mediaPlayer == null || lecteurEnPreparation) {
            return;
        }
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                musiqueSuspendueParPause = true;
            }
        } catch (IllegalStateException ignored) {
            musiqueSuspendueParPause = false;
            relancerMusiqueSiNecessaire();
        }
    }

    private void reprendreMusiqueApresPause() {
        if (!musiqueJeuActive || partieTerminee) {
            return;
        }
        if (mediaPlayer == null) {
            relancerMusiqueSiNecessaire();
            return;
        }
        try {
            if (musiqueSuspendueParPause && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                musiqueSuspendueParPause = false;
            }
        } catch (IllegalStateException e) {
            musiqueSuspendueParPause = false;
            relancerMusiqueSiNecessaire();
        }
        demarrerSurveillanceMusique();
    }

    private void nouvellePartie() {
        arreterTimer();
        handler.removeCallbacksAndMessages(null);
        score = 0;
        vies = VIES_DEPART;
        niveau = 1;
        tempsRestantSecondes = TEMPS_DEPART_SECONDES;
        partieTerminee = false;
        roundEnPause = false;
        questionBoss = false;
        textViewPauseRound.setVisibility(View.GONE);
        activerSaisie(true);
        demarrerMusiqueJeu();
        afficherPauseAvantQuestion(getString(R.string.message_debut_partie), true);
        majAffichageScoreViesTimer();
    }

    private void activerSaisie(boolean actif) {
        editTextReponse.setEnabled(actif);
        boutonValider.setEnabled(actif);
        boutonPasser.setEnabled(actif);
    }

    private int getNiveauDifficulte() {
        return Math.min(5, 1 + ((niveau - 1) / 5));
    }

    private boolean estNiveauBoss() {
        return niveau % 16 == 0;
    }

    private void genererCalcul() {
        roundEnPause = false;
        activerSaisie(true);
        textViewPauseRound.setVisibility(View.GONE);

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
        List<TypeOperationQuestion> operations = new ArrayList<>();
        int difficulte = getNiveauDifficulte();
        operations.add(TypeOperationQuestion.ADDITION);
        if (difficulte >= 2) {
            operations.add(TypeOperationQuestion.SOUSTRACTION);
        }
        if (difficulte >= 3) {
            operations.add(TypeOperationQuestion.MULTIPLICATION);
        }
        if (difficulte >= 4) {
            operations.add(TypeOperationQuestion.DIVISION);
        }

        if (typeOperationAnnoncee != null && operations.contains(typeOperationAnnoncee)) {
            typeOperationCourante = typeOperationAnnoncee;
            typeOperationAnnoncee = null;
        } else {
            typeOperationCourante = operations.get(random.nextInt(operations.size()));
        }
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
                int maxDiviseur = difficulte >= 5 ? 250 : 12;
                int maxQuotient = difficulte >= 5 ? 250 : 12;
                b = random.nextInt(maxDiviseur) + 1;
                bonneReponse = random.nextInt(maxQuotient + 1);
                a = b * bonneReponse;
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

    private void genererCalculBoss() {
        int typeBoss = random.nextInt(3);
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

    private void demarrerTimerQuestion() {
        arreterTimer();
        if (questionBoss) {
            tempsQuestionSecondes = TEMPS_BOSS_SECONDES;
        } else {
            tempsQuestionSecondes = Math.max(TEMPS_MINIMUM_SECONDES, TEMPS_DEPART_SECONDES - score);
        }
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
        niveau = score + 1;
        boolean difficulteAugmente = getNiveauDifficulte() > ancienneDifficulte;

        if (bossVaincu && vies < VIES_MAX) {
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
        textViewPauseRound.setVisibility(View.VISIBLE);
        textViewPauseRound.animate().alpha(1f).setDuration(250).start();

        handler.postDelayed(() -> {
            if (partieTerminee) {
                return;
            }
            vies = Math.max(0, viesAvantPerte - 1);
            majAffichageScoreViesTimer();

            if (vies <= 0) {
                terminerPartie();
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
        if (estNiveauBoss()) {
            message.append("\n").append(getString(R.string.message_entree_boss));
        }
        message.append("\n\n").append(prochainMessageOperation);

        textViewPauseRound.setText(message.toString());
        textViewPauseRound.setAlpha(0f);
        textViewPauseRound.setVisibility(View.VISIBLE);
        textViewPauseRound.animate().alpha(1f).setDuration(300).start();
        textViewOperation.setText("");
        editTextReponse.setText("");

        handler.postDelayed(this::genererCalcul, demarrage ? DELAI_PAUSE_DEMARRAGE_MS : DELAI_PAUSE_ROUND_MS);
    }

    private String afficherCoeurs() {
        return afficherCoeurs(vies, false);
    }

    private String afficherCoeurs(int nombreVies, boolean coeurPerduEnGris) {
        StringBuilder coeurs = new StringBuilder();
        for (int i = 0; i < VIES_MAX; i++) {
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
        List<TypeOperationQuestion> operations = new ArrayList<>();
        int difficulte = getNiveauDifficulte();
        operations.add(TypeOperationQuestion.ADDITION);
        if (difficulte >= 2) {
            operations.add(TypeOperationQuestion.SOUSTRACTION);
        }
        if (difficulte >= 3) {
            operations.add(TypeOperationQuestion.MULTIPLICATION);
        }
        if (difficulte >= 4) {
            operations.add(TypeOperationQuestion.DIVISION);
        }
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

    private void terminerPartie() {
        arreterTimer();
        partieTerminee = true;
        roundEnPause = false;
        activerSaisie(false);
        textViewPauseRound.setVisibility(View.GONE);
        demarrerMusiqueFinPartie();
        afficherDialogueNom();
    }

    private void afficherDialogueNom() {
        EditText inputNom = new EditText(this);
        inputNom.setHint(R.string.hint_nom_joueur);
        inputNom.setSingleLine(true);
        inputNom.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        new AlertDialog.Builder(this)
                .setTitle(R.string.titre_fin_partie)
                .setMessage(getString(R.string.message_fin_partie, score))
                .setView(inputNom)
                .setCancelable(false)
                .setPositiveButton(R.string.bouton_enregistrer, (dialog, which) -> {
                    String nom = inputNom.getText().toString().trim();
                    if (nom.isEmpty()) {
                        nom = getString(R.string.nom_anonyme);
                    }
                    scoreDatabaseHelper.ajouterScore(nom, score);
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
        if (itemVies != null) {
            itemVies.setTitle(getString(R.string.format_vies, vies));
        }
        if (itemScore != null) {
            itemScore.setTitle(getString(R.string.format_score, score));
        }
        if (itemTimer != null) {
            itemTimer.setTitle(getString(R.string.format_timer, tempsRestantSecondes));
        }
        if (itemNiveau != null) {
            itemNiveau.setTitle(getString(R.string.format_niveau, niveau));
        }
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
        suspendreMusiquePourPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reprendreMusiqueApresPause();
        if (!partieTerminee && !roundEnPause && textViewOperation != null && countDownTimer == null
                && textViewOperation.getText() != null && textViewOperation.getText().length() > 0) {
            demarrerTimerQuestion();
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
