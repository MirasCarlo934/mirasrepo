package bm.modules;

import bm.ComponentRepository;
import bm.modules.parents.Module;
import main.objects.request_response.Request;
import mqtt.MQTTHandler;

public class TestModule extends Module {
	private ComponentRepository cr;

	public TestModule(MQTTHandler mqttHandler, ComponentRepository cr) {
		super("TestModule", "test", null, mqttHandler);
		this.cr = cr;
	}

	@Override
	protected void process(Request request) throws Exception {
		cr.registerComponent("testmac1", "Test ESP for Reg", "0001", "salas");
	}
}
