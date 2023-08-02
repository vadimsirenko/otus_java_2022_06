package ru.otus.crm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.cachehw.HwCache;
import ru.otus.cachehw.MyCache;
import ru.otus.core.repository.DataTemplate;
import ru.otus.core.sessionmanager.TransactionRunner;
import ru.otus.crm.model.Client;

import java.util.List;
import java.util.Optional;

public class DbServiceClientImpl implements DBServiceClient {
    private static final Logger log = LoggerFactory.getLogger(DbServiceClientImpl.class);

    private final DataTemplate<Client> dataTemplate;
    private final TransactionRunner transactionRunner;

    private final HwCache<String, Client> innerCache;

    private final boolean useCache;

    public DbServiceClientImpl(TransactionRunner transactionRunner, DataTemplate<Client> dataTemplate, boolean useCache) {
        this.transactionRunner = transactionRunner;
        this.dataTemplate = dataTemplate;
        this.innerCache = new MyCache<>();
        this.useCache = useCache;
    }

    public DbServiceClientImpl(TransactionRunner transactionRunner, DataTemplate<Client> dataTemplate) {
        this(transactionRunner, dataTemplate, false);
    }

    @Override
    public Client saveClient(Client client) {
        Client result = transactionRunner.doInTransaction(connection -> {
            if (client.getId() == null) {
                var clientId = dataTemplate.insert(connection, client);
                var createdClient = new Client(clientId, client.getName());
                log.info("created client: {}", createdClient);
                return createdClient;
            }
            dataTemplate.update(connection, client);
            log.info("updated client: {}", client);
            return client;
        });
        if (useCache) {
            String key = new String(result.getId().toString());
            innerCache.put(key, result);
        }
        return result;
    }

    @Override
    public Optional<Client> getClient(long id) {
        String key;
        if (useCache) {
            key = new String(((Long) id).toString());
            var val = innerCache.get(key);
            if (val != null) {
                return Optional.of(val);
            }
        } else {
            key = null;
        }
        Optional<Client> result = transactionRunner.doInTransaction(connection ->
        {
            var clientOptional = dataTemplate.findById(connection, id);
            log.info("client: {}", clientOptional);
            return clientOptional;
        });
        if (useCache) {
            log.info("Execute SELECT BY ID !!!");
            result.ifPresent(client -> innerCache.put(key, client));
        }
        return result;
    }

    @Override
    public List<Client> findAll() {
        var clientList = transactionRunner.doInTransaction(connection -> {
            var clients = dataTemplate.findAll(connection);
            log.info("clientList:{}", clients);
            return clients;
        });
        if (useCache) {
            innerCache.clear();
            for (Client client : clientList) {
                String key = new String(client.getId().toString());
                innerCache.put(key, client);
            }
        }
        return clientList;
    }
}
