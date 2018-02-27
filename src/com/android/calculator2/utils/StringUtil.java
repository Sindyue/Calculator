package com.android.calculator2.utils;

import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

    public static String uuid12() {
        String result = null;
        result = java.util.UUID.randomUUID().toString();
        result = result.substring(result.length() - 11);
        char d = (char) (new java.util.Random().nextInt(26) + 97);
        result = d + result;
        return result;
    }

    public static String getFixLenInt(int len, int val) {
        String r = "";
        String tmp = String.valueOf(val);
        for (int i = 0; i < len - tmp.length(); i++) r += "0";
        r = r + tmp;
        return r;
    }

    public static boolean isValid(String in) {
        return in != null && in.trim().length() > 0 && (!in.equals("null"));
    }

    /**
     * 使用指定的分隔符连接数组元素，如: join(new String[]{"s1","s2","s3"}, ",", "") => "s1,s2,s3"
     *
     * @param elements
     * @param delimiter
     * @param defVal    若某元素为空时的替代值
     * @return
     */
    public static String join(Object[] elements, String delimiter, String defVal) {
        return join(java.util.Arrays.asList(elements).iterator(), delimiter, defVal);
    }

    public static String join(Iterator<?> iter, String delimiter, String defVal) {
        if (iter == null)
            return null;
        StringBuffer sb = new StringBuffer();
        while (iter.hasNext()) {
            Object obj = iter.next();
            sb.append(delimiter).append(obj != null ? obj : defVal);
        }
        return sb.substring(1);
    }

    public static String join(Iterator<?> iter, String delimiter) {
        return join(iter, delimiter, "");
    }

    /**
     * 使用指定的分隔符连接数组元素, 若Java版本为1.8以上，应使用Java自带的String.join方法。
     *
     * @param elements
     * @param delimiter
     * @return
     */
    public static String join(Object[] elements, String delimiter) {
        return join(elements, delimiter, "");
    }

    public static JSONObject query2json(String in) throws Exception {
        JSONObject result = new JSONObject();
        if (in == null) return result;
        String[] params = in.split("&");
        for (String p : params) {
            String[] vs = p.split("=");
            result.put(vs[0], java.net.URLDecoder.decode(vs[1], "UTF-8"));
        }
        return result;
    }

    public static JSONObject tmf814name2json(String in) throws JSONException {
        if (in.charAt(0) == '/')
            in = in.replaceFirst("/", "");
        in = in.replaceAll("/", ",");
        in = in.replaceAll("=", ":");
        JSONObject result = new JSONObject("{" + in + "}");
        return result;
    }

    public static String getMarcroniTag(String nsap) {
        String addr = nsap;
        if (addr == null) return addr;
        addr = addr.replaceAll("\\.", "_");
        if (addr.length() == 40) addr = addr.substring(26, 38);
        return addr;
    }

    public static Object toString(byte[] bytes) {
        if (bytes == null) {
            return JSONObject.NULL;
        }

        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                sb.append(",");
            }

            sb.append((int) bytes[i]);
        }

        sb.append("]");

        return sb.toString();
    }

    //  2017/11/8  关于小数点的保留位数：商品的数量在系统中显示时若数值为整数，则不显示小数位数（2 件）；若为小数，则最多显示4位小数；
    /*至多保留四位小数*/
    //注释：1.少于等于4位小数的，直接返回
    //     2.多于4位小数的，直接返回4位小数的
    //     3.整数，直接返回
    public static String DecimalFormat4keep0(String str) {
        if (StringUtil.isEmptyString(str)) {
            return str;
        }
        try {
            if (str.equals(".")) {
                return ".";
            }
            double r = Double.parseDouble(str);
            return DecimalFormat4keep0(r);
///// return new DecimalFormat("0.####").format(r);可以代替以下方法
//            String tempString = new DecimalFormat("0.0000").format(r);
//            String[] temp = tempString.split("\\.");
////            Log.d("cj", "strings " + str + " strings " + temp[0] + " strings " + temp[1]);
////            Log.d("cj", "substring " + temp.substring(4, 5));//5
//            if (temp[1].length() == 4 && !temp[1].substring(3, 4).equals("0")) {
//                return tempString;
//            } else if (temp[1].length() == 3 && !temp[1].substring(2, 3).equals("0")) {
//                return new DecimalFormat("0.000").format(r);
//            } else if (temp[1].length() == 2 && !temp[1].substring(1, 2).equals("0")) {
//                return new DecimalFormat("0.00").format(r);
//            } else if (temp[1].length() == 1 && !temp[1].substring(0, 1).equals("0")) {
//                return new DecimalFormat("0.0").format(r);
//            } else {
//                return new DecimalFormat("0").format(r);
//            }
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
///// return new DecimalFormat("0.####").format(r);可以代替以下方法
//            String tempString = new DecimalFormat("0.0000").format(r);
//            String[] temp = tempString.split("\\.");
////            Log.d("cj", "strings " + str + " strings " + temp[0] + " strings " + temp[1]);
////            Log.d("cj", "substring " + temp.substring(4, 5));//5
//            if (temp[1].length() == 4 && !temp[1].substring(3, 4).equals("0")) {
//                return tempString;
//            } else if (temp[1].length() == 3 && !temp[1].substring(2, 3).equals("0")) {
//                return new DecimalFormat("0.000").format(r);
//            } else if (temp[1].length() == 2 && !temp[1].substring(1, 2).equals("0")) {
//                return new DecimalFormat("0.00").format(r);
//            } else if (temp[1].length() == 1 && !temp[1].substring(0, 1).equals("0")) {
//                return new DecimalFormat("0.0").format(r);
//            } else {
//                return new DecimalFormat("0").format(r);
//            }
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
        if (StringUtil.isEmptyString(str)) {
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
///// return new DecimalFormat("0.00##").format(r);可以代替以下方法
//            String tempString = new DecimalFormat("0.0000").format(r);
//            String[] temp = tempString.split("\\.");
////            Log.d("cj", "substring " + temp.substring(4, 5));//5
//            if (!temp[1].substring(3, 4).equals("0")) {
//                return tempString;
//            } else if (!temp[1].substring(2, 3).equals("0")) {
//                return new DecimalFormat("0.000").format(r);
//            } else {
//                return new DecimalFormat("0.00").format(r);
//            }
        } catch (Exception e) {
            Log.d("cj", "e " + e.getMessage());
            return str;
        }
    }

    /**
     * 保留两位小数
     */
    public static String DecimalFormat2(String str) {
        if (StringUtil.isEmptyString(str)) {
            return str;
        }
        try {

            if (str.equals(".")) {
                return ".";
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
            if ("".equals(source.toString())) {
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
                    CharSequence newText = dest.subSequence(dstart, dend);
                    return newText;
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
            if (!StringUtil.isEmptyString(mString)) {
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

    /*判断字符串是否为空*/
    public static boolean isEmptyString(String str) {
        if (str != null && !"".equals(str.trim())) {
            return false;
        }
        return true;
    }
}