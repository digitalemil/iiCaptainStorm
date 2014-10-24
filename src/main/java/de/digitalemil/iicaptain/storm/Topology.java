package de.digitalemil.iicaptain.storm;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import storm.kafka.BrokerHosts;
import storm.kafka.KafkaConfig;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.ZkHosts;

import org.apache.storm.hbase.*;
import org.apache.storm.hbase.bolt.HBaseBolt;
import org.apache.storm.hbase.bolt.mapper.HBaseMapper;
import org.apache.storm.hbase.bolt.mapper.SimpleHBaseMapper;
import org.apache.storm.hbase.common.ColumnList;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;


public class Topology {

	public static final String KAFKA_SPOUT_ID = "kafka-spout";
	public static final String INDEXSOLR_BOLT_ID = "indexsolr-bolt";
	public static final String HBASE_LONGTERM_BOLT_ID = "hbaselongterm-bolt";
	public static final String HBASE_SHORTTERM_BOLT_ID = "hbaseshortterm-bolt";

	public static final String TUPLETRANSFORMER_BOLT_ID = "tupletransformer-bolt";
	public static final String TOPOLOGY_NAME = "iicaptain-topology";

	public static void main(String[] args) throws Exception {
		
		if(args.length!= 2) {
			System.err.println("Error Deploying Topology. Two arguments needed:\n1. ZooKeeper Hosts  e.g. 127.0.0.1\n2. Solr URL e.g. http://127.0.0.1:8983/solr/locations/update/json?commit=true");
		}
		BrokerHosts hosts = new ZkHosts(args[0]);
		
		SpoutConfig spoutConfig = new SpoutConfig(hosts, "iicaptain", "/kafkastorm","src");
	//	spoutConfig.forceFromStart= true;
		spoutConfig.useStartOffsetTimeIfOffsetOutOfRange= true;
		spoutConfig.startOffsetTime= System.currentTimeMillis();
	
		KafkaSpout kafkaSpout = new KafkaSpout(spoutConfig);
		IndexSolr index= new IndexSolr(args[1]);
		
		String [] keys= {"user","latitude","longitude", "altitude", "accuracy", "altitudeAccuracy", "heading", "speed", "timestamp", "iata" };
	
		List<String> fields = new ArrayList<String>();
		for (int i = 0; i < keys.length; i++) {
			fields.add(keys[i]);
		}

		SimpleHBaseMapper mapper = new SimpleHBaseMapper() 
        .withRowKeyField(keys[0])
        .withColumnFields(new Fields(fields))
        .withColumnFamily("l");
		
		HBaseBolt ohbolt= new HBaseBolt("iicaptain-1min", mapper);
		
		SimpleHBaseMapper mapperl = new SimpleHBaseMapper() 
        .withRowKeyField(keys[0])
        .withColumnFields(new Fields(fields))
        .withColumnFamily("l");
		
		HBaseBolt ohboltl= new HBaseBolt("iicaptain", mapperl);
			
		//MyHBaseBolt hbase = new MyHBaseBolt("iicaptain-1min", "l", keys[0], fields);
	
		TupleTransformer tt= new TupleTransformer(keys);

		TopologyBuilder builder = new TopologyBuilder();		
		
		builder.setSpout(KAFKA_SPOUT_ID, kafkaSpout);
			
		builder.setBolt(TUPLETRANSFORMER_BOLT_ID, tt).shuffleGrouping(KAFKA_SPOUT_ID);
		
		builder.setBolt(INDEXSOLR_BOLT_ID, index).shuffleGrouping(TUPLETRANSFORMER_BOLT_ID);
		builder.setBolt(HBASE_SHORTTERM_BOLT_ID, ohbolt).shuffleGrouping(TUPLETRANSFORMER_BOLT_ID);
		builder.setBolt(HBASE_LONGTERM_BOLT_ID, ohboltl).shuffleGrouping(TUPLETRANSFORMER_BOLT_ID);		
		
		Config config = new Config();
		
		config.setNumAckers(2);
		
		//LocalCluster cluster = new LocalCluster();
		//cluster.submitTopology(TOPOLOGY_NAME, config, builder.createTopology());
		//Utils.waitForSeconds(1000);
		//cluster.killTopology(TOPOLOGY_NAME);
		//cluster.shutdown();
		
		StormSubmitter.submitTopology(TOPOLOGY_NAME, config, builder.createTopology());
	}
}

