package de.digitalemil.iicaptain.storm;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.storm.hbase.bolt.HBaseBolt;
import org.apache.storm.hbase.bolt.mapper.HBaseMapper;
import org.apache.storm.hbase.common.ColumnList;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.storm.hbase.bolt.mapper.HBaseMapper;
import org.apache.storm.hbase.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

public class MyHBaseBolt extends BaseRichBolt {
	private String tableName, columnFamily, rowkey;
	protected OutputCollector collector;
	private Configuration config;
	private HTable table;
	private List<String> columns;

	private static final Logger LOG = LoggerFactory
			.getLogger(MyHBaseBolt.class);

	public MyHBaseBolt(String tn, String cf, String rk, List<String> cols)
			throws IOException {
		System.out.println("MyHBaseBolt construction.");
		tableName = tn;
		rowkey = rk;
		columnFamily = cf;
		columns = cols;
		System.out.println("MyHBaseBolt constructed.");
	}

	@Override
	public void execute(Tuple tuple) {
		System.out.println("MyHBaseBolt.execute()");
		byte[] rk = Bytes.toBytes(tuple.getStringByField(rowkey));
		System.out.println("MyHBaseBolt.execute() rowkey: " + new String(rk));
		Put p = new Put(rk);

		try {
			for (int i = 1; i < tuple.size(); i++) {
				System.out.println("MyHBaseBolt.execute() put: "
						+ columns.get(i) + " " + tuple.getString(i));
				p.add(Bytes.toBytes(columnFamily),
						Bytes.toBytes(columns.get(i)),
						Bytes.toBytes(tuple.getString(i)));
			}
			table.put(p);
		} catch (Exception e) {
			this.collector.fail(tuple);
			e.printStackTrace();
			return;
		}
		System.out.println("MyHBaseBolt.execute() ack tuple");
		this.collector.ack(tuple);
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
	}

	@Override
	public void prepare(Map map, TopologyContext arg1, OutputCollector col) {
		this.collector = col;
		System.out.println("MyHBaseBolt.prepare()");
		config = HBaseConfiguration.create();
		//config.set("zookeeper.znode.parent", "/hbase-unsecure");
		//config.set("hbase.rootdir", "hdfs://sandbox:8020/apps/hbase/data/");
		System.out.println("MyHBaseBolt.prepare(): Created config: " + config);
		System.out.println("MyHBaseBolt.prepare() HTable");
		try {
			table = new HTable(config, tableName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("MyHBaseBolt.prepare() HTable Object: " + table);
	}
}
