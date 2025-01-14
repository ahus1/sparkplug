/*******************************************************************************
 * Copyright (c) 2022 Ian Craggs
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Ian Craggs - initial implementation and documentation
 *******************************************************************************/

package org.eclipse.sparkplug.tck.test.edge;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectPacket;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extension.sdk.api.packets.subscribe.SubscribePacket;
import com.hivemq.extension.sdk.api.packets.subscribe.Subscription;
import com.hivemq.extension.sdk.api.packets.general.Qos;

import org.eclipse.sparkplug.tck.sparkplug.Sections;
import org.eclipse.sparkplug.tck.test.TCK;
import org.eclipse.sparkplug.tck.test.TCKTest;
import org.eclipse.sparkplug.tck.test.common.SparkplugBProto.DataType;
import org.eclipse.sparkplug.tck.test.common.SparkplugBProto.Payload.Metric;
import org.eclipse.sparkplug.tck.test.common.SparkplugBProto.PayloadOrBuilder;
import org.eclipse.sparkplug.tck.test.common.TopicConstants;
import org.eclipse.sparkplug.tck.test.common.Utils;

import org.jboss.test.audit.annotations.SpecAssertion;
import org.jboss.test.audit.annotations.SpecVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.eclipse.sparkplug.tck.test.common.Requirements.*;
import static org.eclipse.sparkplug.tck.test.common.TopicConstants.*;
import static org.eclipse.sparkplug.tck.test.common.Utils.setResult;

/**
 * This is the edge node Sparkplug session termination test
 * 
 * The purpose is to test NDEATH and DDEATH messages
 *
 * @author Ian Craggs
 */
@SpecVersion(
		spec = "sparkplug",
		version = "3.0.0-SNAPSHOT")
public class SessionTerminationTest extends TCKTest {
	private static final @NotNull Logger logger = LoggerFactory.getLogger("Sparkplug");

	private final @NotNull Map<String, String> testResults = new HashMap<>();
	private final @NotNull List<String> testIds = List.of(ID_TOPICS_DDEATH_MQTT, ID_TOPICS_DDEATH_SEQ_NUM,
			ID_PAYLOADS_DDEATH_TIMESTAMP, ID_PAYLOADS_DDEATH_SEQ, ID_PAYLOADS_DDEATH_SEQ_INC,
			ID_PAYLOADS_DDEATH_SEQ_NUMBER, ID_OPERATIONAL_BEHAVIOR_EDGE_NODE_INTENTIONAL_DISCONNECT_NDEATH,
			ID_OPERATIONAL_BEHAVIOR_EDGE_NODE_INTENTIONAL_DISCONNECT_PACKET, ID_OPERATIONAL_BEHAVIOR_DEVICE_DDEATH);

	private final @NotNull TCK theTCK;
	private final @NotNull Map<String, Boolean> deviceIds = new HashMap<>();

	private @NotNull String testClientId = null;
	private @NotNull String hostApplicationId;
	private @NotNull String groupId;
	private @NotNull String edgeNodeId;
	private @NotNull String deviceId;

	private @NotNull boolean ndeathFound = false;
	private @NotNull boolean ddeathFound = false;

	public SessionTerminationTest(final @NotNull TCK aTCK, final @NotNull String[] parms) {
        logger.info("Edge Node session termination test. Parameters: {} ", Arrays.asList(parms));
        theTCK = aTCK;

        if (parms.length < 4) {
            logger.error("Parameters to edge session termination test must be: hostApplicationId groupId edgeNodeId deviceId");
            throw new IllegalArgumentException();
        }

        hostApplicationId = parms[0];
        groupId = parms[1];
        edgeNodeId = parms[2];
        deviceId = parms[3];

        logger.info("Host application id: {}, Group id: {}, Edge node id: {}, Device id: {}", hostApplicationId, groupId, edgeNodeId, deviceId);
    }

	public String getName() {
		return "SessionTermination";
	}

	public String[] getTestIds() {
		return testIds.toArray(new String[0]);
	}

	public Map<String, String> getResults() {
		return testResults;
	}

	@Override
	public void endTest(Map<String, String> results) {
		testResults.putAll(results);
		testClientId = null;
		Utils.setEndTest(getName(), testIds, testResults);
		reportResults(testResults);
	}

	public void connect(final @NotNull String clientId, final @NotNull ConnectPacket packet) {

	}

	@Override
	public void disconnect(String clientId, DisconnectPacket packet) {
		// TODO Auto-generated method stub

	}

	public void subscribe(final @NotNull String clientId, final @NotNull SubscribePacket packet) {

	}


