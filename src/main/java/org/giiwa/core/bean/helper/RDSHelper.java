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
package org.giiwa.core.bean.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.json.JSON;

/**
 * The {@code RDSHelper} Class is base class for all class that database access,
 * it almost includes all methods that need for database <br>
 * all data access MUST be inherited from it
 * 
 */
public class RDSHelper implements Helper.DBHelper {

  private static Log                 log     = LogFactory.getLog(RDSHelper.class);

  /**
   * indicated whether is debug model
   */
  public static boolean              DEBUG   = true;
  public static final int            MAXROWS = 10000;

  public static RDSHelper            inst    = new RDSHelper();

  private static Map<String, String> oracle  = new HashMap<String, String>();
  static {
    oracle.put("uid", "\"uid\"");
    oracle.put("access", "\"access\"");
  }

  private static String _where(W q, Connection c) throws SQLException {
    if (q == null || c == null) {
      return null;
    }

    if (isOracle(c)) {
      return q.where(oracle);
    }

    return q.where();
  }

  private static String _orderby(W q, Connection c) throws SQLException {
    if (q == null || c == null) {
      return null;
    }

    if (isOracle(c)) {
      return q.orderby(oracle);
    }

    return q.orderby();
  }

  /**
   * update the data in db.
   * 
   * @param table
   *          the table name
   * @param sets
   *          the sets SQL sentence
   * @param q
   *          the query object
   * @param db
   *          the db name
   * @return int
   */
  public int update(String table, String sets, W q, String db) {

    /**
     * update it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;

    try {
      if (X.isEmpty(db)) {
        c = getConnection();
      } else {
        c = getConnection(db);
      }
      if (c == null)
        return -1;

      /**
       * create the sql statement
       */
      StringBuilder sql = new StringBuilder();
      sql.append("update ").append(table).append(" set ").append(sets);

      String where = _where(q, c);
      Object[] args = q.args();

      if (where != null) {
        sql.append(" where ").append(where);
      }

      p = c.prepareStatement(sql.toString());

      int order = 1;
      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);
        }
      }

      return p.executeUpdate();

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(q, e);
    } finally {
      close(c, p, r);
    }

    return 0;
  }

  /**
   * Sets the parameter.
   * 
   * @param p
   *          the p
   * @param i
   *          the i
   * @param o
   *          the o
   * @throws SQLException
   *           the SQL exception
   */
  private static void setParameter(PreparedStatement p, int i, Object o) throws SQLException {
    if (o == null) {
      p.setObject(i, null);
    } else if (o instanceof Integer) {
      p.setInt(i, (Integer) o);
    } else if (o instanceof Date) {
      p.setDate(i, new java.sql.Date(((Date) o).getTime()));
    } else if (o instanceof java.sql.Date) {
      p.setDate(i, (java.sql.Date) o);
    } else if (o instanceof Long) {
      p.setLong(i, (Long) o);
    } else if (o instanceof Float) {
      p.setFloat(i, (Float) o);
    } else if (o instanceof Double) {
      p.setDouble(i, (Double) o);
    } else if (o instanceof Boolean) {
      p.setBoolean(i, (Boolean) o);
    } else if (o instanceof Timestamp) {
      p.setTimestamp(i, (Timestamp) o);
    } else {
      p.setString(i, o.toString());
    }
  }

  /**
   * Delete the data in table.
   *
   * @param table
   *          the table name
   * @param q
   *          the query object
   * @param db
   *          the db
   * @return int
   */
  public int delete(String table, W q, String db) {

    /**
     * update it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    int n = -1;

    try {
      c = getConnection(db);

      if (c == null)
        return -1;

      /**
       * create the sql statement
       */
      StringBuilder sql = new StringBuilder();
      sql.append("delete from ").append(table);
      String where = _where(q, c);
      Object[] args = q.args();

      if (!X.isEmpty(where)) {
        sql.append(" where ").append(where);
      }

      p = c.prepareStatement(sql.toString());

      if (args != null) {
        int order = 1;

        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);

        }
      }

      n = p.executeUpdate();

      if (log.isDebugEnabled()) {
        log.debug("delete, table=" + table + ", q=" + q + ", deleted=" + n);
      }

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(q, e);
    } finally {
      close(p, c);
    }

    return n;
  }

  /**
   * Gets the connection.
   * 
   * @return Connection
   * @throws SQLException
   *           the SQL exception
   */
  public static Connection getConnection() throws SQLException {
    try {
      // long tid = Thread.currentThread().getId();
      // if (outdoor.size() > 0) {
      // Connection[] cs = null;
      // synchronized (outdoor) {
      // cs = outdoor.keySet().toArray(new Connection[outdoor.size()]);
      // }
      //
      // for (Connection c : cs) {
      // Long[] dd = outdoor.get(c);
      // if (dd != null && dd[0] == tid) {
      // dd[2]++;
      // synchronized (outdoor) {
      // outdoor.put(c, dd);
      // }
      // return c;
      // }
      // }
      // }
      Connection c = RDB.getConnection();
      // synchronized (outdoor) {
      // outdoor.put(c, new Long[] { tid, System.currentTimeMillis(), 0L });
      // }
      return c;
    } catch (SQLException e1) {
      log.error(e1.getMessage(), e1);
      throw e1;
    }
  }

  // private static Map<Connection, Long[]> outdoor = new HashMap<Connection,
  // Long[]>();

  /**
   * Gets the SQL connection by name.
   *
   * @param name
   *          the name of the connection pool
   * @return the connection
   * @throws SQLException
   *           the SQL exception
   */
  public static Connection getConnection(String name) throws SQLException {
    return RDB.getConnection(name);
  }

  /**
   * Close the objects, the object cloud be ResultSet, Statement,
   * PreparedStatement, Connection <br>
   * if the connection was required twice in same thread, then the reference
   * "-1", if "=0", then close it.
   *
   * @param objs
   *          the objects of "ResultSet, Statement, Connection"
   */
  final public static void close(Object... objs) {
    for (Object o : objs) {
      try {
        if (o == null)
          continue;

        if (o instanceof ResultSet) {
          ((ResultSet) o).close();
        } else if (o instanceof Statement) {
          ((Statement) o).close();
        } else if (o instanceof PreparedStatement) {
          ((PreparedStatement) o).close();
        } else if (o instanceof Connection) {
          // local.remove();
          Connection c = (Connection) o;
          // Long[] dd = outdoor.get(c);
          // if (dd == null || dd[2] <= 0) {
          try {
            if (!c.getAutoCommit()) {
              c.commit();
            }
          } catch (Exception e1) {
          } finally {
            c.close();
          }
          // synchronized (outdoor) {
          // outdoor.remove(c);
          // }
          // } else {
          // dd[2]--;
          // synchronized (outdoor) {
          // outdoor.put(c, dd);
          // }
          // }
        }
      } catch (SQLException e) {
        if (log.isErrorEnabled())
          log.error(e.getMessage(), e);
      }
    }
  }

  /**
   * test exists.
   *
   * @param table
   *          the table name
   * @param q
   *          the query object
   * @param db
   *          the db
   * @return boolean
   */
  public boolean exists(String table, W q, String db) {
    /**
     * create the sql statement
     */
    TimeStamp t = TimeStamp.create();

    /**
     * search it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;

    try {
      c = getConnection(db);

      if (c == null)
        return false;

      StringBuilder sql = new StringBuilder();
      sql.append("select 1 from ").append(table);

      String where = _where(q, c);
      Object[] args = q.args();

      if (!X.isEmpty(where)) {
        sql.append(" where ").append(where);
      }

      if (isOracle(c)) {
        if (X.isEmpty(where)) {
          sql.append(" where ");
        } else {
          sql.append(" and ");
        }
        sql.append(" rownum=1");
      } else {
        sql.append(" limit 1");
      }

      p = c.prepareStatement(sql.toString());

      int order = 1;
      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];
          try {
            setParameter(p, order++, o);
          } catch (Exception e) {
            log.error("i=" + i + ", o=" + o, e);
          }
        }
      }

      r = p.executeQuery();
      return r.next();

    } catch (SQLException e) {
      if (log.isErrorEnabled())
        log.error(q, e);

    } finally {
      close(r, p, c);

      if (log.isDebugEnabled())
        log.debug("cost:" + t.past() + "ms, sql=[" + q);
    }

    return false;
  }

  /**
   * Update the data.
   *
   * @param table
   *          the table name
   * @param q
   *          the query object
   * @param sets
   *          the values
   * @param db
   *          the db
   * @return int
   */
  public int updateTable(String table, W q, V sets, String db) {

    /**
     * update it in database
     */
    Connection c = null;
    PreparedStatement p = null;

    int updated = 0;
    try {

      c = getConnection(db);

      if (c == null)
        return -1;

      /**
       * create the sql statement
       */
      StringBuilder sql = new StringBuilder();
      sql.append("update ").append(table).append(" set ");

      boolean isoracle = isOracle(c);

      StringBuilder s = new StringBuilder();
      for (String name : sets.names()) {
        if (s.length() > 0)
          s.append(",");
        if (isoracle && oracle.containsKey(name)) {
          s.append(oracle.get(name));
        } else {
          s.append(name);
        }
        s.append("=?");
      }
      sql.append(s);

      String where = _where(q, c);
      Object[] args = q.args();

      if (where != null) {
        sql.append(" where ").append(where);
      }

      p = c.prepareStatement(sql.toString());

      int order = 1;
      for (String name : sets.names()) {
        Object v = sets.value(name);
        try {
          setParameter(p, order++, v);
        } catch (Exception e) {
          log.error(name + "=" + v, e);
        }
      }

      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);
        }
      }

      updated = p.executeUpdate();

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(q + ",values=" + sets.toString(), e);
    } finally {
      close(p, c);
    }

    return updated;

  }

  /**
   * load the Bean.
   *
   * @param <T>
   *          the generic type
   * @param q
   *          the query object
   * @param t
   *          the Class of Bean
   * @return the Bean
   */
  // public static <T extends Bean> T load(W q, Class<T> t) {
  // /**
  // * get the require annotation onGet
  // */
  // Table mapping = (Table) t.getAnnotation(Table.class);
  // if (mapping == null) {
  // if (log.isErrorEnabled())
  // log.error("mapping missed in [" + t + "] declaretion");
  // return null;
  // }
  //
  // return load(mapping.name(), q, t);
  //
  // }

  /**
   * Load the data, the data will be load(ResultSet r) method.
   *
   * @param cols
   *          the cols, * if null
   * @param table
   *          the table name
   * @param q
   *          the query object
   * @param b
   *          the Bean
   * @param db
   *          the db
   * @return boolean
   */
  public boolean load(String cols, String table, W q, Bean b, String db) {
    /**
     * create the sql statement
     */
    TimeStamp t = TimeStamp.create();

    /**
     * search it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;
    StringBuilder sql = new StringBuilder();

    try {
      c = getConnection(db);

      if (c == null)
        return false;

      sql.append("select * from ").append(table);

      String where = _where(q, c);
      Object[] args = q.args();
      String orderby = _orderby(q, c);

      if (!X.isEmpty(where)) {
        sql.append(" where ").append(where);
      }

      if (isOracle(c)) {
        if (X.isEmpty(where)) {
          sql.append(" where ");
        } else {
          sql.append(" and ");
        }
        sql.append(" rownum=1");

        if (!X.isEmpty(orderby)) {
          sql.append(" ").append(orderby);
        }

      } else {
        if (!X.isEmpty(orderby)) {
          sql.append(" ").append(orderby);
        }
        sql.append(" limit 1");
      }

      p = c.prepareStatement(sql.toString());

      int order = 1;
      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);
        }
      }

      r = p.executeQuery();
      if (r.next()) {
        b.load(r);

        return true;
      }

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(sql, e);
    } finally {
      close(r, p, c);

      if (log.isDebugEnabled())
        log.debug("cost: " + t.past() + "ms, sql=" + q + ", result=" + b);
    }

    return false;
  }

  /**
   * Load the data from the RDBMS table, by the where and.
   *
   * @param <T>
   *          the generic type
   * @param table
   *          the table
   * @param cols
   *          the cols
   * @param q
   *          the query object
   * @param clazz
   *          the Class Bean
   * @return the list
   */
  public final static <T extends Bean> List<T> load(String table, String[] cols, W q, Class<T> clazz) {
    return load(table, cols, q, -1, -1, clazz);
  }

  /**
   * load the data from RDBMS table which associated with Bean.
   * 
   * @param <T>
   *          the generic Bean Class
   * @param cols
   *          the column name array
   * @param q
   *          the query object
   * @param clazz
   *          the Bean Class
   * @return List
   */
  public final static <T extends Bean> List<T> load(String[] cols, W q, Class<T> clazz) {
    return load(cols, q, -1, -1, clazz);
  }

  /**
   * load the list of beans, by the where.
   *
   * @param <T>
   *          the generic Bean Class
   * @param cols
   *          the column name array
   * @param q
   *          the query object
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @param t
   *          the Bean Class
   * @return List
   */
  public final static <T extends Bean> List<T> load(String[] cols, W q, int offset, int limit, Class<T> t) {
    /**
     * get the require annotation onGet
     */
    Table mapping = (Table) t.getAnnotation(Table.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return null;
    }

    return load(mapping.name(), cols, q, offset, limit, t);
  }

  /**
   * Load the list data from the RDBMS table.
   *
   * @param <T>
   *          the generic type
   * @param table
   *          the table name
   * @param cols
   *          the column name array
   * @param q
   *          the query object
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @param clazz
   *          the Bean Class
   * @return List
   */
  public static <T extends Bean> List<T> load(String table, String[] cols, W q, int offset, int limit, Class<T> clazz) {
    /**
     * create the sql statement
     */
    TimeStamp t = TimeStamp.create();

    // log.debug("sql:" + sql.toString());

    /**
     * search it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;

    try {
      c = getConnection();

      if (c == null)
        return null;

      StringBuilder sql = new StringBuilder();
      sql.append("select ");
      if (cols != null) {
        for (int i = 0; i < cols.length - 1; i++) {
          sql.append(cols[i]).append(", ");
        }

        sql.append(cols[cols.length - 1]);

      } else {
        sql.append("*");
      }

      String where = _where(q, c);
      Object[] args = q.args();
      String orderby = _orderby(q, c);

      sql.append(" from ").append(table);
      if (!X.isEmpty(where)) {
        sql.append(" where ").append(where);
      }

      if (isOracle(c)) {
        if (limit > 0) {
          if (X.isEmpty(where)) {
            sql.append(" where ");
          } else {
            sql.append(" and ");
          }
          if (offset < 0) {
            offset = MAXROWS;
          }
          sql.append(" rownum>").append(offset).append(" and rownum<=").append(offset + limit);
        }
        if (!X.isEmpty(orderby)) {
          sql.append(" ").append(orderby);
        }
      } else {
        if (!X.isEmpty(orderby)) {
          sql.append(" ").append(orderby);
        }
        if (limit > 0) {
          sql.append(" limit ").append(limit);
        }
        if (offset > 0) {
          sql.append(" offset ").append(offset);
        }
      }

      p = c.prepareStatement(sql.toString());

      int order = 1;
      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);
        }
      }

      r = p.executeQuery();
      List<T> list = new ArrayList<T>();
      while (r.next()) {
        T b = clazz.newInstance();
        b.load(r);
        list.add(b);
      }

      return list;
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(q, e);
    } finally {
      close(r, p, c);

      if (log.isDebugEnabled())
        log.debug("cost:" + t.past() + "ms, sql=" + q);
    }
    return null;
  }

  /**
   * advance load API.
   *
   * @param <T>
   *          the base object
   * @param select
   *          the select section, etc:
   *          "select a.* from tbluser a, tblrole b where a.uid=b.uid"
   * @param q
   *          the additional query condition;
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @param clazz
   *          the Class Bean
   * @return List the list of Bean
   */
  public static <T extends Bean> List<T> loadBy(String select, W q, int offset, int limit, Class<T> clazz) {
    /**
     * create the sql statement
     */
    TimeStamp t = TimeStamp.create();

    /**
     * search it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;

    try {
      c = getConnection();

      if (c == null)
        return null;

      StringBuilder sql = new StringBuilder();
      sql.append(select);
      String where = _where(q, c);
      Object[] args = q.args();
      String orderby = _orderby(q, c);

      if (!X.isEmpty(where)) {
        if (select.indexOf(" where ") < 0) {
          sql.append(" where ").append(where);
        } else {
          sql.append(" and (").append(where).append(")");
        }
      }

      if (isOracle(c)) {
        if (X.isEmpty(where)) {
          sql.append(" where ");
        } else {
          sql.append(" and ");
        }
        if (offset < 0) {
          offset = MAXROWS;
        }
        sql.append(" rownum>").append(offset).append(" and rownum<=").append(offset + limit);
        if (!X.isEmpty(orderby)) {
          sql.append(" ").append(orderby);
        }
      } else {
        if (!X.isEmpty(orderby)) {
          sql.append(" ").append(orderby);
        }
        if (limit > 0) {
          sql.append(" limit ").append(limit);
        }
        if (offset > 0) {
          sql.append(" offset ").append(offset);
        }
      }

      p = c.prepareStatement(sql.toString());

      int order = 1;
      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);
        }
      }

      r = p.executeQuery();
      List<T> list = new ArrayList<T>();
      while (r.next()) {
        T b = clazz.newInstance();
        b.load(r);
        list.add(b);
      }

      return list;
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(q.toString(), e);
    } finally {
      close(r, p, c);

      if (log.isDebugEnabled())
        log.debug("cost:" + t.past() + "ms, sql=" + q);
    }
    return null;
  }

  /**
   * Load the list data from the RDBMS table.
   *
   * @param <T>
   *          the generic Bean Class
   * @param table
   *          the table name
   * @param q
   *          the query object
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @param clazz
   *          the Bean Class
   * @param db
   *          the db
   * @return Beans
   */
  public <T extends Bean> Beans<T> load(String table, W q, int offset, int limit, Class<T> clazz, String db) {
    return load(null, table, q, offset, limit, clazz, db);
  }

  /**
   * load the data of cols from table, with the query, offset, limit, class and
   * dbname
   * 
   * @param <T>
   *          the Class
   * @param cols
   *          the cols, * if null
   * @param table
   *          the table name
   * @param q
   *          the query
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @param clazz
   *          the Class of Bean
   * @param db
   *          th dbname
   * @return the Beans
   */
  public <T extends Bean> Beans<T> load(String cols, String table, W q, int offset, int limit, Class<T> clazz,
      String db) {
    /**
     * create the sql statement
     */
    TimeStamp t = TimeStamp.create();

    // log.debug("sql:" + sql.toString());
    /**
     * search it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;
    StringBuilder sql = new StringBuilder();

    try {

      c = getConnection(db);

      if (c == null)
        return null;

      sql.append("select ");
      if (X.isEmpty(cols)) {
        sql.append("*");
      } else {
        sql.append(cols);
      }
      sql.append(" from ").append(table);

      String where = _where(q, c);
      Object[] args = q.args();
      String orderby = _orderby(q, c);

      if (!X.isEmpty(where)) {
        sql.append(" where ").append(where);
      }

      if (isOracle(c)) {
        if (X.isEmpty(where)) {
          sql.append(" where ");
        } else {
          sql.append(" and ");
        }
        if (offset < 0) {
          offset = MAXROWS;
        }
        sql.append(" rownum>").append(offset).append(" and rownum<=").append(offset + limit);

        if (!X.isEmpty(orderby)) {
          sql.append(" ").append(orderby);
        }
      } else {
        if (!X.isEmpty(orderby)) {
          sql.append(" ").append(orderby);
        }

        if (limit > 0) {
          sql.append(" limit ").append(limit);
        }
        if (offset > 0) {
          sql.append(" offset ").append(offset);
        }
      }

      Beans<T> rs = new Beans<T>();

      p = c.prepareStatement(sql.toString());

      int order = 1;
      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);
        }
      }

      r = p.executeQuery();
      rs.list = new ArrayList<T>();
      while (r.next()) {
        T b = clazz.newInstance();
        b.load(r);
        rs.list.add(b);
      }

      if (log.isDebugEnabled())
        log.debug(
            "load - cost=" + t.past() + "ms, collection=" + table + ", sql=" + sql + ", q=" + q + ", result=" + rs);

      rs.setCost(t);
      return rs;
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(sql, e);

    } finally {
      close(r, p, c);
    }
    return null;
  }

  /**
   * get the data.
   * 
   * @param <T>
   *          the generic Bean Class
   * @param table
   *          the table name
   * @param q
   *          the query object
   * @param offset
   *          the offset
   * @param limit
   *          the limit
   * @param clazz
   *          the Bean Class
   * @param c
   *          the connection
   * @return Beans
   */
  public static <T extends Bean> Beans<T> load(String table, W q, int offset, int limit, Class<T> clazz, Connection c) {
    /**
     * create the sql statement
     */
    TimeStamp t = TimeStamp.create();

    // log.debug("sql:" + sql.toString());

    /**
     * search it in database
     */
    PreparedStatement p = null;
    ResultSet r = null;

    try {
      if (c == null)
        return null;

      StringBuilder sql = new StringBuilder();
      sql.append("select * from ").append(table);
      String where = _where(q, c);
      Object[] args = q.args();
      String orderby = _orderby(q, c);

      if (!X.isEmpty(where)) {
        sql.append(" where ").append(where);
      }

      if (isOracle(c)) {
        if (X.isEmpty(where)) {
          sql.append(" where ");
        } else {
          sql.append(" and ");
        }
        if (offset < 0) {
          offset = MAXROWS;
        }
        sql.append(" rownum>").append(offset).append(" and rownum<=").append(offset + limit);
        if (!X.isEmpty(orderby)) {
          sql.append(" ").append(orderby);
        }
      } else {
        if (!X.isEmpty(orderby)) {
          sql.append(" ").append(orderby);
        }
        if (limit > 0) {
          sql.append(" limit ").append(limit);
        }
        if (offset > 0) {
          sql.append(" offset ").append(offset);
        }
      }

      Beans<T> rs = new Beans<T>();

      p = c.prepareStatement(sql.toString());

      int order = 1;
      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);
        }
      }

      r = p.executeQuery();
      rs.list = new ArrayList<T>();
      while (r.next()) {
        T b = clazz.newInstance();
        b.load(r);
        rs.list.add(b);
      }

      if (log.isDebugEnabled())
        log.debug("load - cost=" + t.past() + "ms, collection=" + table + ", sql=" + sql + ", result=" + rs);

      if (t.past() > 10000) {
        log.warn("load - cost=" + t.past() + "ms, collection=" + table + ", sql=" + sql + ", result=" + rs);
      }

      return rs;
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(q, e);

    } finally {
      close(r, p);
    }
    return null;
  }

  /**
   * Insert values into the table.
   *
   * @param table
   *          the table name
   * @param sets
   *          the values
   * @param db
   *          the db
   * @return int
   */
  public int insertTable(String table, V sets, String db) {

    /**
     * insert it in database
     */
    Connection c = null;

    try {
      c = getConnection(db);

      if (c == null)
        return -1;

      return insertTable(table, sets, c);

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(sets.toString(), e);
    } finally {
      close(c);
    }
    return 0;

  }

  private static int insertTable(String table, V sets, Connection c) {

    /**
     * insert it in database
     */
    PreparedStatement p = null;

    try {

      if (c == null)
        return -1;

      /**
       * create the sql statement
       */
      StringBuilder sql = new StringBuilder();
      sql.append("insert into ").append(table).append(" (");
      StringBuilder s = new StringBuilder();
      int total = 0;
      boolean isoracle = isOracle(c);
      for (String name : sets.names()) {
        if (s.length() > 0)
          s.append(",");
        if (isoracle && oracle.containsKey(name)) {
          s.append(oracle.get(name));
        } else {
          s.append(name);
        }
        total++;
      }
      sql.append(s).append(") values( ");

      for (int i = 0; i < total - 1; i++) {
        sql.append("?, ");
      }
      sql.append("?)");

      p = c.prepareStatement(sql.toString());

      int order = 1;
      for (String name : sets.names()) {
        Object v = sets.value(name);
        setParameter(p, order++, v);
      }

      return p.executeUpdate();

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(sets.toString(), e);
    } finally {
      close(p);
    }
    return 0;
  }

  /**
   * Load list result, string, integer, or base data type.
   *
   * @param <T>
   *          the generic Bean Class
   * @param table
   *          the table name
   * @param col
   *          the column name
   * @param q
   *          the query object
   * @param clazz
   *          the Bean Class
   * @param db
   *          the db name
   * @return List
   * @deprecated
   */
  @SuppressWarnings("unchecked")
  public static <T> List<T> loadList(String table, String col, W q, Class<T> clazz, String db) {
    /**
     * create the sql statement
     */
    TimeStamp t = TimeStamp.create();

    /**
     * search it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;

    try {
      if (X.isEmpty(db)) {
        c = getConnection();
      } else {
        c = getConnection(db);
      }
      if (c == null)
        return null;

      StringBuilder sql = new StringBuilder();
      sql.append("select ").append(col).append(" from ").append(table);

      String where = _where(q, c);
      Object[] args = q.args();

      if (where != null) {
        sql.append(" where ").append(where);
      }

      p = c.prepareStatement(sql.toString());

      int order = 1;
      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);
        }
      }

      List<T> list = new ArrayList<T>();
      r = p.executeQuery();
      while (r.next()) {
        T b = (T) (r.getObject(1));
        list.add(b);
      }
      return list;
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(q, e);
    } finally {
      close(r, p, c);

      if (log.isDebugEnabled())
        log.debug("cost:" + t.past() + "ms, sql=" + q);
    }
    return null;
  }

  /**
   * get a string value from a col from the table.
   * 
   * @param table
   *          the table name
   * @param col
   *          the column name
   * @param q
   *          the query object
   * @param db
   *          the db name
   * @return String
   */
  public static String getString(String table, String col, W q, String db) {

    /**
     * search it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;

    try {
      if (X.isEmpty(db)) {
        c = getConnection();
      } else {
        c = getConnection(db);
      }
      if (c == null)
        return null;

      /**
       * create the sql statement
       */
      StringBuilder sql = new StringBuilder();
      // TODO, the col need to be transfer ? in oracle
      sql.append("select ").append(col).append(" from ").append(table);
      String where = _where(q, c);
      Object[] args = q.args();

      if (!X.isEmpty(where)) {
        sql.append(" where ").append(where);
      }

      if (isOracle(c)) {
        if (X.isEmpty(where)) {
          sql.append(" where ");
        } else {
          sql.append(" and ");
        }
        sql.append(" rownum=1");
      } else {
        sql.append(" limit 1");
      }

      p = c.prepareStatement(sql.toString());

      int order = 1;
      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);
        }
      }

      r = p.executeQuery();
      if (r.next()) {
        return r.getString(col);
      }

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(q, e);

    } finally {
      close(r, p, c);
    }

    return null;
  }

  /**
   * get the list of the column.
   *
   * @param <T>
   *          the generic Bean Class
   * @param col
   *          the column name
   * @param q
   *          the query object
   * @param s
   *          the offset
   * @param n
   *          the limit
   * @param t
   *          the Bean class
   * @return List
   * @deprecated
   */
  public final static <T> List<T> getList(String col, W q, int s, int n, Class<? extends Bean> t) {
    /**
     * get the require annotation onGet
     */
    Table mapping = (Table) t.getAnnotation(Table.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return null;
    }

    return getList(mapping.name(), col, q, s, n);
  }

  /**
   * get one field.
   * 
   * @param <T>
   *          the generic Bean Class
   * @param col
   *          the column name
   * @param q
   *          the query object
   * @param position
   *          the offset
   * @param t
   *          the Bean class
   * @return T
   */
  public final static <T> T getOne(String col, W q, int position, Class<? extends Bean> t) {
    /**
     * get the require annotation onGet
     */
    Table mapping = (Table) t.getAnnotation(Table.class);
    if (mapping == null) {
      if (log.isErrorEnabled())
        log.error("mapping missed in [" + t + "] declaretion");
      return null;
    }

    return getOne(mapping.name(), col, q, position);
  }

  /**
   * getOne by the query.
   * 
   * @param <T>
   *          the generic Bean Class
   * @param table
   *          the table name
   * @param col
   *          the column anme
   * @param q
   *          the query object
   * @param position
   *          the offset
   * @return T
   */
  @SuppressWarnings("unchecked")
  public static <T> T getOne(String table, String col, W q, int position) {

    /**
     * search it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;

    try {
      c = getConnection();

      if (c == null)
        return null;

      /**
       * create the sql statement
       */
      StringBuilder sql = new StringBuilder();
      sql.append("select ").append(col).append(" from ").append(table);
      String where = _where(q, c);
      Object[] args = q.args();
      String orderby = _orderby(q, c);

      if (!X.isEmpty(where)) {
        sql.append(" where ").append(where);
      }

      if (isOracle(c)) {
        if (X.isEmpty(where)) {
          sql.append(" where ");
        } else {
          sql.append(" and ");
        }
        sql.append(" rownum=").append(position);
        if (!X.isEmpty(orderby)) {
          sql.append(" ").append(orderby);
        }

      } else {
        if (!X.isEmpty(orderby)) {
          sql.append(" ").append(orderby);
        }

        sql.append(" limit 1");
        if (position > 0) {
          sql.append(" offset ").append(position);
        }
      }

      p = c.prepareStatement(sql.toString());

      int order = 1;
      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);
        }
      }

      r = p.executeQuery();
      if (r.next()) {
        return (T) r.getObject(1);
      }

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(q, e);

    } finally {
      close(r, p, c);
    }

    return null;
  }

  /**
   * get the list of the col.
   *
   * @param <T>
   *          the generic Bean Class
   * @param table
   *          the table name
   * @param col
   *          the column name
   * @param q
   *          the query object
   * @param s
   *          the offset
   * @param n
   *          the limit
   * @return List
   * @deprecated
   */
  @SuppressWarnings("unchecked")
  public final static <T> List<T> getList(String table, String col, W q, int s, int n) {

    /**
     * search it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;

    try {
      c = getConnection();

      if (c == null)
        return null;

      /**
       * create the sql statement
       */
      StringBuilder sql = new StringBuilder();
      sql.append("select ").append(col).append(" from ").append(table);
      String where = _where(q, c);
      Object[] args = q.args();
      String orderby = _orderby(q, c);

      if (!X.isEmpty(where)) {
        sql.append(" where ").append(where);
      }

      if (isOracle(c)) {
        if (X.isEmpty(where)) {
          sql.append(" where ");
        } else {
          sql.append(" and ");
        }
        if (s < 0) {
          s = MAXROWS;
        }
        sql.append(" rownum>").append(s).append(" and rownum<=").append(s + n);
        if (!X.isEmpty(orderby)) {
          sql.append(" ").append(orderby);
        }
      } else {
        if (!X.isEmpty(orderby)) {
          sql.append(" ").append(orderby);
        }
        sql.append(" limit ").append(n);
        if (s > 0) {
          sql.append(" offset ").append(s);
        }
      }

      p = c.prepareStatement(sql.toString());

      int order = 1;
      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);
        }
      }

      r = p.executeQuery();
      List<T> list = new ArrayList<T>();
      while (r.next()) {
        list.add((T) r.getObject(1));
      }
      return list;

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(q, e);

    } finally {
      close(r, p, c);
    }

    return null;
  }

  /**
   * convert the array objects to string.
   * 
   * @param arr
   *          the array objects
   * @return the string
   */
  public static String toString(Object[] arr) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    if (arr != null) {
      int len = arr.length;
      for (int i = 0; i < len; i++) {
        if (i > 0) {
          sb.append(",");
        }

        Object o = arr[i];
        if (o == null) {
          sb.append("null");
        } else if (o instanceof Integer) {
          sb.append(o);
        } else if (o instanceof Date) {
          sb.append("Date(").append(o).append(")");
        } else if (o instanceof Long) {
          sb.append(o);
        } else if (o instanceof Float) {
          sb.append(o);
        } else if (o instanceof Double) {
          sb.append(o);
        } else if (o instanceof Boolean) {
          sb.append("Bool(").append(o).append(")");
        } else {
          sb.append("\"").append(o).append("\"");
        }
      }
    }

    return sb.append("]").toString();
  }

  /**
   * load record in database.
   *
   * @param <T>
   *          the generic Bean Class
   * @param table
   *          the table name
   * @param q
   *          the query object
   * @param clazz
   *          the Bean class
   * @param db
   *          the db
   * @return Bean
   */
  public <T extends Bean> T load(String table, W q, Class<T> clazz, String db) {
    return load(null, table, q, clazz, db);
  }

  /**
   * load the data of cols from table
   * 
   * @param <T>
   *          the object
   * @param cols
   *          the cols, * if null
   * @param table
   *          the table name
   * @param q
   *          the query
   * @param clazz
   *          the Class of Bean
   * @param db
   *          the dbname
   * @return the Beans
   */
  public <T extends Bean> T load(String cols, String table, W q, Class<T> clazz, String db) {
    try {
      T b = (T) clazz.newInstance();

      if (load(cols, table, q, b, db)) {
        return b;
      }
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(e.getMessage(), e);
    }
    return null;
  }

  /**
   * test if confiured RDS
   * 
   * @return true: configured, false: no
   */
  public boolean isConfigured() {
    return RDB.isConfigured();
  }

  /**
   * count the data.
   *
   * @param table
   *          the table name
   * @param q
   *          the query object
   * @param db
   *          the db
   * @return the number of data
   */
  public long count(String table, W q, String db) {

    /**
     * create the sql statement
     */
    TimeStamp t = TimeStamp.create();

    // log.debug("sql:" + sql.toString());

    /**
     * search it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;
    long n = 0;
    try {

      c = getConnection(db);

      if (c == null) {
        n = 0;
      } else {
        StringBuilder sum = new StringBuilder();
        sum.append("select count(*) t from ").append(table);
        String where = _where(q, c);
        Object[] args = q.args();

        if (!X.isEmpty(where)) {
          sum.append(" where ").append(where);
        }

        p = c.prepareStatement(sum.toString());

        int order = 1;
        if (args != null) {
          for (int i = 0; i < args.length; i++) {
            Object o = args[i];

            setParameter(p, order++, o);
          }
        }

        r = p.executeQuery();
        if (r.next()) {
          n = r.getInt("t");
        }
      }
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(q, e);

    } finally {
      close(r, p, c);

      if (log.isDebugEnabled())
        log.debug("cost:" + t.past() + "ms, sql=" + q + ", n=" + n);
    }

    return n;
  }

  /**
   * get distinct data list.
   *
   * @param table
   *          the table name
   * @param name
   *          the column name
   * @param q
   *          the query object
   * @param db
   *          the db
   * @return the list of Object
   */
  @SuppressWarnings("unchecked")
  public <T> List<T> distinct(String table, String name, W q, Class<T> clazz, String db) {
    /**
     * create the sql statement
     */
    TimeStamp t = TimeStamp.create();

    // log.debug("sql:" + sql.toString());

    /**
     * search it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;

    try {

      c = getConnection(db);

      if (c == null)
        return null;

      StringBuilder sql = new StringBuilder();
      // TODO, the name need to be transfer? in oracle
      sql.append("select distinct(").append(name).append(") from ").append(table);
      String where = _where(q, c);
      Object[] args = q.args();

      if (where != null) {
        sql.append(" where ").append(where);
      }

      p = c.prepareStatement(sql.toString());

      int order = 1;
      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);
        }
      }

      r = p.executeQuery();
      List<T> list = new ArrayList<T>();
      while (r.next()) {
        list.add((T) r.getObject(1));
      }

      if (log.isDebugEnabled())
        log.debug("load - cost=" + t.past() + "ms, collection=" + table + ", sql=" + sql + ", result=" + list);

      if (t.past() > 10000) {
        log.warn("load - cost=" + t.past() + "ms, collection=" + table + ", sql=" + sql + ", result=" + list);
      }

      return list;
    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(q, e);

    } finally {
      close(r, p, c);
    }
    return null;
  }

  /**
   * backup the data to file.
   *
   * @param filename
   *          the filename
   */
  public static void backup(String filename) {

    File f = new File(filename);
    f.getParentFile().mkdirs();
    Connection c = null;
    ResultSet r1 = null;

    try {
      ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(f));
      zip.putNextEntry(new ZipEntry("db"));
      PrintStream out = new PrintStream(zip);

      c = getConnection();
      DatabaseMetaData m1 = c.getMetaData();
      r1 = m1.getTables(null, null, null, new String[] { "TABLE" });
      while (r1.next()) {
        _backup(out, c, r1.getString("TABLE_NAME"));
      }
      zip.closeEntry();
      zip.close();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      close(r1, c);
    }

  }

  private static void _backup(PrintStream out, Connection c, String tablename) {

    log.debug("backuping " + tablename);

    Statement stat = null;
    ResultSet r = null;
    try {
      stat = c.createStatement();
      r = stat.executeQuery("select * from " + tablename);

      int rows = 0;
      while (r.next()) {
        rows++;
        ResultSetMetaData m1 = r.getMetaData();

        JSON jo = new JSON();
        jo.put("_table", tablename);
        for (int i = 1; i <= m1.getColumnCount(); i++) {
          Object o = r.getObject(i);
          if (o != null) {
            jo.put(m1.getColumnName(i), o);
          }
        }

        out.println(jo.toString());
      }

      log.debug("backup " + tablename + ", rows=" + rows);

    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      close(r, stat);
    }
  }

  /**
   * recover the data from the file.
   *
   * @param file
   *          the file
   */
  public static void recover(File file) {

    Connection c = null;
    ResultSet r1 = null;
    Statement stat = null;

    try {
      ZipInputStream zip = new ZipInputStream(new FileInputStream(file));
      zip.getNextEntry();
      BufferedReader in = new BufferedReader(new InputStreamReader(zip));

      c = getConnection();
      DatabaseMetaData m1 = c.getMetaData();
      r1 = m1.getTables(null, null, null, new String[] { "TABLE" });
      while (r1.next()) {
        try {
          stat = c.createStatement();
          stat.execute("delete from " + r1.getString("TABLE_NAME"));
          stat.close();
          stat = null;
        } catch (Exception e) {
          log.error("ignore this exception", e);
        }
      }

      String line = in.readLine();
      while (line != null) {
        _recover(line, c);
        line = in.readLine();
      }
      zip.closeEntry();
      in.close();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      close(r1, stat, c);
    }
  }

  private static void _recover(String json, Connection c) {
    try {
      JSON jo = JSON.fromObject(json);
      V v = V.create().copy(jo);
      String tablename = jo.getString("_table");
      v.remove("_table");
      insertTable(tablename, v, c);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  private static boolean isOracle(Connection c) throws SQLException {
    String s = c.getMetaData().getDatabaseProductName();
    String[] ss = X.split(s, "[ /]");
    return X.isSame(ss[0], "oracle");
  }

  /**
   * inc.
   *
   * @param table
   *          the table
   * @param q
   *          the q
   * @param name
   *          the name
   * @param n
   *          the n
   * @param db
   *          the db
   * @return the int
   */
  public int inc(String table, W q, String name, int n, String db) {

    /**
     * update it in database
     */
    Connection c = null;
    PreparedStatement p = null;
    ResultSet r = null;

    try {
      if (X.isEmpty(db)) {
        c = getConnection();
      } else {
        c = getConnection(db);
      }
      if (c == null)
        return -1;

      /**
       * create the sql statement
       */
      StringBuilder sql = new StringBuilder();
      sql.append("update ").append(table).append(" set ").append(name).append("=").append(name).append("+?");

      String where = _where(q, c);
      Object[] args = q.args();

      if (where != null) {
        sql.append(" where ").append(where);
      }

      p = c.prepareStatement(sql.toString());

      int order = 1;
      setParameter(p, order++, n);

      if (args != null) {
        for (int i = 0; i < args.length; i++) {
          Object o = args[i];

          setParameter(p, order++, o);
        }
      }

      return p.executeUpdate();

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(q, e);
    } finally {
      close(c, p, r);
    }

    return 0;
  }

  public BasicDataSource getDB(String db) {
    return RDB.getDataSource(db);
  }

  public void createIndex(String table, LinkedHashMap<String, Integer> ss, String db) {
    Connection c = null;
    Statement stat = null;
    StringBuilder sb = new StringBuilder();
    try {
      c = RDSHelper.getConnection(db);
      stat = c.createStatement();
      sb.append("create index ").append(table).append("_index");
      for (String s : ss.keySet()) {
        sb.append("_").append(s);
      }
      sb.append(" on ").append(table).append("(");

      StringBuilder sb1 = new StringBuilder();
      for (String s : ss.keySet()) {
        if (sb1.length() > 0)
          sb1.append(",");
        sb1.append(s);
        if (X.toInt(ss.get(s)) < 1) {
          sb1.append(" ").append("desc");
        }
      }
      sb.append(sb1.toString()).append(")");
      stat.executeUpdate(sb.toString());

    } catch (Exception e) {

      // log.error(sb.toString(), e);
      log.warn("indexes=" + getIndexes(table, db));

    } finally {
      RDSHelper.close(stat, c);
    }

  }

  @Override
  public List<Map<String, Object>> getIndexes(String table, String db) {

    List<Map<String, Object>> l1 = new ArrayList<Map<String, Object>>();

    Connection c = null;
    ResultSet r = null;

    try {
      c = RDB.getConnection(db);
      DatabaseMetaData d1 = c.getMetaData();
      r = d1.getIndexInfo(null, null, table, false, true);

      Map<String, Map<String, Object>> indexes = new TreeMap<String, Map<String, Object>>();
      while (r.next()) {
        String name = r.getString("index_name").toLowerCase();
        Map<String, Object> m = indexes.get(name);
        if (m == null) {
          m = new LinkedHashMap<String, Object>();
          indexes.put(name, m);
        }

        // ResultSetMetaData m1 = r.getMetaData();
        // for (int i = 1; i <= m1.getColumnCount(); i++) {
        // System.out.println(m1.getColumnName(i) + "=" + r.getObject(i));
        // }
        m.put(r.getString("column_name").toLowerCase(), X.isSame(r.getString("asc_or_desc"), "A") ? 1 : -1);
      }

      for (String name : indexes.keySet()) {
        Map<String, Object> m1 = new LinkedHashMap<String, Object>();
        m1.put("name", name);
        m1.put("key", indexes.get(name));
        l1.add(m1);
      }

    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      RDSHelper.close(r, c);
    }

    return l1;
  }

  @Override
  public void dropIndex(String table, String name, String db) {
    Connection c = null;
    Statement stat = null;
    try {
      c = RDB.getConnection(db);
      stat = c.createStatement();
      stat.executeUpdate("drop index " + name);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      RDSHelper.close(stat, c);
    }
  }

  @Override
  public int insertTable(String table, List<V> values, String db) {

    /**
     * insert it in database
     */
    PreparedStatement p = null;
    Connection c = null;
    try {
      if (X.isEmpty(values))
        return 0;

      c = getConnection(db);

      /**
       * create the sql statement
       */
      StringBuilder sql = new StringBuilder();
      sql.append("insert into ").append(table).append(" (");
      StringBuilder s = new StringBuilder();
      int total = 0;
      boolean isoracle = isOracle(c);

      for (String name : values.get(0).names()) {
        if (s.length() > 0)
          s.append(",");
        if (isoracle && oracle.containsKey(name)) {
          s.append(oracle.get(name));
        } else {
          s.append(name);
        }
        total++;
      }
      sql.append(s).append(") values( ");

      for (int i = 0; i < total - 1; i++) {
        sql.append("?, ");
      }
      sql.append("?)");

      p = c.prepareStatement(sql.toString());

      int order = 1;
      for (V sets : values) {
        for (String name : sets.names()) {
          Object v = sets.value(name);
          setParameter(p, order++, v);
        }

        p.addBatch();
      }

      return X.sum(p.executeBatch());

    } catch (Exception e) {
      if (log.isErrorEnabled())
        log.error(values.toString(), e);
    } finally {
      close(p);
    }
    return 0;
  }

}
