package net.corda.pharmaledger.medical.contracts;

import static net.corda.core.contracts.ContractsDSL.requireThat;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.pharmaledger.medical.states.PatientEvaluationState;

public class PatientEvaluationStateContract implements Contract{
    public static final String ID = "net.corda.pharmaledger.medical.contracts.PatientEvaluationStateContract";
    
    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        List<ContractState> outputs = tx.getOutputStates();
		requireThat(require -> {
			PatientEvaluationState evaluation = (PatientEvaluationState) outputs.get(0);			
			require.using("Evaluation Result should not be empty", !StringUtils.isEmpty(evaluation.getEvaluationResult()) );
            return null;
		});
        
    }

    
	public interface Commands extends CommandData {
        class Create implements Commands {}
    }
    
}
