create keyspace IF NOT EXISTS public
    WITH REPLICATION = {
        'class': 'NetworkTopologyStrategy'
        };