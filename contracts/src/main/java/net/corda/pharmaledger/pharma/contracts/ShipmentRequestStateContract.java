package net.corda.pharmaledger.pharma.contracts;

import static net.corda.core.contracts.ContractsDSL.requireThat;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.pharmaledger.pharma.states.ShipmentRequestState;

public class ShipmentRequestStateContract implements Contract {

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        List<ContractState> outputs = tx.getOutputStates();
		requireThat(require -> {
			ShipmentRequestState shipment = (ShipmentRequestState) outputs.get(0);
            require.using("Shipment Mapping ID should not be empty", !StringUtils.isEmpty(shipment.getShipmentMappingID()));
            require.using("Package ID should not be empty", !StringUtils.isEmpty(shipment.getPackageID()));
			return null;
		});
    }
    
    public interface Commands extends CommandData {
        class Create implements Commands {}
    }
}
