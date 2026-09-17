package com.example.calculatorr

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CalculatorActivity : AppCompatActivity() {

    private lateinit var tvDisplay: TextView
    private var currentInput = ""
    private var firstNumber = 0.0
    private var operator = ""
    private var isNewInput = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)

        tvDisplay = findViewById(R.id.tvDisplay)

        val numberIds = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )

        for (id in numberIds) {
            findViewById<Button>(id).setOnClickListener { v ->
                val digit = (v as Button).text.toString()
                if (isNewInput) {
                    currentInput = digit
                    isNewInput = false
                } else {
                    currentInput += digit
                }
                tvDisplay.text = currentInput
            }
        }

        findViewById<Button>(R.id.btnAdd).setOnClickListener { setOperator("+") }
        findViewById<Button>(R.id.btnSub).setOnClickListener { setOperator("-") }
        findViewById<Button>(R.id.btnMul).setOnClickListener { setOperator("*") }
        findViewById<Button>(R.id.btnDiv).setOnClickListener { setOperator("/") }

        findViewById<Button>(R.id.btnEq).setOnClickListener { calculate() }

        findViewById<Button>(R.id.btnC).setOnClickListener {
            currentInput = ""
            firstNumber = 0.0
            operator = ""
            isNewInput = true
            tvDisplay.text = "0"
        }
    }

    private fun setOperator(op: String) {
        if (currentInput.isNotEmpty()) {
            firstNumber = currentInput.toDouble()
            operator = op
            isNewInput = true
        }
    }

    private fun calculate() {
        if (currentInput.isEmpty() || operator.isEmpty()) return

        val secondNumber = currentInput.toDouble()
        var result = 0.0

        when (operator) {
            "+" -> result = firstNumber + secondNumber
            "-" -> result = firstNumber - secondNumber
            "*" -> result = firstNumber * secondNumber
            "/" -> {
                if (secondNumber == 0.0) {
                    tvDisplay.text = "Ошибка"
                    currentInput = ""
                    operator = ""
                    isNewInput = true
                    return
                }
                result = firstNumber / secondNumber
            }
        }

        tvDisplay.text = if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            result.toString()
        }

        currentInput = result.toString()
        isNewInput = true
        operator = ""
    }
}