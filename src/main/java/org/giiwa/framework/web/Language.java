/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.framework.web;

import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.logging.*;
import org.giiwa.core.base.Html;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.core.vengine.VEngine;

/**
 * language data which located at /modules/[module]/i18n/
 * 
 * @author yjiang
 * 
 */
public class Language {

  final static Log                     log     = LogFactory.getLog(Language.class);

  /**
   * locale of language
   */
  private String                       locale;

  /**
   * the language mapping data
   */
  private Map<String, String[]>        data    = new HashMap<String, String[]>();

  /**
   * missed key-value
   */
  private Map<String, String>          missed  = new HashMap<String, String>();

  /**
   * cache the language data withe locale
   */
  private static Map<String, Language> locales = new HashMap<String, Language>();

  public static Language getLanguage() {
    return getLanguage(Module.home == null ? X.EMPTY : Global.getString("language", "zh_cn"));
  }

  public String getLocale() {
    return this.locale;
  }

  /**
   * Prints the.
   * 
   * @param format
   *          the format
   * @param args
   *          the args
   * @return the string
   */
  public String print(String format, Object... args) {
    return String.format(format, args);
  }

  /**
   * Clean.
   */
  public static void clean() {
    locales.clear();
  }

  /**
   * Truncate.
   *
   * @param s
   *          the s
   * @param length
   *          the length
   * @return the object
   */
  public Object truncate(Object s, int length) {
    if (X.isEmpty(s)) {
      return s;
    }

    if (s instanceof String) {
      String s1 = Html.create((String) s).text();
      if (s1.length() > length) {
        return s1.substring(0, length - 3) + "<span class='truncated' title='" + s1 + "'>...</span>";
      }
      return s1;
    }
    return s;
  }

  /**
   * Color.
   * 
   * @param d
   *          the d
   * @param bits
   *          the bits
   * @return the string
   */
  public String color(long d, int bits) {
    String s = Long.toHexString(d);
    StringBuilder sb = new StringBuilder();
    for (int i = s.length() - 1; i >= 0; i--) {
      sb.append(s.charAt(i));
    }
    if (sb.length() < bits) {
      for (int i = sb.length(); i < bits; i++) {
        sb.append("0");
      }
      return "#" + sb.toString();
    } else {
      return "#" + sb.substring(0, bits);
    }
  }

  /**
   * Now.
   * 
   * @param format
   *          the format
   * @return the string
   */
  public String now(String format) {
    return format(System.currentTimeMillis(), format);
  }

  /**
   * Bitmaps.
   * 
   * @param f
   *          the f
   * @return the list
   */
  public List<Integer> bitmaps(int f) {
    List<Integer> list = new ArrayList<Integer>();
    int m = 1;
    for (int i = 0; i < 32; i++) {
      if ((m & f) > 0) {
        list.add(m);
      }
      m <<= 1;
    }
    return list.size() > 0 ? list : null;
  }

  /**
   * Bits.
   * 
   * @param f
   *          the f
   * @param s
   *          the s
   * @param n
   *          the n
   * @return the int
   */
  public int bits(int f, int s, int n) {
    f = f >> s;
    return f - (f >> n) << n;
  }

  /**
   * Gets the language.
   * 
   * @param locale
   *          the locale
   * @return the language
   */
  protected static Language getLanguage(String locale) {

    if (locales.containsKey(locale)) {
      return locales.get(locale);
    }

    Language l = new Language(locale);
    locales.put(locale, l);
    return l;

  }

  private Language(String locale) {
    this.locale = locale;
    if (Module.home != null && !Module.home.supportLocale(locale)) {
      this.locale = Global.getString("language", "en_us");
    }

    load();
  }

  /**
   * Checks for.
   * 
   * @param name
   *          the name
   * @return true, if successful
   */
  public boolean has(String name) {
    if (name == null)
      return false;
    return data.containsKey(name);
  }

  /**
   * get All
   * 
   * @return Map
   */
  public Map<String, String[]> getData() {
    return data;
  }

