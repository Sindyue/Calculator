/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2;

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.android.calculator2.CalculatorEditText.OnTextSizeChangeListener;
import com.android.calculator2.CalculatorExpressionEvaluator.EvaluateCallback;
import com.android.calculator2.utils.StringUtil;

public class CalculatorSimple extends Activity implements OnTextSizeChangeListener, EvaluateCallback {

    private static final String NAME = CalculatorSimple.class.getName();
    /// M: limit the max input len
    private static final int MAX_INPUT_CHARACTERS = 100;

    // instance state keys
    private static final String KEY_CURRENT_STATE = NAME + "_currentState";
    private static final String KEY_CURRENT_EXPRESSION = NAME + "_currentExpression";

    /**
     * Constant for an invalid resource id.
     */
    public static final int INVALID_RES_ID = -1;

    private enum CalculatorState {
        INPUT, EVALUATE, RESULT, ERROR
    }

    private final TextWatcher mFormulaTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            setState(CalculatorState.INPUT);
            //mResultEtNew.setText(StringUtil.DecimalFormat2(editable.toString()));
        }
    };

    private final OnKeyListener mFormulaOnKeyListener = new OnKeyListener() {
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                        onEquals();
                    }
                    // ignore all other actions
                    return true;
            }
            return false;
        }
    };

    private final Editable.Factory mFormulaEditableFactory = new Editable.Factory() {
        @Override
        public Editable newEditable(CharSequence source) {
            final boolean isEdited = mCurrentState == CalculatorState.INPUT
                    || mCurrentState == CalculatorState.ERROR;
            return new CalculatorExpressionBuilder(source, mTokenizer, isEdited);
        }
    };

    private CalculatorState mCurrentState;
    private CalculatorExpressionTokenizer mTokenizer;
    private CalculatorExpressionEvaluator mEvaluator;

    private CalculatorEditText mFormulaEditText;
    private CalculatorEditText mResultEditText;
    private CalculatorEditText mResultEtNew;  //新需求的内容显示
    private View mEqualButton;

    private boolean isInputNew = true;      //用于控制显示新输入的数字还是计算结果
    private String currentResult = "";      //当前输入的表达式计算结果
    private String currentExpression = "";  //当前输入的表达式
    private String currentInputString = ""; //当前输入的字符串
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator_port_simple);

        mContext = this;

        mFormulaEditText = (CalculatorEditText) findViewById(R.id.formula);
        mResultEditText = (CalculatorEditText) findViewById(R.id.result);
        mResultEtNew = (CalculatorEditText) findViewById(R.id.result_et);

        mEqualButton = findViewById(R.id.pad_numeric).findViewById(R.id.eq);
        if (mEqualButton == null || mEqualButton.getVisibility() != View.VISIBLE) {
            mEqualButton = findViewById(R.id.pad_operator).findViewById(R.id.eq);
        }

        mTokenizer = new CalculatorExpressionTokenizer(this);
        mEvaluator = new CalculatorExpressionEvaluator(mTokenizer);

        savedInstanceState = savedInstanceState == null ? Bundle.EMPTY : savedInstanceState;
        setState(CalculatorState.values()[
                savedInstanceState.getInt(KEY_CURRENT_STATE, CalculatorState.INPUT.ordinal())]);
        mFormulaEditText.setText(mTokenizer.getLocalizedExpression(
                savedInstanceState.getString(KEY_CURRENT_EXPRESSION, "")));
        mEvaluator.evaluate(mFormulaEditText.getText(), this);

        mFormulaEditText.setEditableFactory(mFormulaEditableFactory);
        mResultEtNew.addTextChangedListener(mFormulaTextWatcher);
        mFormulaEditText.setOnKeyListener(mFormulaOnKeyListener);
        mFormulaEditText.setOnTextSizeChangeListener(this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        // If there's an animation in progress, end it immediately to ensure the state is
        // up-to-date before it is serialized.
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_CURRENT_STATE, mCurrentState.ordinal());
        outState.putString(KEY_CURRENT_EXPRESSION,
                mTokenizer.getNormalizedExpression(mFormulaEditText.getText().toString()));
    }

    private void setState(CalculatorState state) {
        if (mCurrentState != state) {
            mCurrentState = state;
        }
    }

    public void onButtonClick(View view) {
        String currentInputChar = view.getTag().toString();
        switch (view.getId()) {
            case R.id.eq:
                onEquals();
                break;
            case R.id.clr:
                onClear();
                break;
            case R.id.op_add:
            case R.id.op_sub:
                currentExpression = currentExpression + currentInputString;
                currentInputString = currentInputString + currentInputChar;
                //mFormulaEditText.append(view.getTag().toString());/* getText [BIRD_WEIMI_CALCULATOR] wangyueyue 20150326 modify*/
                mEvaluator.evaluate(currentExpression, CalculatorSimple.this);
                //// TODO: 2018/2/26  点击加减号时，计算结果，并保留此时的运算符
                isInputNew = true;
                mResultEtNew.setText(StringUtil.DecimalFormat2(currentResult));
                break;
            case R.id.dec_point: //点，点击点后，替换点后面的字符串
                if (isInputNew) {
                    currentInputString = currentInputChar;
                    isInputNew = false;
                } else {
                    if (currentInputString.contains(".")) {
                        showMaxToast();
                        return;
                    } else {
                        currentInputString = currentInputString + currentInputChar;
                        mResultEtNew.setText(StringUtil.DecimalFormat2(currentInputString));
                    }
                }
                break;
            default://数字
                if (isInputNew) {
                    if (currentInputChar.equals("0")) {
                        return;
                    }
                    currentInputString = currentInputChar;
                    isInputNew = false;
                } else {
                    if (currentInputString.contains(".")) {
                        int index = currentInputString.indexOf(".");
                        //小数点后超过两位或者小数点前超过10位，直接返回
                        if (currentInputString.substring(0, index).length() > 10) {
                            showMaxToast();
                            return;
                        } else if (currentInputString.substring(index, currentInputString.length() - 1).length() >= 2) {
                            showMaxToast();
                            return;
                        } else {
                            currentInputString = currentInputString + currentInputChar;
                        }
                    } else if (currentInputString.length() < 10) {
                        currentInputString = currentInputString + currentInputChar;
                    } else {
                        showMaxToast();
                        return;
                    }
                }
                mResultEtNew.setText(StringUtil.DecimalFormat2(currentInputString));
                break;
        }
    }

    /**
     * 显示达到最大值的限定
     */
    private void showMaxToast() {
        mHandler.removeMessages(MAX_DIGITS_ALERT);
        mHandler.sendEmptyMessageDelayed(MAX_DIGITS_ALERT, TOAST_INTERVAL);
        vibrate();
    }

    @Override
    public void onEvaluate(String expr, String result, int errorResourceId) {
        if (mCurrentState == CalculatorState.INPUT) {
            currentResult = result;
            mResultEditText.setText(result);
        } else if (errorResourceId != INVALID_RES_ID) {
            onError(errorResourceId);
        } else if (!TextUtils.isEmpty(result)) {
            onResult(result);
        } else if (mCurrentState == CalculatorState.EVALUATE) {
            // The current expression cannot be evaluated -> return to the input state.
            setState(CalculatorState.INPUT);
        }

        mFormulaEditText.requestFocus();
    }

    @Override
    public void onTextSizeChanged(final TextView textView, float oldSize) {
        if (mCurrentState != CalculatorState.INPUT) {
            // Only animate text changes that occur from user input.
            return;
        }

// Calculate the values needed to perform the scale and translation animations,
// maintaining the same apparent baseline for the displayed text.
        final float textScale = oldSize / textView.getTextSize();
        final float translationX = (1.0f - textScale) *
                (textView.getWidth() / 2.0f - textView.getPaddingEnd());
        final float translationY = (1.0f - textScale) *
                (textView.getHeight() / 2.0f - textView.getPaddingBottom());

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(textView, View.SCALE_X, textScale, 1.0f),
                ObjectAnimator.ofFloat(textView, View.SCALE_Y, textScale, 1.0f),
                ObjectAnimator.ofFloat(textView, View.TRANSLATION_X, translationX, 0.0f),
                ObjectAnimator.ofFloat(textView, View.TRANSLATION_Y, translationY, 0.0f));
        animatorSet.setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    private void onEquals() {
        if (mCurrentState == CalculatorState.INPUT) {
            setState(CalculatorState.EVALUATE);
            mEvaluator.evaluate(currentExpression, this);
        }
        //// TODO: 2018/2/26   “=”号键切换为“完成”键
        isInputNew = true;

        mResultEtNew.setText(StringUtil.DecimalFormat2(currentResult));
    }

    private void onClear() {
        currentExpression = "0";  //当前输入的表达式
        currentInputString = "0"; //当前输入的字符串
        mResultEtNew.setText("0.00");
        isInputNew = true;
    }

    private void onError(final int errorResourceId) {
        if (mCurrentState != CalculatorState.EVALUATE) {
            // Only animate error on evaluate.
            currentResult = getString(errorResourceId);
            mResultEditText.setText(errorResourceId);
            return;
        }

        /** M: [ALPS01798852] Cannot start this animator on a detached or null view @{ */
        if (mEqualButton != null && mEqualButton.isAttachedToWindow()) {
            setState(CalculatorState.ERROR);
            currentResult = getString(errorResourceId);
            mResultEditText.setText(errorResourceId);
        } else {
            currentResult = getString(errorResourceId);
            mResultEditText.setText(errorResourceId);
        }
    }

    private void onResult(final String result) {
        // Use a value animator to fade to the final text color over the course of the animation.
        final int resultTextColor = mResultEditText.getCurrentTextColor();
        final int formulaTextColor = mFormulaEditText.getCurrentTextColor();
        final ValueAnimator textColorAnimator =
                ValueAnimator.ofObject(new ArgbEvaluator(), resultTextColor, formulaTextColor);
        textColorAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mResultEditText.setTextColor((int) valueAnimator.getAnimatedValue());
            }
        });

        // Finally update the formula to use the current result.
        currentResult = result;
        mResultEditText.setText(result);
        // Finally update the formula to use the current result.
        mFormulaEditText.setText(result);
        setState(CalculatorState.RESULT);
    }

    /**
     * M: vibrate when input more than limited @{
     */
    protected final static int MAX_DIGITS_ALERT = 0;
    protected final static int MAX_INPUT_ALERT = 1;
    protected final static int TOAST_INTERVAL = 500;
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MAX_DIGITS_ALERT:
                    Toast.makeText(mContext, R.string.max_digits_alert,
                            Toast.LENGTH_SHORT).show();
                    break;
                case MAX_INPUT_ALERT:
                    Toast.makeText(mContext, R.string.max_input_alert,
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    throw new IllegalArgumentException("Invalid message.what = "
                            + msg.what);
            }
            return false;
        }
    });

    public void vibrate() {
        Vibrator vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(new long[]{100, 100}, -1);
        }
    }
}
