/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package sample;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder;
import com.datastax.oss.driver.api.querybuilder.schema.CreateKeyspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;

public class App {

  CqlSession session;
  private Logger logger = LoggerFactory.getLogger(App.class);

  public static void main(String[] args) {
    App app = new App();
    try {
      app.connect();
      //app.createDataStructure();
      app.insertData();
      app.queryData();

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      app.close();
    }
  }

  private void close() {
    if (!session.isClosed()) {
      session.close();
    }
  }

  private void insertData() {
    if (!session.isClosed()) {
      try {
        logger.info("inserting data");
        session.execute("INSERT INTO genesys.emails (id, subject, body, userid)\n"
          + "    VALUES (904b88b2-9c61-4539-952e-c179a3805b22, 'Hello world', 'Cassandra is great, "
          + "but it''s even better with EsIndex and Elasticsearch', 42);");
        logger.info("data inserted");
      } catch (Exception e) {
        session.close();
        logger.warn("Something went wrong", e);
      }
    }

  }

  private void queryData() {
    if (!session.isClosed()) {
      try {
        logger.info("query data in elastic search through cassandra (query='body:cassan*';)");
        final ResultSet resultSet =
          session.execute("select id, subject, body, userid, query  from genesys.emails where query='body:cassan*';");
        for (Row row : resultSet) {
          logger.info(row.getFormattedContents());
        }
      } catch (Exception e) {
        session.close();
        logger.warn("Something went wrong", e);
      }
    }

  }

  private void createDataStructure() {
    try {
      CreateKeyspace createKs = SchemaBuilder.createKeyspace("genesys").ifNotExists().withSimpleStrategy(1);
      session.execute(createKs.build());

      logger.info("creating table");
      session.execute("CREATE TABLE genesys.emails (\n"
        + "   id UUID PRIMARY KEY,\n"
        + "   subject text,\n"
        + "   body text,\n"
        + "   userid int,\n"
        + "   query text\n"
        + ");");

      logger.info("table created");
      logger.info("creating index");
      session.execute("CREATE CUSTOM INDEX ON genesys.emails(query)\n"
        + "USING 'com.genesyslab.webme.commons.index.EsSecondaryIndex'\n"
        + "WITH OPTIONS = {'unicast-hosts': 'elasticsearch:9200'};");
      logger.info("closing connection");
    } catch (Exception e) {
      session.close();
      logger.warn("Something went wrong", e);
    }
    logger.info("all created");

  }

  public synchronized Boolean connect() {
    logger.info("connecting to cassandra");

    DriverConfigLoader loader = DriverConfigLoader.programmaticBuilder()
      .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(5))
      .startProfile("slow")
      .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(30))
      .endProfile()
      .build();

    session = CqlSession.builder().withConfigLoader(loader)
      .addContactPoint(new InetSocketAddress("localhost", 9041))
      .withLocalDatacenter("datacenter1")
      .build();
    logger.info("connected");
    return true;
  }
}
