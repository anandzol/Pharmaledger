package net.corda.pharmaledger.pharma.contracts;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.contracts.ContractState;
import net.corda.pharmaledger.pharma.states.MedicalStaffState;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class MedicalStaffStateContract implements Contract {

	@Override
	public void verify(LedgerTransaction tx) throws IllegalArgumentException {
		List<ContractState> outputs = tx.getOutputStates();
		requireThat(require -> {
			MedicalStaffState staff = (MedicalStaffState) outputs.get(0);
			require.using("ID should be greater than 0", staff.getStaffID() > 0);
            require.using("Name should not be empty", !StringUtils.isEmpty(staff.getStaffName()));
			require.using("Proficiency should not be empty", !StringUtils.isEmpty(staff.getProficiency()));
			return null;
		});
	}
    
}
