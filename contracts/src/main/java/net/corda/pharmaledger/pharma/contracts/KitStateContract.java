package net.corda.pharmaledger.pharma.contracts;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.pharmaledger.pharma.states.KitState;

import static net.corda.core.contracts.ContractsDSL.requireThat;
public class KitStateContract implements Contract {

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        List<ContractState> outputs = tx.getOutputStates();
        requireThat(require -> {
            KitState kit = (KitState) outputs.get(0);
			require.using("ID should be greater than 0", kit.getKitID() > 0);
            // Patient and Shipment contracts verified...
            require.using("Medication should not be empty",!kit.getMedication().isEmpty() );
            return null;
        });
    }

}    