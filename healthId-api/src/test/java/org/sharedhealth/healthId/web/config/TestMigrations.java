package org.sharedhealth.healthId.web.config;

import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.ConstantReconnectionPolicy;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import org.sharedhealth.healthId.cassandra.Migrations;

import java.util.Map;

public class TestMigrations extends Migrations {

    public TestMigrations(Map<String, String> env) {
        super(env);
    }

    @Override
    protected Cluster connectCluster() {
        Cluster.Builder clusterBuilder = new Cluster.Builder();

        QueryOptions queryOptions = new QueryOptions();
        queryOptions.setConsistencyLevel(ConsistencyLevel.QUORUM);


        PoolingOptions poolingOptions = new PoolingOptions();

        clusterBuilder
                .withPort(Integer.parseInt(env.get("CASSANDRA_PORT")))
                .withClusterName(env.get("CASSANDRA_KEYSPACE"))
                .withLoadBalancingPolicy(new RoundRobinPolicy())
                .withPoolingOptions(poolingOptions)
                .withProtocolVersion(Integer.parseInt(env.get("CASSANDRA_VERSION")))
                .withQueryOptions(queryOptions)
                .withReconnectionPolicy(new ConstantReconnectionPolicy(ONE_MINUTE))
                .addContactPoint(env.get("CASSANDRA_HOST"));
        return clusterBuilder.build();

    }

    @Override
    protected Session createSession(Cluster cluster) {
        String keyspace = env.get("CASSANDRA_KEYSPACE");

        Session session = cluster.connect();
        session.execute(
                String.format(
                        "CREATE KEYSPACE  IF NOT EXISTS %s WITH replication = {'class':'SimpleStrategy', 'replication_factor':1}; ",
                        keyspace)
        );
        session.close();
        return cluster.connect(keyspace);
    }
}
