package com.android.calculator2.utils;

import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

    //  2017/11/8  关于小数点的保留位数：商品的数量在系统中显示时若数值为整数，则不显示小数位数（2 件）；若为小数，则最多显示4位小数；
    /*至多保留四位小数*/
    //注释：1.少于等于4位小数的，直接返回
    //     2.多于4位小数的，直接返回4位小数的
    //     3.整数，直接返回
    public static String DecimalFormat4keep0(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        try {
            if (str.equals(".")) {
                return ".";
            }
            double r = Double.parseDouble(str);
            return DecimalFormat4keep0(r);
        } catch (Exception e) {
            Log.d("cj", "e " + e.getMessage());
            return str;
        }
    }

    public static String DecimalFormat4keep0(double value) {

        try {
            if (value == 0) {
                return "0";
            }

            return new DecimalFormat("0.####").format(value);
        } catch (Exception e) {
            Log.d("cj", "e " + e.getMessage());
            return value + "";
        }
    }

    //  2017/11/8  关于小数点的保留位数：商品的单价在系统中显示时若数值为整数则显示两位小数（2.00 元）；若为小数，则最多显示4位小数（2.1253 元）；
    /*至多保留四位小数*/
    //注释：1.少于等于4位小数的，直接返回
    //     2.多于4位小数的，直接返回4位小数的
    //     3.整数，直接返回2位小数的
    public static String DecimalFormat4keep2(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        try {
            if (str.equals(".")) {
                return ".";
            }
            double r = Double.parseDouble(str);
            if (r == 0) {
                return "0";
            }

            return new DecimalFormat("0.00##").format(r);
        } catch (Exception e) {
            Log.d("cj", "e " + e.getMessage());
            return str;
        }
    }

    /**
     * 保留两位小数
     */
    public static String DecimalFormat2(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        try {
            if (str.equals(".")) {
                return "0.00";
            }

            double f = Double.parseDouble(str);
            return DecimalFormat2(f);

        } catch (Exception e) {
            return str;
        }
    }

    /**
     * 保留两位小数，针对 double 类型
     */
    public static String DecimalFormat2(double value) {
        try {
            if (value == 0) {
                return "0.00";
            }

            DecimalFormat df = new DecimalFormat("0.00");
            return df.format(value);
        } catch (Exception e) {
            return value + "";
        }
    }

    /**
     * 设置小数位数控制
     */
    public static InputFilter lengthfilter2 = new InputFilter() {
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            String dValue = dest.toString();
            Pattern p = Pattern.compile("[0-9]*");
            // 删除等特殊字符，直接返回
            if (TextUtils.isEmpty(source.toString())) {
                return null;
            }
            //验证非数字或者小数点的情况
            Matcher m = p.matcher(source);
            if (dValue.contains(".")) {
                //已经存在小数点的情况下，只能输入数字
                if (!m.matches()) {
                    return null;
                }
            } else {
                //未输入小数点的情况下，可以输入小数点和数字
                if (!m.matches() && !source.equals(".")) {
                    return null;
                }
            }
            //验证输入金额的大小
            if (!source.toString().equals("") && !source.toString().equals(".")) {
                double dold = Double.parseDouble(dValue + source.toString());
                if (dold > 99999999) {
                    return dest.subSequence(dstart, dend);
                } else if (dold == 99999999) {
                    if (source.toString().equals(".")) {
                        return dest.subSequence(dstart, dend);
                    }
                }
            }
            //验证小数位精度是否正确
            if (dValue.contains(".")) {
                int index = dValue.indexOf(".");
                int len = dend - index;
                //  2017/11/8 关于小数点的保留位数：
                //手动输入商品的数量和单价时最多输入4位小数；
                //注：农机具数量也做以上的处理

                //小数位只能2位
                if (len > 2) {
                    return dest.subSequence(dstart, dend);
                }
            }
            return dest.subSequence(dstart, dend) + source.toString();
        }
    };

    /**
     * String 转 Double
     */
    public static double stringToDouble(String mString) {
        double mDouble = 0;
        try {
            if (!TextUtils.isEmpty(mString)) {
                if (mString.equals(".") || mString.equals("-")) {
                    mDouble = 0;
                } else {
                    mDouble = Double.parseDouble(mString);
                }
            }
        } catch (Exception e) {
//            Log.i("cj", "Exception " + mString);
            mDouble = 0;
        }
        return mDouble;
    }
}