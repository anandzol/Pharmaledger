package net.corda.pharmaledger.logistics.contracts;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.pharmaledger.logistics.states.ShipmentState;

import static net.corda.core.contracts.ContractsDSL.requireThat;
public class ShipmentStateContract implements Contract {

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        List<ContractState> outputs = tx.getOutputStates();
		requireThat(require -> {
			ShipmentState shipment = (ShipmentState) outputs.get(0);
			require.using("ID should be greater than 0", shipment.getShipmentID() > 0);
			require.using("Shipment code should not be empty", !StringUtils.isEmpty(shipment.getShipmentLabel().get("code").getAsString()));
			require.using("shipment status should not be empty", !StringUtils.isEmpty(shipment.getStatus()));
            return null;
		});
        
    }

}    