	@SpecAssertion(
			section = Sections.PAYLOADS_DESC_DDEATH,
			id = ID_TOPICS_DDEATH_MQTT)
	@SpecAssertion(
			section = Sections.PAYLOADS_DESC_DDEATH,
			id = ID_TOPICS_DDEATH_SEQ_NUM)
	@SpecAssertion(
			section = Sections.PAYLOADS_B_DDEATH,
			id = ID_PAYLOADS_DDEATH_TIMESTAMP)
	@SpecAssertion(
			section = Sections.PAYLOADS_B_DDEATH,
			id = ID_PAYLOADS_DDEATH_SEQ)
	@SpecAssertion(
			section = Sections.PAYLOADS_B_DDEATH,
			id = ID_PAYLOADS_DDEATH_SEQ_INC)
	@SpecAssertion(
			section = Sections.PAYLOADS_B_DDEATH,
			id = ID_PAYLOADS_DDEATH_SEQ_NUMBER)
	
	@SpecAssertion(
			section = Sections.OPERATIONAL_BEHAVIOR_EDGE_NODE_SESSION_TERMINATION,
			id = ID_OPERATIONAL_BEHAVIOR_EDGE_NODE_INTENTIONAL_DISCONNECT_NDEATH)
	@SpecAssertion(
			section = Sections.OPERATIONAL_BEHAVIOR_EDGE_NODE_SESSION_TERMINATION,
			id = ID_OPERATIONAL_BEHAVIOR_EDGE_NODE_INTENTIONAL_DISCONNECT_PACKET)
	
	@SpecAssertion(
			section = Sections.OPERATIONAL_BEHAVIOR_DEVICE_SESSION_TERMINATION,
			id = ID_OPERATIONAL_BEHAVIOR_DEVICE_DDEATH)
	public void publish(final @NotNull String clientId, final @NotNull PublishPacket packet) {
			
		// topics namespace/group_id/NDEATH/edge_node_id
		//        namespace/group_id/DDEATH/edge_node_id/device_id
		
		String topic = packet.getTopic();
		String[] topicLevels = topic.split("/");
		
		if (topicLevels.length == 4 &&
				topicLevels[0].equals(TOPIC_ROOT_SP_BV_1_0) && 
				topicLevels[1].equals(groupId) &&
				topicLevels[2].equals("NDEATH") &&
				topicLevels[3].equals(edgeNodeId)) {
			ndeathFound = true;
			logger.info("Edge session termination test - publish - to topic: {} ", packet.getTopic());
		}
		
		if (topicLevels.length == 5 &&
				topicLevels[0].equals(TOPIC_ROOT_SP_BV_1_0) && 
				topicLevels[1].equals(groupId) &&
				topicLevels[2].equals("DDEATH") &&
				topicLevels[3].equals(edgeNodeId) && 
				topicLevels[4].equals(deviceId)) {
			ddeathFound = true;
			
			// DDEATH messages MUST be published with MQTT QoS equal to 0 and retain equal to false.
			boolean isValidMQTT = (packet.getQos() == Qos.AT_MOST_ONCE && packet.getRetain() == false);
			testResults.put(ID_TOPICS_DDEATH_MQTT, setResult(isValidMQTT, TOPICS_DDEATH_MQTT));
			
			// payload related tests
			PayloadOrBuilder payload = Utils.getSparkplugPayload(packet);
						
			// The DDEATH MUST include a sequence number in the payload and it MUST have a value of one 
			//   greater than the previous MQTT message from the Edge Node contained unless the previous 
			//   MQTT message contained a value of 255. In this case the sequence number MUST be 0.
			
			boolean isValidSeq = false; // TODO check sequence increment
			if (payload.hasSeq()) {
				long seq = payload.getSeq();
				if (seq >= 0 && seq <= 255) {
					isValidSeq = true;
				}
			}
			testResults.put(ID_TOPICS_DDEATH_SEQ_NUM, setResult(isValidSeq, TOPICS_DDEATH_SEQ_NUM));
			testResults.put(ID_PAYLOADS_DDEATH_SEQ_INC, setResult(isValidSeq, PAYLOADS_DDEATH_SEQ_INC));
			
			testResults.put(ID_PAYLOADS_DDEATH_TIMESTAMP, setResult(payload.hasTimestamp(), PAYLOADS_DDEATH_TIMESTAMP));
			
			testResults.put(ID_PAYLOADS_DDEATH_SEQ, setResult(payload.hasSeq(), PAYLOADS_DDEATH_SEQ));
			
			testResults.put(ID_PAYLOADS_DDEATH_SEQ_NUMBER, setResult(payload.hasSeq(), PAYLOADS_DDEATH_SEQ_NUMBER));
			
		}

	}

}
