package net.corda.pharmaledger.pharma.contracts;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.contracts.ContractState;
import net.corda.pharmaledger.pharma.states.TrialState;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class TrialStateContract implements Contract {

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        List<ContractState> outputs = tx.getOutputStates();
		requireThat(require -> {
			TrialState trial = (TrialState) outputs.get(0);
			require.using("ID should be greater than 0", trial.getTrialID() > 0);
            require.using("Status should not be empty", !StringUtils.isEmpty(trial.getStatus()));
			return null;
		});
    }

}    