/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.lens.server;

import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.apache.lens.api.LensConf;
import org.apache.lens.api.LensSessionHandle;
import org.apache.lens.api.query.LensQuery;
import org.apache.lens.api.query.QueryHandle;
import org.apache.lens.api.query.QueryStatus;
import org.apache.lens.api.response.LensResponse;
import org.apache.lens.api.response.NoErrorPayload;
import org.apache.lens.server.api.LensConfConstants;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.TableType;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.ql.metadata.Hive;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.metadata.Table;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.testng.Assert;

/**
 * The Class LensTestUtil.
 */
public final class LensTestUtil {

  public static final String DB_WITH_JARS = "test_db_static_jars";
  public static final String DB_WITH_JARS_2 = "test_db_static_jars_2";
  private LensTestUtil() {

  }

  /**
   * Creates the table.
   *
   * @param tblName       the tbl name
   * @param parent        the parent
   * @param lensSessionId the lens session id
   * @param schemaStr     the schema string, with surrounding parenthesis.
   * @throws InterruptedException the interrupted exception
   */
  public static void createTable(String tblName, WebTarget parent, LensSessionHandle lensSessionId, String schemaStr)
    throws InterruptedException {
    LensConf conf = new LensConf();
    conf.addProperty(LensConfConstants.QUERY_PERSISTENT_RESULT_INDRIVER, "false");
    final WebTarget target = parent.path("queryapi/queries");

    final FormDataMultiPart mp = new FormDataMultiPart();
    String createTable = "CREATE TABLE IF NOT EXISTS " + tblName + schemaStr;

    mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("sessionid").build(), lensSessionId,
      MediaType.APPLICATION_XML_TYPE));
    mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("query").build(), createTable));
    mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("operation").build(), "execute"));
    mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("conf").fileName("conf").build(), conf,
      MediaType.APPLICATION_XML_TYPE));

    final QueryHandle handle = target.request()
        .post(Entity.entity(mp, MediaType.MULTIPART_FORM_DATA_TYPE),
            new GenericType<LensResponse<QueryHandle, NoErrorPayload>>() {}).getData();

    // wait till the query finishes
    LensQuery ctx = target.path(handle.toString()).queryParam("sessionid", lensSessionId).request()
      .get(LensQuery.class);
    QueryStatus stat = ctx.getStatus();
    while (!stat.finished()) {
      ctx = target.path(handle.toString()).queryParam("sessionid", lensSessionId).request().get(LensQuery.class);
      stat = ctx.getStatus();
      Thread.sleep(1000);
    }
    assertTrue(ctx.getSubmissionTime() > 0);
    assertTrue(ctx.getLaunchTime() > 0);
    assertTrue(ctx.getDriverStartTime() > 0);
    assertTrue(ctx.getDriverFinishTime() > 0);
    assertTrue(ctx.getFinishTime() > 0);
    Assert.assertEquals(ctx.getStatus().getStatus(), QueryStatus.Status.SUCCESSFUL);
  }

  public static void createTable(String tblName, WebTarget parent, LensSessionHandle lensSessionId)
    throws InterruptedException {
    createTable(tblName, parent, lensSessionId, "(ID INT, IDSTR STRING)");
  }

  public static void loadData(String tblName, final String testDataFile, WebTarget parent,
      LensSessionHandle lensSessionId) throws InterruptedException {
    LensConf conf = new LensConf();
    conf.addProperty(LensConfConstants.QUERY_PERSISTENT_RESULT_INDRIVER, "false");
    final WebTarget target = parent.path("queryapi/queries");

    final FormDataMultiPart mp = new FormDataMultiPart();
    String dataLoad = "LOAD DATA LOCAL INPATH '" + testDataFile + "' OVERWRITE INTO TABLE " + tblName;

    mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("sessionid").build(), lensSessionId,
        MediaType.APPLICATION_XML_TYPE));
    mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("query").build(), dataLoad));
    mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("operation").build(), "execute"));
    mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("conf").fileName("conf").build(), conf,
        MediaType.APPLICATION_XML_TYPE));

    final QueryHandle handle = target.request().post(Entity.entity(mp, MediaType.MULTIPART_FORM_DATA_TYPE),
        new GenericType<LensResponse<QueryHandle, NoErrorPayload>>() {}).getData();

    // wait till the query finishes
    LensQuery ctx = target.path(handle.toString()).queryParam("sessionid", lensSessionId).request()
        .get(LensQuery.class);
    QueryStatus stat = ctx.getStatus();
    while (!stat.finished()) {
      ctx = target.path(handle.toString()).queryParam("sessionid", lensSessionId).request().get(LensQuery.class);
      stat = ctx.getStatus();
      Thread.sleep(1000);
    }
    Assert.assertEquals(ctx.getStatus().getStatus(), QueryStatus.Status.SUCCESSFUL);
  }
  /**
   * Load data.
   *
   * @param tblName        the tbl name
   * @param testDataFile the test data file
   * @param parent         the parent
   * @param lensSessionId  the lens session id
   * @throws InterruptedException the interrupted exception
   */
  public static void loadDataFromClasspath(String tblName, final String testDataFile, WebTarget parent,
      LensSessionHandle lensSessionId) throws InterruptedException {

    String absolutePath = LensTestUtil.class.getClassLoader().getResource(testDataFile).getPath();
    loadData(tblName, absolutePath, parent, lensSessionId);
  }

  /**
   * Drop table.
   *
   * @param tblName       the tbl name
   * @param parent        the parent
   * @param lensSessionId the lens session id
   * @throws InterruptedException the interrupted exception
   */
  public static void dropTable(String tblName, WebTarget parent, LensSessionHandle lensSessionId)
    throws InterruptedException {
    LensConf conf = new LensConf();
    conf.addProperty(LensConfConstants.QUERY_PERSISTENT_RESULT_INDRIVER, "false");
    dropTableWithConf(tblName, parent, lensSessionId, conf);
  }

  /**
   * Drop table with conf passed.
   *
   * @param tblName       the tbl name
   * @param parent        the parent
   * @param lensSessionId the lens session id
   * @param conf          the query conf
   *
   * @throws InterruptedException
   */
  public static void dropTableWithConf(String tblName, WebTarget parent, LensSessionHandle lensSessionId,
    LensConf conf) throws InterruptedException {
    final WebTarget target = parent.path("queryapi/queries");

    final FormDataMultiPart mp = new FormDataMultiPart();
    String createTable = "DROP TABLE IF EXISTS " + tblName;

    mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("sessionid").build(), lensSessionId,
      MediaType.APPLICATION_XML_TYPE));
    mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("query").build(), createTable));
    mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("operation").build(), "execute"));
    mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("conf").fileName("conf").build(), conf,
      MediaType.APPLICATION_XML_TYPE));

    final QueryHandle handle = target.request().post(Entity.entity(mp, MediaType.MULTIPART_FORM_DATA_TYPE),
        new GenericType<LensResponse<QueryHandle, NoErrorPayload>>() {}).getData();

    // wait till the query finishes
    LensQuery ctx = target.path(handle.toString()).queryParam("sessionid", lensSessionId).request()
      .get(LensQuery.class);
    QueryStatus stat = ctx.getStatus();
    while (!stat.finished()) {
      ctx = target.path(handle.toString()).queryParam("sessionid", lensSessionId).request().get(LensQuery.class);
      stat = ctx.getStatus();
      Thread.sleep(1000);
    }
    Assert.assertEquals(ctx.getStatus().getStatus(), QueryStatus.Status.SUCCESSFUL);
  }

  /**
   * Creates the hive table.
   *
   * @param tableName the table name
   * @throws HiveException the hive exception
   */
  public static void createHiveTable(String tableName) throws HiveException {
    List<FieldSchema> columns = new ArrayList<FieldSchema>();
    columns.add(new FieldSchema("col1", "string", ""));
    List<FieldSchema> partCols = new ArrayList<FieldSchema>();
    partCols.add(new FieldSchema("pcol1", "string", ""));
    Map<String, String> params = new HashMap<String, String>();
    params.put("test.hive.table.prop", "tvalue");
    Table tbl = Hive.get().newTable(tableName);
    tbl.setTableType(TableType.MANAGED_TABLE);
    tbl.getTTable().getSd().setCols(columns);
    tbl.setPartCols(partCols);
    tbl.getTTable().getParameters().putAll(params);
    Hive.get().createTable(tbl);
  }

  /**
   * Drop hive table.
   *
   * @param tableName the table name
   * @throws HiveException the hive exception
   */
  public static void dropHiveTable(String tableName) throws HiveException {
    Hive.get().dropTable(tableName);
  }

  public static void createTestDatabaseResources(String[] testDatabases, HiveConf conf) throws Exception {
    File resDir = new File("target/resources");
    if (!resDir.exists()) {
      resDir.mkdir();
    }

    // Create databases and resource dirs
    Hive hive = Hive.get(conf);
    File testJarFile = new File("testdata/test.jar");
    File serdeJarFile = new File("testdata/serde.jar");

    for (String db : testDatabases) {
      Database database = new Database();
      database.setName(db);
      hive.createDatabase(database, true);
      File dbDir = new File(resDir, db);
      if (!dbDir.exists()) {
        dbDir.mkdir();
      }
      // Add a jar in the directory
      try {

        String[] jarOrder = {
          "x_" + db + ".jar",
          "y_" + db + ".jar",
          "z_" + db + ".jar",
          "serde.jar",
        };

        // Jar order is -> z, y, x, File listing order is x, y, z
        // We are explicitly specifying jar order
        FileUtils.writeLines(new File(dbDir, "jar_order"), Arrays.asList(jarOrder[2], jarOrder[1],
          jarOrder[0], jarOrder[3]));

        FileUtils.copyFile(testJarFile, new File(dbDir, jarOrder[0]));
        FileUtils.copyFile(testJarFile, new File(dbDir, jarOrder[1]));
        FileUtils.copyFile(testJarFile, new File(dbDir, jarOrder[2]));
        FileUtils.copyFile(serdeJarFile, new File(dbDir, jarOrder[3]));
      } catch (FileNotFoundException fnf) {
        fnf.printStackTrace();
      }
    }
  }
}
