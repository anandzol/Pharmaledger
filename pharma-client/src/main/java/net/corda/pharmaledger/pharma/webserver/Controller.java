package net.corda.pharmaledger.pharma.webserver;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r3.corda.lib.accounts.contracts.states.AccountInfo;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.corda.client.jackson.JacksonSupport;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria;
import net.corda.pharmaledger.accountUtilities.CreateNewAccount;
import net.corda.pharmaledger.accountUtilities.ShareAccountTo;
import net.corda.pharmaledger.medical.states.PatientEvaluationState;
import net.corda.pharmaledger.medical.states.PatientState;
import net.corda.pharmaledger.pharma.DeleteMedicalStaffData;
import net.corda.pharmaledger.pharma.EditTrial;
import net.corda.pharmaledger.pharma.SendMedicalStaffData;
import net.corda.pharmaledger.pharma.SendShipmentRequest;
import net.corda.pharmaledger.pharma.SendTrial;
import net.corda.pharmaledger.pharma.SendTrialTemplate;
import net.corda.pharmaledger.pharma.sendKitShipmentDetails;
import net.corda.pharmaledger.pharma.states.KitShipmentState;
import net.corda.pharmaledger.pharma.states.MedicalStaffState;
import net.corda.pharmaledger.pharma.states.ShipmentRequestState;
import net.corda.pharmaledger.pharma.states.TrialState;
import net.corda.pharmaledger.pharma.states.TrialTemplateState;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/v1/pharma") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private static final Logger logger = LoggerFactory.getLogger(RestController.class);
    private final CordaRPCOps proxy;
    private final CordaX500Name me;

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.me = proxy.nodeInfo().getLegalIdentities().get(0).getName();

    }

    /** Helpers for filtering the network map cache. */
    public String toDisplayString(X500Name name) {
        return BCStyle.INSTANCE.toString(name);
    }

    private boolean isNotary(NodeInfo nodeInfo) {
        return !proxy.notaryIdentities().stream().filter(el -> nodeInfo.isLegalIdentity(el))
                .collect(Collectors.toList()).isEmpty();
    }

    private boolean isMe(NodeInfo nodeInfo) {
        return nodeInfo.getLegalIdentities().get(0).getName().equals(me);
    }

    private boolean isNetworkMap(NodeInfo nodeInfo) {
        return nodeInfo.getLegalIdentities().get(0).getName().getOrganisation().equals("Network Map Service");
    }

    @Configuration
    class Plugin {
        @Bean
        public ObjectMapper registerModule() {
            return JacksonSupport.createNonRpcMapper();
        }
    }

    @GetMapping(value = "/status", produces = TEXT_PLAIN_VALUE)
    public String status() {
        return "200";
    }

    @GetMapping(value = "/servertime", produces = TEXT_PLAIN_VALUE)
    public String serverTime() {
        return (LocalDateTime.ofInstant(proxy.currentNodeTime(), ZoneId.of("UTC"))).toString();
    }

    @GetMapping(value = "/addresses", produces = TEXT_PLAIN_VALUE)
    public String addresses() {
        return proxy.nodeInfo().getAddresses().toString();
    }

    @GetMapping(value = "/identities", produces = TEXT_PLAIN_VALUE)
    public String identities() {
        return proxy.nodeInfo().getLegalIdentities().toString();
    }

    @GetMapping(value = "/platformversion", produces = TEXT_PLAIN_VALUE)
    public String platformVersion() {
        return Integer.toString(proxy.nodeInfo().getPlatformVersion());
    }

    @GetMapping(value = "/peers", produces = APPLICATION_JSON_VALUE)
    public HashMap<String, List<String>> getPeers() {
        HashMap<String, List<String>> myMap = new HashMap<>();

        // Find all nodes that are not notaries, ourself, or the network map.
        Stream<NodeInfo> filteredNodes = proxy.networkMapSnapshot().stream()
                .filter(el -> !isNotary(el) && !isMe(el) && !isNetworkMap(el));
        // Get their names as strings
        List<String> nodeNames = filteredNodes.map(el -> el.getLegalIdentities().get(0).getName().toString())
                .collect(Collectors.toList());

        myMap.put("peers", nodeNames);
        return myMap;
    }

    @GetMapping(value = "/notaries", produces = TEXT_PLAIN_VALUE)
    public String notaries() {
        return proxy.notaryIdentities().toString();
    }

    @GetMapping(value = "/flows", produces = TEXT_PLAIN_VALUE)
    public String flows() {
        return proxy.registeredFlows().toString();
    }

    @GetMapping(value = "/states", produces = TEXT_PLAIN_VALUE)
    public String states() {
        return proxy.vaultQuery(ContractState.class).getStates().toString();
    }

    public StateAndRef<AccountInfo> getAccountInfobyName(String accountName) {
        List<StateAndRef<AccountInfo>> accounts = proxy.vaultQuery(AccountInfo.class).getStates();
        return accounts.stream().filter(account -> account.getState().getData().getName().equals(accountName)).findAny()
                .orElse(null);
    }

    // APIs for Account Management

    @PostMapping(value = "/accounts/createaccount", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> createAccount(HttpServletRequest request) {
        String acctName = request.getParameter("acctName");
        try {
            String result = proxy.startTrackedFlowDynamic(CreateNewAccount.class, acctName).getReturnValue().get();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping(value = "/accounts/shareaccountto", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> shareAccountTo(HttpServletRequest request) {
        String acctName = request.getParameter("acctName");
        String shareTo = request.getParameter("shareTo");

        Set<Party> parties = proxy.partiesFromName(shareTo, false);
        Iterator it = parties.iterator();

        try {
            String result = proxy.startTrackedFlowDynamic(ShareAccountTo.class, acctName, it.next()).getReturnValue()
                    .get();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // Participant Management APIs

    @PostMapping(value = "/participants/assigntrial", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> assignTrial(HttpServletRequest request) throws IllegalArgumentException {
        int patientID = Integer.valueOf(request.getParameter("patientID"));
        int trialID = Integer.valueOf(request.getParameter("trialID"));
        String fromPharma = request.getParameter("fromPharma");
        String toMedical = request.getParameter("toMedical");

        try {
            String result = proxy.startTrackedFlowDynamic(EditTrial.class, trialID, patientID, fromPharma, toMedical)
                    .getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value = "/participants/getallparticipants", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<PatientState>>> getallparticipants(HttpServletRequest request) {
        String accountName = request.getParameter("accountName");
        StateAndRef<AccountInfo> account = getAccountInfobyName(accountName);
        if (account != null) {
            UUID accountID = account.getState().getData().getIdentifier().getId();
            QueryCriteria generalCriteria = new VaultQueryCriteria().withExternalIds(Arrays.asList(accountID));
            return ResponseEntity.ok(proxy.vaultQueryByCriteria(generalCriteria, PatientState.class).getStates());
        } else {
            throw new IllegalArgumentException("No Such account exist");
        }
    }

    @GetMapping(value = "/participants/getparticipants", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<PatientState>>> getparticipant(HttpServletRequest request) {
        int patientID = Integer.valueOf(request.getParameter("patientID"));
        String accountName = request.getParameter("accountName");
        StateAndRef<AccountInfo> account = getAccountInfobyName(accountName);
        if (account != null) {
            UUID accountID = account.getState().getData().getIdentifier().getId();
            QueryCriteria generalCriteria = new VaultQueryCriteria().withExternalIds(Arrays.asList(accountID));
            List<StateAndRef<PatientState>> patients = proxy.vaultQueryByCriteria(generalCriteria, PatientState.class)
                    .getStates().stream().filter(it -> it.getState().getData().getPatientID() == patientID)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(patients);
        } else {
            throw new IllegalArgumentException("No Such account exist");
        }
    }

    @GetMapping(value = "/participants/getevaluation", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<PatientEvaluationState>>> getevaluation(HttpServletRequest request) {
        int patientID = Integer.valueOf(request.getParameter("patientID"));
        String accountName = request.getParameter("accountName");
        StateAndRef<AccountInfo> account = getAccountInfobyName(accountName);
        if (account != null) {
            UUID accountID = account.getState().getData().getIdentifier().getId();
            QueryCriteria accountCriteria = new VaultQueryCriteria().withExternalIds(Arrays.asList(accountID));
            QueryCriteria stateCriteria = new VaultQueryCriteria(Vault.StateStatus.ALL);
            QueryCriteria generalCriteria = accountCriteria.and(stateCriteria);
            List<StateAndRef<PatientEvaluationState>> patients = proxy
                    .vaultQueryByCriteria(generalCriteria, PatientEvaluationState.class).getStates().stream()
                    .filter(it -> it.getState().getData().getPatientID() == patientID).collect(Collectors.toList());
            return ResponseEntity.ok(patients);
        } else {
            throw new IllegalArgumentException("No Such account exist");
        }
    }

    @PostMapping(value = "/participants/changetrialdates", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> changeTrialDates(HttpServletRequest request) throws IllegalArgumentException {
        int trialID = Integer.valueOf(request.getParameter("trialID"));
        String startDate = request.getParameter("startDate");
        String endDate = request.getParameter("endDate");
        String fromPharma = request.getParameter("fromPharma");
        String toMedical = request.getParameter("toMedical");

        try {
            String result = proxy
                    .startTrackedFlowDynamic(EditTrial.class, trialID, startDate, endDate, fromPharma, toMedical)
                    .getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // APIs for Trial Management

    @PostMapping(value = "/trials/createtrialtemplate", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> createTrialTemplate(HttpServletRequest request) throws IllegalArgumentException {
        String trialTemplateID = request.getParameter("trialTemplateID");
        String trialResult = request.getParameter("trialResult");
        String trialDirection = request.getParameter("trialDirection");
        String fromPharma = request.getParameter("fromPharma");
        String toMedical = request.getParameter("toMedical");

        try {
            String result = proxy.startTrackedFlowDynamic(SendTrialTemplate.class, trialTemplateID, trialResult,
                    trialDirection, fromPharma, toMedical).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value = "/trials/getalltrialtemplate", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<TrialTemplateState>>> getAllTemplate(HttpServletRequest request)
            throws IllegalArgumentException {
        String accountName = request.getParameter("accountName");
        StateAndRef<AccountInfo> account = getAccountInfobyName(accountName);
        if (account != null) {
            UUID accountID = account.getState().getData().getIdentifier().getId();
            QueryCriteria generalCriteria = new VaultQueryCriteria().withExternalIds(Arrays.asList(accountID));
            return ResponseEntity.ok(proxy.vaultQueryByCriteria(generalCriteria, TrialTemplateState.class).getStates());
        } else {
            throw new IllegalArgumentException("No Such account exist");
        }
    }

    @GetMapping(value = "/trials/gettrialtemplate", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<TrialTemplateState>>> getTemplate(HttpServletRequest request)
            throws IllegalArgumentException {
        String accountName = request.getParameter("accountName");
        String templateID = request.getParameter("templateID");
        StateAndRef<AccountInfo> account = getAccountInfobyName(accountName);
        if (account != null) {
            UUID accountID = account.getState().getData().getIdentifier().getId();
            QueryCriteria generalCriteria = new VaultQueryCriteria().withExternalIds(Arrays.asList(accountID));
            List<StateAndRef<TrialTemplateState>> trialTemplate = proxy
                    .vaultQueryByCriteria(generalCriteria, TrialTemplateState.class).getStates().stream()
                    .filter(it -> it.getState().getData().getTrialTemplateID().equals(templateID))
                    .collect(Collectors.toList());
            if (trialTemplate.isEmpty()) {
                throw new IllegalArgumentException("No Trial Template exist");
            }
            return ResponseEntity.ok(trialTemplate);
        } else {
            throw new IllegalArgumentException("No Such account exist");
        }
    }

    @PostMapping(value = "/trials/createtrial", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> createTrial(HttpServletRequest request) throws IllegalArgumentException {
        int trialID = Integer.valueOf(request.getParameter("trialID"));
        String trialPatients = request.getParameter("trialPatients");
        String trialTemplateID = request.getParameter("trialTemplateID");
        String trialStatus = request.getParameter("trialStatus");
        String trialStartDate = request.getParameter("trialStartDate");
        String trialEndDate = request.getParameter("trialEndDate");
        String fromPharma = request.getParameter("fromPharma");
        String toMedical = request.getParameter("toMedical");

        try {
            String result = proxy.startTrackedFlowDynamic(SendTrial.class, trialID, trialPatients, trialTemplateID,
                    trialStatus, trialStartDate, trialEndDate, fromPharma, toMedical).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value = "/trials/getalltrials", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<TrialState>>> getAllTrials(HttpServletRequest request)
            throws IllegalArgumentException {
        String accountName = request.getParameter("accountName");
        StateAndRef<AccountInfo> account = getAccountInfobyName(accountName);
        if (account != null) {
            UUID accountID = account.getState().getData().getIdentifier().getId();
            QueryCriteria generalCriteria = new VaultQueryCriteria().withExternalIds(Arrays.asList(accountID));
            return ResponseEntity.ok(proxy.vaultQueryByCriteria(generalCriteria, TrialState.class).getStates());
        } else {
            throw new IllegalArgumentException("No Such account exist");
        }
    }

    @GetMapping(value = "/trials/gettrial", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<TrialState>>> getTrial(HttpServletRequest request)
            throws IllegalArgumentException {
        String accountName = request.getParameter("accountName");
        int trialID = Integer.valueOf(request.getParameter("trialID"));
        StateAndRef<AccountInfo> account = getAccountInfobyName(accountName);
        if (account != null) {
            UUID accountID = account.getState().getData().getIdentifier().getId();
            QueryCriteria generalCriteria = new VaultQueryCriteria().withExternalIds(Arrays.asList(accountID));
            List<StateAndRef<TrialState>> trial = proxy.vaultQueryByCriteria(generalCriteria, TrialState.class)
                    .getStates().stream().filter(it -> it.getState().getData().getTrialID() == trialID)
                    .collect(Collectors.toList());
            if (trial.isEmpty()) {
                throw new IllegalArgumentException("No Trial exist");
            }
            return ResponseEntity.ok(trial);
        } else {
            throw new IllegalArgumentException("No Such account exist");
        }
    }

    @GetMapping(value = "/trials/trackshipment", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<ShipmentRequestState>>> trackShipment(HttpServletRequest request)
            throws IllegalArgumentException {
        String kitID = request.getParameter("KitID");
        String accountName = request.getParameter("accountName");
        StateAndRef<AccountInfo> account = getAccountInfobyName(accountName);
        if (account != null) {
            UUID accountID = account.getState().getData().getIdentifier().getId();
            QueryCriteria generalCriteria = new VaultQueryCriteria().withExternalIds(Arrays.asList(accountID));
            List<StateAndRef<KitShipmentState>> kits = proxy
                    .vaultQueryByCriteria(generalCriteria, KitShipmentState.class).getStates().stream()
                    .filter(it -> it.getState().getData().getKitID().equals(kitID)).collect(Collectors.toList());
            if (kits.isEmpty()) {
                throw new IllegalArgumentException("No such kit exist");
            }
            String packageID = kits.get(0).getState().getData().getPackageID();
            return ResponseEntity.ok(proxy.vaultQuery(ShipmentRequestState.class).getStates().stream()
                    .filter(it -> it.getState().getData().getPackageID().equals(packageID))
                    .collect(Collectors.toList()));
        } else {
            throw new IllegalArgumentException("No Such account exist");
        }
    }

    // APIs for Kit Management

    @PostMapping(value = "/kits/createshipment", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> createShipment(HttpServletRequest request) {
        String shipmentMappingID = request.getParameter("shipmentMappingID");
        String packageID = request.getParameter("packageID");
        String fromPharma = request.getParameter("fromPharma");
        String toLogistics = request.getParameter("toLogistics");
        String kitID = request.getParameter("kitID");

        try {
            String result = proxy.startTrackedFlowDynamic(SendShipmentRequest.class, shipmentMappingID, packageID,
                    fromPharma, toLogistics).getReturnValue().get();
            String result1 = proxy.startTrackedFlowDynamic(sendKitShipmentDetails.class, shipmentMappingID, packageID,
                    kitID, fromPharma, toLogistics).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body(result + "\n" + result1);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value = "/trials/getallkitshipment", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<KitShipmentState>>> getAllKitShipment(HttpServletRequest request)
            throws IllegalArgumentException {
        String accountName = request.getParameter("accountName");
        StateAndRef<AccountInfo> account = getAccountInfobyName(accountName);
        if (account != null) {
            UUID accountID = account.getState().getData().getIdentifier().getId();
            QueryCriteria generalCriteria = new VaultQueryCriteria().withExternalIds(Arrays.asList(accountID));
            return ResponseEntity.ok(proxy.vaultQueryByCriteria(generalCriteria, KitShipmentState.class).getStates());
        } else {
            throw new IllegalArgumentException("No Such account exist");
        }
    }

    @GetMapping(value = "/trials/getallshipmentrequest", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<ShipmentRequestState>>> getAllShipmentRequest(HttpServletRequest request)
            throws IllegalArgumentException {
        String accountName = request.getParameter("accountName");
        StateAndRef<AccountInfo> account = getAccountInfobyName(accountName);
        if (account != null) {
            UUID accountID = account.getState().getData().getIdentifier().getId();
            QueryCriteria generalCriteria = new VaultQueryCriteria().withExternalIds(Arrays.asList(accountID));
            return ResponseEntity
                    .ok(proxy.vaultQueryByCriteria(generalCriteria, ShipmentRequestState.class).getStates());
        } else {
            throw new IllegalArgumentException("No Such account exist");
        }
    }

    @GetMapping(value = "/trials/getshipmentrequest", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<ShipmentRequestState>>> getShipmentRequest(HttpServletRequest request) {
        String packageID = request.getParameter("packageID");
        String accountName = request.getParameter("accountName");
        StateAndRef<AccountInfo> account = getAccountInfobyName(accountName);
        if (account != null) {
            UUID accountID = account.getState().getData().getIdentifier().getId();
            QueryCriteria generalCriteria = new VaultQueryCriteria().withExternalIds(Arrays.asList(accountID));
            List<StateAndRef<ShipmentRequestState>> trial = proxy
                    .vaultQueryByCriteria(generalCriteria, ShipmentRequestState.class).getStates().stream()
                    .filter(it -> it.getState().getData().getPackageID().equals(packageID))
                    .collect(Collectors.toList());
            if (trial.isEmpty()) {
                throw new IllegalArgumentException("No Trial exist");
            }
            return ResponseEntity.ok(trial);
        } else {
            throw new IllegalArgumentException("No Such account exist");
        }
    }

    // APIs for Medical Management

    @PostMapping(value = "/medical/createstaff", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> createStaff(HttpServletRequest request) {
        String staffID = request.getParameter("staffID");
        String staffName = request.getParameter("staffName");
        String proficiency = request.getParameter("proficiency");
        String fromPharma = request.getParameter("fromPharma");
        String toMedical = request.getParameter("toMedical");

        try {
            String result = proxy.startTrackedFlowDynamic(SendMedicalStaffData.class, staffID, staffName, proficiency,
                    fromPharma, toMedical).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping(value = "/medical/deletestaff", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> deleteStaff(HttpServletRequest request) {
        String staffID = request.getParameter("staffID");
        String fromPharma = request.getParameter("fromPharma");
        String toMedical = request.getParameter("toMedical");

        try {
            String result = proxy.startTrackedFlowDynamic(DeleteMedicalStaffData.class, staffID, fromPharma, toMedical)
                    .getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value = "/medical/getallstaff", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<MedicalStaffState>>> getAllStaff(HttpServletRequest request) {
        String accountName = request.getParameter("accountName");
        StateAndRef<AccountInfo> account = getAccountInfobyName(accountName);
        if (account != null) {
            UUID accountID = account.getState().getData().getIdentifier().getId();
            QueryCriteria generalCriteria = new VaultQueryCriteria().withExternalIds(Arrays.asList(accountID));
            return ResponseEntity.ok(proxy.vaultQueryByCriteria(generalCriteria, MedicalStaffState.class).getStates());
        } else {
            throw new IllegalArgumentException("No Such account exist");
        }
    }

    @GetMapping(value = "/medical/getstaff", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<MedicalStaffState>>> getStaff(HttpServletRequest request) {
        String staffID = request.getParameter("staffID");
        String accountName = request.getParameter("accountName");
        StateAndRef<AccountInfo> account = getAccountInfobyName(accountName);
        if (account != null) {
            UUID accountID = account.getState().getData().getIdentifier().getId();
            QueryCriteria generalCriteria = new VaultQueryCriteria().withExternalIds(Arrays.asList(accountID));
            List<StateAndRef<MedicalStaffState>> staff = proxy
                    .vaultQueryByCriteria(generalCriteria, MedicalStaffState.class).getStates().stream()
                    .filter(it -> it.getState().getData().getStaffID().equals(staffID)).collect(Collectors.toList());
            if (staff.isEmpty()) {
                throw new IllegalArgumentException("No Patient exist");
            }
            return ResponseEntity.ok(staff);
        } else {
            throw new IllegalArgumentException("No Such account exist");
        }
    }
}