package com.example.uttoapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class CalculatorActivity extends AppCompatActivity {

    private TextView tvDisplay;
    private StringBuilder expression = new StringBuilder();
    private boolean lastResultShown = false; // flag to know if "=" was pressed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        tvDisplay = findViewById(R.id.tvDisplay);

        // Numbers
        setNumberClick(R.id.btn0, "0");
        setNumberClick(R.id.btn1, "1");
        setNumberClick(R.id.btn2, "2");
        setNumberClick(R.id.btn3, "3");
        setNumberClick(R.id.btn4, "4");
        setNumberClick(R.id.btn5, "5");
        setNumberClick(R.id.btn6, "6");
        setNumberClick(R.id.btn7, "7");
        setNumberClick(R.id.btn8, "8");
        setNumberClick(R.id.btn9, "9");
        setNumberClick(R.id.btnDot, ".");

        // Operators
        setOperatorClick(R.id.btnPlus, "+");
        setOperatorClick(R.id.btnMinus, "-");
        setOperatorClick(R.id.btnMultiply, "*");
        setOperatorClick(R.id.btnDivide, "/");

        // Clear
        Button btnClear = findViewById(R.id.btnClear);
        btnClear.setOnClickListener(v -> {
            expression.setLength(0);
            tvDisplay.setText("0");
            lastResultShown = false;
        });

        // Delete
        Button btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(v -> {
            if (expression.length() > 0) {
                expression.deleteCharAt(expression.length() - 1);
                tvDisplay.setText(expression.length() > 0 ? expression.toString() : "0");
            }
        });

        // Equals
        Button btnEquals = findViewById(R.id.btnEquals);
        btnEquals.setOnClickListener(v -> {
            try {
                double result = eval(expression.toString());
                tvDisplay.setText(String.valueOf(result));

                expression.setLength(0);
                expression.append(result); // store result for continued calc
                lastResultShown = true;
            } catch (Exception e) {
                tvDisplay.setText("Error");
                expression.setLength(0);
                lastResultShown = false;
            }
        });
    }

    private void setNumberClick(int id, String value) {
        Button btn = findViewById(id);
        btn.setOnClickListener(v -> {
            if (lastResultShown) {
                // If last thing shown was result, and user presses a number,
                // start fresh
                expression.setLength(0);
                lastResultShown = false;
            }
            expression.append(value);
            tvDisplay.setText(expression.toString());
        });
    }

    private void setOperatorClick(int id, String value) {
        Button btn = findViewById(id);
        btn.setOnClickListener(v -> {
            lastResultShown = false; // allow chaining after result
            expression.append(value);
            tvDisplay.setText(expression.toString());
        });
    }

    // Simple evaluator (you can replace with better parser if needed)
    private double eval(String exp) {
        try {
            // Clean up expression (replace weird symbols if any)
            exp = exp.replace("÷", "/").replace("×", "*");

            return parseExpression(exp);
        } catch (Exception e) {
            return 0; // fallback if user enters something invalid
        }
    }

    // Grammar-based parsing
    private double parseExpression(String exp) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < exp.length()) ? exp.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < exp.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            // Expression = Term | Expression `+` Term | Expression `-` Term
            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            // Term = Factor | Term `*` Factor | Term `/` Factor
            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            // Factor = `+` Factor | `-` Factor | Number | `(` Expression `)`
            double parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(exp.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                return x;
            }
        }.parse();
    }

}
