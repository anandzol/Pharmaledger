package net.corda.pharmaledger.medical.webserver;

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
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria;
import net.corda.pharmaledger.accountUtilities.CreateNewAccount;
import net.corda.pharmaledger.accountUtilities.ShareAccountTo;
import net.corda.pharmaledger.medical.SendPatientAddressData;
import net.corda.pharmaledger.medical.SendPatientData;
import net.corda.pharmaledger.medical.sendPatientEvaluationData;
import net.corda.pharmaledger.medical.states.PatientState;
import net.corda.pharmaledger.pharma.states.KitShipmentState;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/v1/medical") // The paths for HTTP requests are relative to this base path.
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
    private String status() {
        return "200";
    }

    @GetMapping(value = "/servertime", produces = TEXT_PLAIN_VALUE)
    private String serverTime() {
        return (LocalDateTime.ofInstant(proxy.currentNodeTime(), ZoneId.of("UTC"))).toString();
    }

    @GetMapping(value = "/addresses", produces = TEXT_PLAIN_VALUE)
    private String addresses() {
        return proxy.nodeInfo().getAddresses().toString();
    }

    @GetMapping(value = "/identities", produces = TEXT_PLAIN_VALUE)
    private String identities() {
        return proxy.nodeInfo().getLegalIdentities().toString();
    }

    @GetMapping(value = "/platformversion", produces = TEXT_PLAIN_VALUE)
    private String platformVersion() {
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
    private String notaries() {
        return proxy.notaryIdentities().toString();
    }

    @GetMapping(value = "/flows", produces = TEXT_PLAIN_VALUE)
    private String flows() {
        return proxy.registeredFlows().toString();
    }

    @GetMapping(value = "/states", produces = TEXT_PLAIN_VALUE)
    private String states() {
        return proxy.vaultQuery(ContractState.class).getStates().toString();
    }

    @GetMapping(value = "/me", produces = APPLICATION_JSON_VALUE)
    private HashMap<String, String> whoami() {
        HashMap<String, String> myMap = new HashMap<>();
        myMap.put("me", me.toString());
        return myMap;
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

    // APIs for Patient Management

    @PostMapping(value = "/patients/createpatients", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> createPatients(HttpServletRequest request) throws IllegalArgumentException {
        int patientID = Integer.valueOf(request.getParameter("patientID"));
        String shipmentMappingID = request.getParameter("shipmentMappingID");
        String patientAddress = request.getParameter("patientAddress");
        String patientMailID = request.getParameter("patientMailID");
        String medicalStaff = request.getParameter("medicalStaff");
        String fromMedical = request.getParameter("fromMedical");
        String toPharma = request.getParameter("toPharma");
        String toLogistics = request.getParameter("toLogistics");
        int Age = Integer.valueOf(request.getParameter("Age"));
        String Gender = request.getParameter("Gender");
        int Weight = Integer.valueOf(request.getParameter("Weight"));
        int Height = Integer.valueOf(request.getParameter("Height"));

        try {
            String result = proxy.startTrackedFlowDynamic(SendPatientData.class, patientID, shipmentMappingID,
                    medicalStaff, fromMedical, toPharma, Age, Gender, Weight, Height).getReturnValue().get();

            String result1 = proxy.startTrackedFlowDynamic(SendPatientAddressData.class, shipmentMappingID,
                    patientAddress, patientMailID, fromMedical, toLogistics).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body(result + "\n" + result1);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value = "/patients/getallpatients", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<PatientState>>> getAllPatient(HttpServletRequest request) {
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

    @GetMapping(value = "/patients/getpatient", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<PatientState>>> getStaff(HttpServletRequest request) {
        int patientID = Integer.valueOf(request.getParameter("patientID"));
        String accountName = request.getParameter("accountName");
        StateAndRef<AccountInfo> account = getAccountInfobyName(accountName);
        if (account != null) {
            UUID accountID = account.getState().getData().getIdentifier().getId();
            QueryCriteria generalCriteria = new VaultQueryCriteria().withExternalIds(Arrays.asList(accountID));
            List<StateAndRef<PatientState>> patient = proxy.vaultQueryByCriteria(generalCriteria, PatientState.class)
                    .getStates().stream().filter(it -> it.getState().getData().getPatientID() == patientID)
                    .collect(Collectors.toList());
            if (patient.isEmpty()) {
                throw new IllegalArgumentException("No such Patient exist");
            }
            return ResponseEntity.ok(patient);
        } else {
            throw new IllegalArgumentException("No such account exist");
        }
    }

    @GetMapping(value = "/patients/getshipments", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<KitShipmentState>>> trackShipment(HttpServletRequest request)
            throws IllegalArgumentException {
        int patientID = Integer.valueOf(request.getParameter("patientID"));
        String accountName = request.getParameter("accountName");
        StateAndRef<AccountInfo> account = getAccountInfobyName(accountName);
        if (account != null) {
            UUID accountID = account.getState().getData().getIdentifier().getId();
            QueryCriteria generalCriteria = new VaultQueryCriteria().withExternalIds(Arrays.asList(accountID));
            List<StateAndRef<PatientState>> patient = proxy.vaultQuery(PatientState.class).getStates().stream()
                .filter(it -> it.getState().getData().getPatientID() == patientID).collect(Collectors.toList());
        if (patient.isEmpty()) {
            throw new IllegalArgumentException("No such kit exist");
        }
        String shipmentMappingID = patient.get(0).getState().getData().getShipmentMappingID();
        return ResponseEntity.ok(proxy.vaultQueryByCriteria(generalCriteria, KitShipmentState.class).getStates().stream()
                .filter(it -> it.getState().getData().getShipmentMappingID().equals(shipmentMappingID))
                .collect(Collectors.toList()));
        } else {
            throw new IllegalArgumentException("No such account exist");
        }
    }

    @PostMapping(value = "/patients/createpatientevaluation", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> createPatientEvalution(HttpServletRequest request) throws IllegalArgumentException {
        int patientID = Integer.valueOf(request.getParameter("patientID"));
        String symptoms = request.getParameter("symptoms");
        String evaluationResult = request.getParameter("evaluationResult");
        String evaluationDate = request.getParameter("evaluationDate");
        String fromMedical = request.getParameter("fromMedical");
        String toPharma = request.getParameter("toPharma");

        try {
            String result = proxy.startTrackedFlowDynamic(sendPatientEvaluationData.class, patientID, symptoms,
                    evaluationDate, evaluationResult, fromMedical, toPharma).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}