  /**
   * get missed keys
   * 
   * @return Map
   */
  public Map<String, String> getMissed() {
    return missed;
  }

  /**
   * Parses the string with the model using velocity engine
   *
   * @param str
   *          the string
   * @param m
   *          the model
   * @return the string
   */
  public String parse(String str, Model m) {

    try {
      str = VEngine.parse(str, m.context);
    } catch (Exception e) {
      log.error(str, e);
    }
    return str;
  }

  /**
   * Gets the.
   * 
   * @param name
   *          the name
   * @param args
   *          the args which used to format the string
   * @return the string
   */
  public String get(String name, Object... args) {
    if (X.isEmpty(name)) {
      return X.EMPTY;
    }

    if (data.containsKey(name)) {
      if (args != null && args.length > 0) {
        return String.format(data.get(name)[0], args);
      } else {
        return data.get(name)[0];
      }
    } else if (missed.containsKey(name)) {
      return missed.get(name);
    } else {
      if (name.startsWith("$,")) {
        return name.substring(2);
      }

      if (name.indexOf("$") > -1) {
        return null;
      }

      missed.put(name, name);
      return name;
    }
  }

  /**
   * Load.
   */
  public void load() {
    data = new HashMap<String, String[]>();
    if (Module.home != null) {
      Module.home.loadLang(data, locale);
    }

    if (data.isEmpty()) {
      // log.error("doesnt support the locale: " + locale);
    }
  }

  /**
   * @deprecated
   */
  private SimpleDateFormat                     sdf     = null;

  private static Map<String, SimpleDateFormat> formats = new HashMap<String, SimpleDateFormat>();

  /**
   * Format.
   * 
   * @param t
   *          the t
   * @param format
   *          the format
   * @return the string
   */
  public String format(String t, String format) {
    try {
      SimpleDateFormat sdf = formats.get(format);
      if (sdf == null) {
        sdf = new SimpleDateFormat(format);

        // if (data.containsKey("date.timezone")) {
        // sdf.setTimeZone(TimeZone.getTimeZone(get("date.timezone")));
        // }

        formats.put(format, sdf);
      }

      // parse t to datetime, assume the datetime is
      // "YYYY ? MM ? dd hh:mm:ss"

      return sdf.format(new Date(_parseDatetime(t)));
    } catch (Exception e) {
      log.error(t, e);
    }
    return t;
  }

  private long _parseDatetime(String sdate) {
    if (sdate == null) {
      return 0;
    }

    String[] ss = sdate.split("[-/_:H ]");

    if (ss.length < 3) {
      return 0;
    }
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(0);
    int[] d = new int[ss.length];
    for (int i = 0; i < d.length; i++) {
      d[i] = X.toInt(ss[i]);
    }

    if (d[0] > 100) {
      c.set(Calendar.YEAR, d[0]);
      if (d[1] > 12) {
        c.set(Calendar.MONTH, d[2] - 1);
        c.set(Calendar.DATE, d[1]);
      } else {
        c.set(Calendar.MONTH, d[1] - 1);
        c.set(Calendar.DATE, d[2]);
      }
    } else if (d[2] > 100) {
      c.set(Calendar.YEAR, d[2]);

      if (d[0] > 12) {
        c.set(Calendar.MONTH, d[1] - 1);
        c.set(Calendar.DATE, d[0]);
      } else {
        c.set(Calendar.MONTH, d[0] - 1);
        c.set(Calendar.DATE, d[1]);
      }
    }

    if (d.length > 3) {
      c.set(Calendar.HOUR, d[3]);
    }
    if (d.length > 4) {
      c.set(Calendar.MINUTE, d[4]);
    }
    if (d.length > 5) {
      c.set(Calendar.SECOND, d[5]);
    }

    return c.getTimeInMillis();
  }

