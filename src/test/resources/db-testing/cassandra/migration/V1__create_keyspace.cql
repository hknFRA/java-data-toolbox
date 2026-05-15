create keyspace IF NOT EXISTS unit_test
    WITH REPLICATION = {
        'class': 'NetworkTopologyStrategy'
        };