/**
 * ShutdownModule documentation
 * 		An embedded Module. ShutdownModule shuts down the entire server system after receiving the request containing the [request_type]
 * 	'exit_system'.
 */
package bm.modules;

import bm.ComponentRepository;
import bm.modules.parents.Module;
import main.TransTechSystem;
import main.objects.request_response.Request;
import mqtt.MQTTHandler;
import tools.TrafficController;

public class ShutdownModule implements Runnable {

	public void shutdown() {
		Thread t = new Thread(this, "SHUTDOWN");
		t.start();
	}

	public void run() {
		TransTechSystem.forceShutdown();
	}
}
