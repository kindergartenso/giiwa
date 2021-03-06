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
package org.giiwa.core.conf;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.giiwa.core.bean.*;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.cache.Cache;
import org.giiwa.framework.web.Model;

/**
 * The Class Global is extended of Config, it can be "overrided" by module or
 * configured, it stored in database
 * 
 * @author yjiang
 */
@Table(name = "gi_config")
public final class Local extends Bean {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;

  @Column(name = X.ID)
  String                    id;

  @Column(name = "s")
  String                    s;

  @Column(name = "i")
  int                       i;

  @Column(name = "l")
  long                      l;

  private static Local      owner            = new Local();

  public static Local getInstance() {
    return owner;
  }

  /**
   * get the int value.
   *
   * @param name
   *          the name
   * @param defaultValue
   *          the default value
   * @return the int
   */
  public static int getInt(String name, int defaultValue) {

    name = Model.node() + "." + name;

    if (!Helper.isConfigured()) {
      return X.toInt(cache.get(name), defaultValue);
    }

    Local c = Cache.get("global/" + name);
    if (c == null || c.expired()) {
      c = Helper.load(W.create(X.ID, name), Local.class);
      if (c != null) {
        /**
         * avoid restarted, can not load new config
         */
        c.setExpired(System.currentTimeMillis() + X.AMINUTE);
        Cache.set("global/" + name, c);
        return X.toInt(c.i, defaultValue);
      } else {
        return Config.getConf().getInt(name, defaultValue);
      }
    }

    return c != null ? X.toInt(c.i, defaultValue) : defaultValue;

  }

  /**
   * get the string value.
   *
   * @param name
   *          the name
   * @param defaultValue
   *          the default value
   * @return the string
   */
  public static String getString(String name, String defaultValue) {
    
    log.debug("loading local." + name);
    
    String name1 = Config.getConf().getString("node.name", X.EMPTY) + "." + name;

    if (!Helper.isConfigured()) {
      Object c1 = cache.get(name1);
      if (c1 != null) {
        return c1.toString();
      }
      Configuration conf = Config.getConf();
      return conf != null ? conf.getString(name1, defaultValue) : defaultValue;
    }

    Local c = Cache.get("global/" + name1);
    if (c == null || c.expired()) {
      c = Helper.load(W.create(X.ID, name1), Local.class);
      if (c != null) {
        /**
         * avoid restarted, can not load new config
         */
        c.setExpired(System.currentTimeMillis() + X.AMINUTE);
        Cache.set("global/" + name1, c);

        return c.s != null ? c.s : defaultValue;
      }
    }

    return c != null && c.s != null ? c.s : Config.getConf().getString(name, defaultValue);

  }

  private static Map<String, Object> cache = new HashMap<String, Object>();

  /**
   * get the long value.
   *
   * @param name
   *          the name
   * @param defaultValue
   *          the default value
   * @return the long
   */
  public static long getLong(String name, long defaultValue) {

    name = Model.node() + "." + name;

    if (!Helper.isConfigured()) {
      return X.toLong(cache.get(name), defaultValue);
    }

    Local c = Cache.get("global/" + name);
    if (c == null || c.expired()) {
      c = Helper.load(W.create(X.ID, name), Local.class);
      if (c != null) {
        /**
         * avoid restarted, can not load new config
         */
        c.setExpired(System.currentTimeMillis() + X.AMINUTE);
        Cache.set("global/" + name, c);

        return X.toLong(c.l, defaultValue);
      } else {
        return Config.getConf().getLong(name, defaultValue);
      }
    }
    return c != null ? X.toLong(c.l, defaultValue) : defaultValue;

  }

  /**
   * Sets the value of the name in database, it will remove the configuration
   * value if value is null.
   *
   * @param name
   *          the name
   * @param o
   *          the value
   */
  public synchronized static void setConfig(String name, Object o) {

    name = Model.node() + "." + name;

    if (X.isEmpty(name)) {
      return;
    }

    Cache.remove("global/" + name);

    if (o == null) {
      Helper.delete(W.create(X.ID, name), Local.class);
      return;
    }

    if (!Helper.isConfigured()) {
      cache.put(name, o);
      return;
    }

    try {
      V v = V.create();
      if (o instanceof Integer) {
        v.set("i", o);
      } else if (o instanceof Long) {
        v.set("l", o);
      } else {
        v.set("s", o.toString());
      }

      if (Helper.exists(W.create(X.ID, name), Local.class)) {
        Helper.update(W.create(X.ID, name), v, Local.class);
      } else {
        Helper.insert(v.set(X.ID, name), Local.class);
      }
    } catch (Exception e1) {
      log.error(e1.getMessage(), e1);
    }
  }

  /**
   * Gets the string value
   *
   * @param name
   *          the name
   * @return the string
   */
  public String get(String name) {
    return getString(name, null);
  }

}
