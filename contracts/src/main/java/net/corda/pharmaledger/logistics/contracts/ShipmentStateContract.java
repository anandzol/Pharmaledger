package net.corda.pharmaledger.logistics.contracts;

import static net.corda.core.contracts.ContractsDSL.requireThat;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.pharmaledger.logistics.states.ShipmentState;
public class ShipmentStateContract implements Contract {

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
		List<String> validShipmentStatuslist = Arrays.asList(new String[]{"PACKED", "IN_TRANSIT", "DELIVERED"});
        List<ContractState> outputs = tx.getOutputStates();
		requireThat(require -> {
			ShipmentState shipment = (ShipmentState) outputs.get(0);
			require.using("ID should not be empty", !StringUtils.isEmpty(shipment.getpackageID()));
			//require.using("Shipment code should not be empty", !StringUtils.isEmpty(shipment.getShipmentLabel().get("code").getAsString()));
			require.using("shipment status should not be empty", validShipmentStatuslist.contains(shipment.getStatus()) );
            return null;
		});
        
    }
	
	public interface Commands extends CommandData {
        class Create implements Commands {}
    }
}    