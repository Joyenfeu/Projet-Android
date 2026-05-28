package com.example.calcul_mental;

public enum TypeOperation {
    ADD("+"),
    SUBSTRACT("-"),
    DIVIDE("/"),
    MULTIPLY("x");

    private String symbole;

    TypeOperation(String s) {
        symbole=s;
    }

    public String getSymbole() {
        return symbole;
    }
}
