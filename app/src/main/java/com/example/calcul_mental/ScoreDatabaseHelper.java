package com.example.calcul_mental;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class ScoreDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "scores.db";
    private static final int DATABASE_VERSION = 2;

    public static final String CATEGORIE_STANDARD = "standard";
    public static final String CATEGORIE_CHRONO = "chrono";

    private static final String TABLE_SCORES = "scores";
    private static final String COL_ID = "id";
    private static final String COL_NOM = "nom";
    private static final String COL_SCORE = "score";
    private static final String COL_CATEGORIE = "categorie";
    private static final String COL_DATE = "date_creation";

    public ScoreDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_SCORES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NOM + " TEXT NOT NULL, " +
                COL_SCORE + " INTEGER NOT NULL, " +
                COL_CATEGORIE + " TEXT NOT NULL DEFAULT '" + CATEGORIE_STANDARD + "', " +
                COL_DATE + " INTEGER NOT NULL" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_SCORES + " ADD COLUMN " + COL_CATEGORIE + " TEXT NOT NULL DEFAULT '" + CATEGORIE_STANDARD + "'");
        }
    }

    public void ajouterScore(String nom, int score) {
        ajouterScore(nom, score, CATEGORIE_STANDARD);
    }

    public void ajouterScore(String nom, int score, String categorie) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NOM, nom);
        values.put(COL_SCORE, score);
        values.put(COL_CATEGORIE, categorie == null ? CATEGORIE_STANDARD : categorie);
        values.put(COL_DATE, System.currentTimeMillis());
        db.insert(TABLE_SCORES, null, values);
        db.close();
    }

    public List<Score> recupererTopScores(int limite) {
        return recupererTopScores(limite, CATEGORIE_STANDARD);
    }

    public List<Score> recupererTopScores(int limite, String categorie) {
        List<Score> scores = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_SCORES,
                new String[]{COL_NOM, COL_SCORE},
                COL_CATEGORIE + " = ?",
                new String[]{categorie == null ? CATEGORIE_STANDARD : categorie},
                null,
                null,
                COL_SCORE + " DESC, " + COL_DATE + " ASC",
                String.valueOf(limite)
        );

        while (cursor.moveToNext()) {
            String nom = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOM));
            int score = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SCORE));
            scores.add(new Score(nom, score));
        }

        cursor.close();
        db.close();
        return scores;
    }

    public static class Score {
        private final String nom;
        private final int score;

        public Score(String nom, int score) {
            this.nom = nom;
            this.score = score;
        }

        public String getNom() {
            return nom;
        }

        public int getScore() {
            return score;
        }
    }
}
