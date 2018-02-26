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

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewAnimationUtils;
import android.view.ViewGroupOverlay;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.android.calculator2.CalculatorEditText.OnTextSizeChangeListener;
import com.android.calculator2.CalculatorExpressionEvaluator.EvaluateCallback;

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
            mEvaluator.evaluate(editable, CalculatorSimple.this);
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

    private View mDisplayView;
    private CalculatorEditText mFormulaEditText;
    private CalculatorEditText mResultEditText;
    private CalculatorEditText mResultEtNew;  //新需求的内容显示
    private View mClearButton;
    private View mEqualButton;

    private boolean isInputNew = true;  //用于控制显示新输入的数字还是计算结果

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator_port_simple);

        mDisplayView = findViewById(R.id.display);
        mFormulaEditText = (CalculatorEditText) findViewById(R.id.formula);
        mResultEditText = (CalculatorEditText) findViewById(R.id.result);
        mResultEtNew = (CalculatorEditText) findViewById(R.id.result_et);
        mClearButton = findViewById(R.id.clr);

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
        mFormulaEditText.addTextChangedListener(mFormulaTextWatcher);
        mFormulaEditText.setOnKeyListener(mFormulaOnKeyListener);
        mFormulaEditText.setOnTextSizeChangeListener(this);
        /** M: add input length limitation @{ */
        mFormulaEditText.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(MAX_INPUT_CHARACTERS) {
                    private static final int MAX_SUCCESSIVE_DIGITS = 10;

                    @Override
                    public CharSequence filter(CharSequence source, int start, int end,
                                               Spanned dest,
                                               int dstart, int dend) {
                        if (mCurrentState == CalculatorState.INPUT) {
                            int keep = getMax() - (dest.length() - (dend - dstart));
                            if (keep >= end - start) {
                                if (TextUtils.isDigitsOnly(source)) {
                                    int digitKeep = MAX_SUCCESSIVE_DIGITS
                                            - (getCountLen(dest, dstart, dend) - (dend - dstart));
                                    if (digitKeep >= end - start) {
                                        return null;
                                    } else {
                                        mHandler.removeMessages(MAX_DIGITS_ALERT);
                                        mHandler.sendEmptyMessageDelayed(MAX_DIGITS_ALERT, TOAST_INTERVAL);
                                        vibrate();
                                        return "";
                                    }
                                } else {
                                    return null;
                                }
                            } else {
                                mHandler.removeMessages(MAX_INPUT_ALERT);
                                mHandler.sendEmptyMessageDelayed(MAX_INPUT_ALERT, TOAST_INTERVAL);
                                vibrate();
                                return "";
                            }
                        } else {
                            return null;
                        }
                    }

                    private int getCountLen(Spanned str, int start, int end) {
                        int len = str.length();
                        for (int i = len - 1; i > 0; i--) {
                            if (!Character.isDigit(str.charAt(i))) {
                                if (start <= i && i <= end) {
                                    continue;
                                } else {
                                    len = len - (i + 1);
                                    break;
                                }
                            }
                        }
                        return len;
                    }
                }
        });
        /** @} */
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

            if (state == CalculatorState.RESULT || state == CalculatorState.ERROR) {
                //mClearButton.setVisibility(View.VISIBLE);
            } else {
                //mClearButton.setVisibility(View.GONE);
            }

            if (state == CalculatorState.ERROR) {
                final int errorColor = getResources().getColor(R.color.calculator_error_color);
                mFormulaEditText.setTextColor(errorColor);
                mResultEditText.setTextColor(errorColor);
                /*[BIRD_WEIMI_CALCULATOR] wangyueyue 20150326 begin*/
                //getWindow().setStatusBarColor(errorColor);
                /*[BIRD_WEIMI_CALCULATOR] wangyueyue 20150326 end*/
            } else {
                mFormulaEditText.setTextColor(
                        getResources().getColor(R.color.display_formula_text_color));
                mResultEditText.setTextColor(
                        getResources().getColor(R.color.display_result_text_color));
                /*[BIRD_WEIMI_CALCULATOR] wangyueyue 20150326 begin*/
                //getWindow().setStatusBarColor(getResources().getColor(R.color.calculator_accent_color));
                /*[BIRD_WEIMI_CALCULATOR] wangyueyue 20150326 end*/
            }
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
    }

    public void onButtonClick(View view) {
        switch (view.getId()) {
            case R.id.eq:
                onEquals();
                break;
            case R.id.del:
                onDelete();
                break;
            case R.id.clr:
                onClear();
                break;
            case R.id.fun_cos:
            case R.id.fun_ln:
            case R.id.fun_log:
            case R.id.fun_sin:
            case R.id.fun_tan:
                // Add left parenthesis after functions.
                mFormulaEditText.append(view.getTag().toString() + "("); /* getText [BIRD_WEIMI_CALCULATOR] wangyueyue 20150326 modify*/
                break;
            case R.id.op_add:
            case R.id.op_sub:
                mFormulaEditText.append(view.getTag().toString());/* getText [BIRD_WEIMI_CALCULATOR] wangyueyue 20150326 modify*/
                //// TODO: 2018/2/26  点击加减号时，计算结果，并保留此时的运算符
                isInputNew = true;
                mResultEtNew.setText(mResultEditText.getText().toString());
                break;
            default:
                String strTemp = view.getTag().toString();
                mFormulaEditText.append(strTemp);/* getText [BIRD_WEIMI_CALCULATOR] wangyueyue 20150326 modify*/
                if (isInputNew) {
                    isInputNew = false;
                    mResultEtNew.setText(strTemp);
                } else {
                    mResultEtNew.append(strTemp);
                }
                break;
        }
    }

    @Override
    public void onEvaluate(String expr, String result, int errorResourceId) {
        if (mCurrentState == CalculatorState.INPUT) {
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
            mEvaluator.evaluate(mFormulaEditText.getText(), this);
        }
        //// TODO: 2018/2/26   “=”号键切换为“完成”键
        isInputNew = true;
        mResultEtNew.setText(mResultEditText.getText().toString());
    }

    private void onDelete() {
        // Delete works like backspace; remove the last character from the expression.
        final Editable formulaText = mFormulaEditText.getEditableText();
        /*[BIRD_WEIMI_CALCULATOR] wangyueyue 20150326 end*/
        String oldText = mFormulaEditText.getEditableText().toString();
        char delete_char = '\0'; 
        /*[BIRD_WEIMI_CALCULATOR] wangyueyue 20150326 end*/
        final int formulaLength = formulaText.length();
        if (formulaLength > 0) {
            delete_char = oldText.charAt(formulaLength - 1); /*[BIRD_WEIMI_CALCULATOR] wangyueyue 20150326 add*/
            formulaText.delete(formulaLength - 1, formulaLength);
        }

        /*[BIRD_WEIMI_CALCULATOR] wangyueyue 20150326 begin*/
        String newText = mFormulaEditText.getEditableText().toString();
        int newTextLength = formulaText.length();
        if (!newText.equals(formulaText) && newTextLength > 0) {
            while (delete_char == '(' && newTextLength > 0 && newText.charAt(newTextLength - 1) <= 'z' && newText.charAt(newTextLength - 1) >= 'a' && newText.charAt(newTextLength - 1) != 'e') {
                formulaText.delete(newTextLength - 1, newTextLength);
                newTextLength--;
            }
        }
        /*[BIRD_WEIMI_CALCULATOR] wangyueyue 20150326 end*/
    }

    private void reveal(View sourceView, int colorRes, AnimatorListener listener) {
        final ViewGroupOverlay groupOverlay =
                (ViewGroupOverlay) getWindow().getDecorView().getOverlay();

        final Rect displayRect = new Rect();
        mDisplayView.getGlobalVisibleRect(displayRect);

        // Make reveal cover the display and status bar.
        final View revealView = new View(this);
        revealView.setBottom(displayRect.bottom);
        revealView.setLeft(displayRect.left);
        revealView.setRight(displayRect.right);
        revealView.setBackgroundColor(getResources().getColor(colorRes));
        groupOverlay.add(revealView);

        final int[] clearLocation = new int[2];
        sourceView.getLocationInWindow(clearLocation);
        clearLocation[0] += sourceView.getWidth() / 2;
        clearLocation[1] += sourceView.getHeight() / 2;

        final int revealCenterX = clearLocation[0] - revealView.getLeft();
        final int revealCenterY = clearLocation[1] - revealView.getTop();

        final double x1_2 = Math.pow(revealView.getLeft() - revealCenterX, 2);
        final double x2_2 = Math.pow(revealView.getRight() - revealCenterX, 2);
        final double y_2 = Math.pow(revealView.getTop() - revealCenterY, 2);
        final float revealRadius = (float) Math.max(Math.sqrt(x1_2 + y_2), Math.sqrt(x2_2 + y_2));

        final Animator revealAnimator =
                ViewAnimationUtils.createCircularReveal(revealView,
                        revealCenterX, revealCenterY, 0.0f, revealRadius);
        revealAnimator.setDuration(
                getResources().getInteger(android.R.integer.config_longAnimTime));

        final Animator alphaAnimator = ObjectAnimator.ofFloat(revealView, View.ALPHA, 0.0f);
        alphaAnimator.setDuration(
                getResources().getInteger(android.R.integer.config_mediumAnimTime));
        alphaAnimator.addListener(listener);
    }

    private void onClear() {
        if (TextUtils.isEmpty(mFormulaEditText.getText())) {
            return;
        }

      /*  final View sourceView = mClearButton.getVisibility() == View.VISIBLE
                ? mClearButton : mDeleteButton;*/
       /*[BIRD_WEIMI_CALCULATOR] wangyueyue 20150326 begin*/
        /*reveal(sourceView, R.color.calculator_accent_color, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mFormulaEditText.getEditableText().clear();
            }
        });*/
        mFormulaEditText.getEditableText().clear();
        mResultEtNew.getEditableText().clear();
        /*[BIRD_WEIMI_CALCULATOR] wangyueyue 20150326 end*/
    }

    private void onError(final int errorResourceId) {
        if (mCurrentState != CalculatorState.EVALUATE) {
            // Only animate error on evaluate.
            mResultEditText.setText(errorResourceId);
            return;
        }

        /** M: [ALPS01798852] Cannot start this animator on a detached or null view @{ */
        if (mEqualButton != null && mEqualButton.isAttachedToWindow()) {
            /*[BIRD_WEIMI_CALCULATOR] wangyueyue 20150326 begin*/
            /*reveal(mEqualButton, R.color.calculator_error_color,
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            setState(CalculatorState.ERROR);
                            mResultEditText.setText(errorResourceId);
                        }
                    });*/
            setState(CalculatorState.ERROR);
            mResultEditText.setText(errorResourceId);
            /*[BIRD_WEIMI_CALCULATOR] wangyueyue 20150326 end*/
        } else {
            mResultEditText.setText(errorResourceId);
            return;
        }
        /** @} */
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
        mFormulaEditText.setText(result);
        setState(CalculatorState.RESULT);
        /*[BIRD_WEIMI_CALCULATOR] wangyueyue 20150505 begin*/
        //animatorSet.start();
        mResultEditText.setText(result);
        // Reset all of the values modified during the animation.
        mResultEditText.setTextColor(resultTextColor);
        // Finally update the formula to use the current result.
        mFormulaEditText.setText(result);
        setState(CalculatorState.RESULT);
        /*[BIRD_WEIMI_CALCULATOR] wangyueyue 20150505 end*/

    }

    /**
     * M: vibrate when input more than limited @{
     */
    protected final static int MAX_DIGITS_ALERT = 0;
    protected final static int MAX_INPUT_ALERT = 1;
    protected final static int TOAST_INTERVAL = 500;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MAX_DIGITS_ALERT:
                    Toast.makeText(CalculatorSimple.this, R.string.max_digits_alert,
                            Toast.LENGTH_SHORT).show();
                    break;
                case MAX_INPUT_ALERT:
                    Toast.makeText(CalculatorSimple.this, R.string.max_input_alert,
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    throw new IllegalArgumentException("Invalid message.what = "
                            + msg.what);
            }
        }

    };

    public void vibrate() {
        Vibrator vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(new long[]{100, 100}, -1);
        }
    }
    /** @} */
}
