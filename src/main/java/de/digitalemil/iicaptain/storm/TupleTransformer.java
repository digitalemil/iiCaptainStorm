package de.digitalemil.iicaptain.storm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.storm.hbase.bolt.HBaseBolt;
import org.apache.storm.hbase.bolt.mapper.HBaseMapper;
import org.json.*;
import backtype.storm.serialization.types.ArrayListSerializer;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class TupleTransformer extends BaseRichBolt {
	OutputCollector col;
	String[] definedKeys;

	public boolean keyContained(String key) {
		if (key == null)
			return false;
		for (int i = 0; i < definedKeys.length; i++) {
			if (definedKeys[i] == null)
				continue;
			if (definedKeys[i].equals(key)) {
				return true;
			}
		}
		return false;
	}

	public void transformTuple(Tuple tuple) {
		// Extract the data from Kafka and construct JSON doc
		Fields fields = tuple.getFields();
		String data = new String((byte[]) tuple.getValueByField(fields.get(0)));
		System.out.println("Tuple Key: " + fields.get(0));
		System.out.println("Tuple Value: " + data);

		JSONObject json = new JSONObject(new JSONTokener(data));
		Values val = new Values();

		String values[] = new String[definedKeys.length];

		for (int i = 0; i < definedKeys.length; i++) {
			try {
				values[i] = json.getString(definedKeys[i]);
				val.add(i, values[i]);
				System.out.println("Key: " + definedKeys[i] + " value: "
						+ values[i]);
			} catch (JSONException ex) {
				val.add(i, "null");
			}
		}

		col.emit(tuple, val);
		col.ack(tuple);
		System.out.println("Emitting values: " + val);
	}

	public TupleTransformer(String[] definedKeys) {
		this.definedKeys = definedKeys;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void execute(Tuple tuple) {
		try {
			transformTuple(tuple);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void prepare(Map map, TopologyContext ctx, OutputCollector col) {
		this.col = col;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer decl) {
		List<String> fields = new ArrayList<String>();
		for (int i = 0; i < definedKeys.length; i++) {
			fields.add(definedKeys[i]);
		}
		decl.declare(new Fields(fields));
	}
}