  /**
   * Format.
   * 
   * @param t
   *          the t
   * @param format
   *          the format
   * @return the string
   */
  public String format(long t, String format) {
    if (t <= 0)
      return Long.toString(t);

    try {
      SimpleDateFormat sdf = formats.get(format);
      if (sdf == null) {
        sdf = new SimpleDateFormat(format);

        // if (data.containsKey("date.timezone")) {
        // sdf.setTimeZone(TimeZone.getTimeZone(get("date.timezone")));
        // }

        formats.put(format, sdf);
      }
      return sdf.format(new Date(t));
    } catch (Exception e) {
      log.error(t, e);
    }
    return Long.toString(t);
  }

  /**
   * Convert.
   * 
   * @param date
   *          the date
   * @param from
   *          the from
   * @param format
   *          the format
   * @return the string
   */
  public String convert(int date, String from, String format) {
    if (date == 0)
      return X.EMPTY;

    long t = parse(Integer.toString(date), from);
    if (t == 0)
      return X.EMPTY;

    return format(t, format);
  }

  /**
   * Convert.
   * 
   * @param date
   *          the date
   * @param from
   *          the from
   * @param format
   *          the format
   * @return the string
   */
  public String convert(String date, String from, String format) {
    if (date == null || date.length() < 8) {
      return date;
    }

    long t = parse(date, from);
    if (t == 0)
      return X.EMPTY;

    return format(t, format);
  }

  /**
   * Parses the.
   * 
   * @param t
   *          the t
   * @param format
   *          the format
   * @return the long
   */
  public long parse(String t, String format) {
    if (t == null || "".equals(t))
      return 0;

    try {
      SimpleDateFormat sdf = formats.get(format);
      if (sdf == null) {
        sdf = new SimpleDateFormat(format);
        // sdf.setTimeZone(TimeZone.getTimeZone(get("date.timezone")));
        formats.put(format, sdf);
      }
      return sdf.parse(t).getTime();
    } catch (Exception e) {
      log.error(t, e);
    }

    return 0;
  }

  /**
   * Format.
   * 
   * @param t
   *          the t
   * @return the string
   */
  public String format(long t) {
    try {
      if (sdf == null) {
        sdf = new SimpleDateFormat(get("date.format"));
        // sdf.setTimeZone(TimeZone.getTimeZone(get("date.timezone")));
      }
      return sdf.format(new Date(t));
    } catch (Exception e) {
      log.error("t=" + t + ", format:" + get("date.format"), e);
    }
    return Long.toString(t);
  }

  /**
   * Size.
   * 
   * @param length
   *          the length
   * @return the string
   */
  public String size(long length) {
    String unit = X.EMPTY;
    double d = Math.abs(length);
    if (d < 1024) {
    } else if (d < 1024 * 1024) {
      unit = "K";
      d /= 1024f;
    } else if (d < 1024 * 1024 * 1024) {
      unit = "M";
      d /= 1024f * 1024;
    } else {
      unit = "G";
      d /= 1024f * 1024 * 1024;
    }

    if (length > 0) {
      return ((long) (d * 10)) / 10f + unit;
    } else {
      return -((long) (d * 10)) / 10f + unit;
    }
  }

  /**
   * Past.
   * 
   * @param base
   *          the base
   * @return the string
   */
  public String past(long base) {
    if (base <= 0) {
      return X.EMPTY;
    }

    int t = (int) ((System.currentTimeMillis() - base) / 1000);
    if (t < 60) {
      return t + get("past.s");
    }

    t /= 60;
    if (t < 60) {
      return t + get("past.m");
    }

    t /= 60;
    if (t < 24) {
      return t + get("past.h");
    }

    t /= 24;
    if (t < 30) {
      return t + get("past.d");
    }

    int t1 = t / 30;
    if (t < 365) {
      return t1 + get("past.M");
    }

    t1 = t / 365;
    return t1 + get("past.y");
  }

  /**
   * Parses the.
   * 
   * @param body
   *          the body
   * @return the html
   */
  public Html parse(String body) {
    return Html.create(body);
  }
}
