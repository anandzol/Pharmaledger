package net.corda.pharmaledger.pharma.contracts;

import static net.corda.core.contracts.ContractsDSL.requireThat;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.pharmaledger.pharma.states.TrialTemplateState;

public class TrialTemplateStateContract implements Contract {

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        List<ContractState> outputs = tx.getOutputStates();
		requireThat(require -> {
			TrialTemplateState trial = (TrialTemplateState) outputs.get(0);
			require.using("ID Should not be empty", !StringUtils.isEmpty(trial.getTrialTemplateID()));
            require.using("Result should not be empty", !StringUtils.isEmpty(trial.getTrialResult()));
            require.using("Directions should not be empty", !StringUtils.isEmpty(trial.getTrialDirection()));
			return null;
		});
    }
    
    public interface Commands extends CommandData {
        class Create implements Commands {}
    }
}
