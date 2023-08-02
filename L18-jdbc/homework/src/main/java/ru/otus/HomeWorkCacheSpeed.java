package ru.otus;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.core.repository.executor.DbExecutorImpl;
import ru.otus.core.sessionmanager.TransactionRunnerJdbc;
import ru.otus.crm.datasource.DriverManagerDataSource;
import ru.otus.crm.model.Client;
import ru.otus.crm.service.DbServiceClientImpl;
import ru.otus.jdbc.mapper.DataTemplateJdbc;
import ru.otus.jdbc.mapper.EntityClassMetaData;
import ru.otus.jdbc.mapper.EntityClassMetaDataImpl;
import ru.otus.jdbc.mapper.EntitySQLMetaData;
import ru.otus.jdbc.mapper.EntitySQLMetaDataImpl;

import javax.sql.DataSource;
import java.util.List;

public class HomeWorkCacheSpeed {

    private static final String URL = "jdbc:postgresql://localhost:5430/demoDB";
    private static final String USER = "usr";
    private static final String PASSWORD = "pwd";

    private static final Logger log = LoggerFactory.getLogger(HomeWorkCache.class);

    public static void main(String[] args) {
// Общая часть
        var dataSource = new DriverManagerDataSource(URL, USER, PASSWORD);
        flywayMigrations(dataSource);
        TransactionRunnerJdbc transactionRunner = new TransactionRunnerJdbc(dataSource);
        DbExecutorImpl dbExecutor = new DbExecutorImpl();
        EntityClassMetaData entityClassMetaDataClient = new EntityClassMetaDataImpl<>(Client.class);
        EntitySQLMetaData entitySQLMetaDataClient = new EntitySQLMetaDataImpl(entityClassMetaDataClient);
        var dataTemplateClient = new DataTemplateJdbc<Client>(dbExecutor, entitySQLMetaDataClient);

        generate100Clients(transactionRunner, dataTemplateClient);

        long resMsWithCache = execTestRead100Clients(transactionRunner, dataTemplateClient, true);
        long resMsWithoutCache = execTestRead100Clients(transactionRunner, dataTemplateClient, false);

        System.out.println("with cache time:" + resMsWithCache + " ms");
        System.out.println("without cache time:" + resMsWithoutCache + " ms");
    }

    private static long execTestRead100Clients(TransactionRunnerJdbc transactionRunner ,DataTemplateJdbc<Client> dataTemplateClient, boolean useCache) {

        var dbServiceClient = new DbServiceClientImpl(transactionRunner, dataTemplateClient, useCache);
        long beginTime = System.currentTimeMillis();
        List<Client> clients = dbServiceClient.findAll();

        for (Client client: clients) {
            dbServiceClient.getClient(client.getId());
        }
        return System.currentTimeMillis() - beginTime;
    }

    private static void generate100Clients(TransactionRunnerJdbc transactionRunner ,DataTemplateJdbc<Client> dataTemplateClient) {
        var dbServiceClient = new DbServiceClientImpl(transactionRunner, dataTemplateClient);
        for(int i=1;i<=100;i++){
            dbServiceClient.saveClient(new Client("Client" + i));
        }
    }

    private static void flywayMigrations(DataSource dataSource) {
        log.info("db migration started...");
        var flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:/db/migration")
                .load();
        flyway.migrate();
        log.info("db migration finished.");
        log.info("***");
    }
}
