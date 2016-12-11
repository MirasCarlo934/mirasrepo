package bm.modules.submodules;

import java.sql.ResultSet;
import java.sql.SQLException;

import main.TransTechSystem;
import main.objects.EventRequest;
import main.objects.Reservation;
import main.objects.DB.DBTableConfig;
import main.objects.request_response.TransactionRequest;
import tools.Time;
import tools.TrafficController;

public class EventRequestTransactionCondition extends TransactionCondition {
	private static final String object_type = "event_request";
	
	private static final String col_name = "name";
	private static final String col_date			= "petsa";
	private static final String col_timeStart	= "timestart";
	private static final String col_timeEnd		= "timeend";
	private static final String col_agenda		= "agenda";
	private static final String col_reservee		= "reservee";
	
	//private static final String vals_param_name = "vals";

	public EventRequestTransactionCondition(TrafficController trafficController) {
		super("BM/EventRequestTransactionCondition", object_type, trafficController);
	}

	@Override
	protected boolean selectCondition(TransactionRequest treq) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	protected boolean insertCondition(TransactionRequest treq) {
		boolean b = true;
		DBTableConfig table = TransTechSystem.config.getDatabaseConfig().getDbTableConfigs().get(object_type);
		//Reservation res = new Reservation(treq.toJSONObject().getJSONObject(vals_param_name));
		EventRequest ereq = new EventRequest(treq.toJSONObject());
		
		ResultSet rs;
		try {
			rs = getTrafficController().selectQuery("*", table.getTableName(), "petsa='" + ereq.getDate().getDateString() + "'");
			if (!rs.isBeforeFirst() ) { //for case 1 and 2
				setError("none");
			}
			else {
				while(rs.next()) {
					String ename = rs.getString(col_name); //room_id of existing reservation
					Time ets = new Time(rs.getString(col_timeStart)); //ets = Existing reservation TimeStart
					Time ete = new Time(rs.getString(col_timeEnd)); //ete = Existing reservation TimeEnd
					Time rts = ereq.getTimeStart(); //rts = Requested reservation TimeStart
					Time rte = ereq.getTimeEnd(); //rte = Requested reservation TimeEnd
					String[] etss = ets.getTimeString().split(":");
					String[] etes = ete.getTimeString().split(":");
					String[] rtss = rts.getTimeString().split(":");
					String[] rtes = rte.getTimeString().split(":");
					int etsval = Integer.parseInt(etss[0] + etss[1]);
					int eteval = Integer.parseInt(etes[0] + etes[1]);
					int rtsval = Integer.parseInt(rtss[0] + rtss[1]);
					int rteval = Integer.parseInt(rtes[0] + rtes[1]);
					
					if(etsval <= rtsval && rtsval <= eteval) { //for case 3a
						setError("time-3a"); 
						b = false;
					} 
					else if(etsval <= rteval && rteval <= eteval) { //for case 3b
						setError("time-3b");
						b = false; 
					}
				}
			}
		} catch (SQLException e) {
			b = false;
			setError("SQLException");
			getLogger().fatal("SQL Error!");
			getLogger().trace(e);
		}
		
		return b;
	}

	@Override
	protected boolean updateCondition(TransactionRequest treq) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	protected boolean deleteCondition(TransactionRequest treq) {
		// TODO Auto-generated method stub
		return true;
	}

}
