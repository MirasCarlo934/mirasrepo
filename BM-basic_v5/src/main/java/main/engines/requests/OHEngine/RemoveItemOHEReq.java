package main.engines.requests.OHEngine;

public class RemoveItemOHEReq extends OHEngineRequest {
	private String cid;
	
	/**
	 * The EngineRequest used to remove a component from OH using REST API.
	 * <br><br>
	 * <i>Processing this EngineRequest only removes the item from the OH DB. To fully remove from 
	 * OH, also do an update() after detachment procedures.</i>
	 * 
	 * @param id The generated ID of this EngineRequest
	 * @param cid The CID of the component to be removed
	 */
	public RemoveItemOHEReq(String id, String cid) {
		super(id, OHRequestType.removeItem);
		this.cid = cid;
	}
	
	/**
	 * Returns the CID/MAC of the component to be removed
	 * @return
	 */
	public String getCID() {
		return cid;
	}
}
