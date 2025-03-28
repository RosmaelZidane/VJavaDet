
public class FederationAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private FederationConfig federationConfig;

    public FederationAuthenticationFilter() {
        super("/j_spring_fediz_security_check");
        setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler());
    }

    @Override
    public Authentication attemptAuthentication(final HttpServletRequest request, final HttpServletResponse response)
        throws AuthenticationException, IOException {

        if (isTokenExpired()) {
            throw new ExpiredTokenException("Token is expired");
        }

        verifySavedState(request);

        String wa = request.getParameter(FederationConstants.PARAM_ACTION);
        String responseToken = getResponseToken(request);

        FedizRequest wfReq = new FedizRequest();
        wfReq.setAction(wa);
        wfReq.setResponseToken(responseToken);
        wfReq.setState(getState(request));
        wfReq.setRequest(request);

        X509Certificate certs[] =
            (X509Certificate[])request.getAttribute("javax.servlet.request.X509Certificate");
        wfReq.setCerts(certs);

        final UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(null, wfReq);

        authRequest.setDetails(authenticationDetailsSource.buildDetails(request));

        return this.getAuthenticationManager().authenticate(authRequest);
    }

    private boolean isTokenExpired() {
        SecurityContext context = SecurityContextHolder.getContext();
        boolean detectExpiredTokens =
            federationConfig != null && federationConfig.getFedizContext().isDetectExpiredTokens();
        if (context != null && detectExpiredTokens) {
            Authentication authentication = context.getAuthentication();
            if (authentication instanceof FederationAuthenticationToken) {
                Instant tokenExpires =
                    ((FederationAuthenticationToken)authentication).getResponse().getTokenExpires();
                if (tokenExpires == null) {
                    return false;
                }

                Instant currentTime = Instant.now();
                if (currentTime.isAfter(tokenExpires)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String getResponseToken(ServletRequest request) {
        if (request.getParameter(FederationConstants.PARAM_RESULT) != null) {
            return request.getParameter(FederationConstants.PARAM_RESULT);
        } else if (request.getParameter(SAMLSSOConstants.SAML_RESPONSE) != null) {
            return request.getParameter(SAMLSSOConstants.SAML_RESPONSE);
        }

        return null;
    }

    private String getState(ServletRequest request) {
        if (request.getParameter(FederationConstants.PARAM_CONTEXT) != null) {
            return request.getParameter(FederationConstants.PARAM_CONTEXT);
        } else if (request.getParameter(SAMLSSOConstants.RELAY_STATE) != null) {
            return request.getParameter(SAMLSSOConstants.RELAY_STATE);
        }

        return null;
    }

    private void verifySavedState(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            logger.warn("The received state does not match the state saved in the context");
            throw new BadCredentialsException("The received state does not match the state saved in the context");
        }

        String savedContext = (String)session.getAttribute(FederationAuthenticationEntryPoint.SAVED_CONTEXT);
        String state = getState(request);
        if (savedContext == null || !savedContext.equals(state)) {
            logger.warn("The received state does not match the state saved in the context");
            throw new BadCredentialsException("The received state does not match the state saved in the context");
        }
        session.removeAttribute(FederationAuthenticationEntryPoint.SAVED_CONTEXT);
    }

    /**
     *
     */
    @Override
    protected boolean requiresAuthentication(final HttpServletRequest request, final HttpServletResponse response) {
        boolean result = isTokenExpired() || super.requiresAuthentication(request, response);
        if (logger.isDebugEnabled()) {
            logger.debug("requiresAuthentication = " + result);
        }
        return result;
    }

    public FederationConfig getFederationConfig() {
        return federationConfig;
    }

    public void setFederationConfig(FederationConfig fedConfig) {
        this.federationConfig = fedConfig;
    }

}




public class ClusterUtils {

    public static final String ZK_SEPERATOR = "/";

    public static final String ASSIGNMENTS_ROOT = "assignments";
    public static final String STORMS_ROOT = "storms";
    public static final String SUPERVISORS_ROOT = "supervisors";
    public static final String WORKERBEATS_ROOT = "workerbeats";
    public static final String BACKPRESSURE_ROOT = "backpressure";
    public static final String LEADERINFO_ROOT = "leader-info";
    public static final String ERRORS_ROOT = "errors";
    public static final String BLOBSTORE_ROOT = "blobstore";
    public static final String BLOBSTORE_MAX_KEY_SEQUENCE_NUMBER_ROOT = "blobstoremaxkeysequencenumber";
    public static final String NIMBUSES_ROOT = "nimbuses";
    public static final String CREDENTIALS_ROOT = "credentials";
    public static final String LOGCONFIG_ROOT = "logconfigs";
    public static final String PROFILERCONFIG_ROOT = "profilerconfigs";
    public static final String SECRET_KEYS_ROOT = "secretkeys";

    public static final String ASSIGNMENTS_SUBTREE = ZK_SEPERATOR + ASSIGNMENTS_ROOT;
    public static final String STORMS_SUBTREE = ZK_SEPERATOR + STORMS_ROOT;
    public static final String SUPERVISORS_SUBTREE = ZK_SEPERATOR + SUPERVISORS_ROOT;
    public static final String WORKERBEATS_SUBTREE = ZK_SEPERATOR + WORKERBEATS_ROOT;
    public static final String BACKPRESSURE_SUBTREE = ZK_SEPERATOR + BACKPRESSURE_ROOT;
    public static final String LEADERINFO_SUBTREE = ZK_SEPERATOR + LEADERINFO_ROOT;
    public static final String ERRORS_SUBTREE = ZK_SEPERATOR + ERRORS_ROOT;
    public static final String BLOBSTORE_SUBTREE = ZK_SEPERATOR + BLOBSTORE_ROOT;
    public static final String BLOBSTORE_MAX_KEY_SEQUENCE_NUMBER_SUBTREE = ZK_SEPERATOR + BLOBSTORE_MAX_KEY_SEQUENCE_NUMBER_ROOT;
    public static final String NIMBUSES_SUBTREE = ZK_SEPERATOR + NIMBUSES_ROOT;
    public static final String CREDENTIALS_SUBTREE = ZK_SEPERATOR + CREDENTIALS_ROOT;
    public static final String LOGCONFIG_SUBTREE = ZK_SEPERATOR + LOGCONFIG_ROOT;
    public static final String PROFILERCONFIG_SUBTREE = ZK_SEPERATOR + PROFILERCONFIG_ROOT;
    public static final String SECRET_KEYS_SUBTREE = ZK_SEPERATOR + SECRET_KEYS_ROOT;

    // A singleton instance allows us to mock delegated static methods in our
    // tests by subclassing.
    private static final ClusterUtils INSTANCE = new ClusterUtils();
    private static ClusterUtils _instance = INSTANCE;

    /**
     * Provide an instance of this class for delegates to use. To mock out delegated methods, provide an instance of a subclass that overrides the
     * implementation of the delegated method.
     *
     * @param u a Cluster instance
     */
    public static void setInstance(ClusterUtils u) {
        _instance = u;
    }

    /**
     * Resets the singleton instance to the default. This is helpful to reset the class to its original functionality when mocking is no longer desired.
     */
    public static void resetInstance() {
        _instance = INSTANCE;
    }

    /**
     * Get ZK ACLs for a topology to have read/write access.
     * @param topoConf the topology config.
     * @return the ACLs.
     */
    public static List<ACL> mkTopoReadWriteAcls(Map<String, Object> topoConf) {
        return mkTopoAcls(topoConf, ZooDefs.Perms.ALL);
    }

    /**
     * Get ZK ACLs for a topology to have read only access.
     * @param topoConf the topology config.
     * @return the ACLs.
     */
    public static List<ACL> mkTopoReadOnlyAcls(Map<String, Object> topoConf) {
        return mkTopoAcls(topoConf, ZooDefs.Perms.READ);
    }

    private static List<ACL> mkTopoAcls(Map<String, Object> topoConf, int perms) {
        List<ACL> aclList = null;
        String payload = (String) topoConf.get(Config.STORM_ZOOKEEPER_TOPOLOGY_AUTH_PAYLOAD);
        if (Utils.isZkAuthenticationConfiguredTopology(topoConf)) {
            aclList = new ArrayList<>();
            ACL acl1 = ZooDefs.Ids.CREATOR_ALL_ACL.get(0);
            aclList.add(acl1);
            try {
                ACL acl2 = new ACL(perms, new Id("digest", DigestAuthenticationProvider.generateDigest(payload)));
                aclList.add(acl2);
            } catch (NoSuchAlgorithmException e) {
                //Should only happen on a badly configured system
                throw new RuntimeException(e);
            }
        }
        return aclList;
    }

    public static String supervisorPath(String id) {
        return SUPERVISORS_SUBTREE + ZK_SEPERATOR + id;
    }

    public static String assignmentPath(String id) {
        return ASSIGNMENTS_SUBTREE + ZK_SEPERATOR + id;
    }

    public static String blobstorePath(String key) {
        return BLOBSTORE_SUBTREE + ZK_SEPERATOR + key;
    }

    public static String blobstoreMaxKeySequenceNumberPath(String key) {
        return BLOBSTORE_MAX_KEY_SEQUENCE_NUMBER_SUBTREE + ZK_SEPERATOR + key;
    }

    public static String nimbusPath(String id) {
        return NIMBUSES_SUBTREE + ZK_SEPERATOR + id;
    }

    public static String stormPath(String id) {
        return STORMS_SUBTREE + ZK_SEPERATOR + id;
    }

    public static String workerbeatStormRoot(String stormId) {
        return WORKERBEATS_SUBTREE + ZK_SEPERATOR + stormId;
    }

    public static String workerbeatPath(String stormId, String node, Long port) {
        return workerbeatStormRoot(stormId) + ZK_SEPERATOR + node + "-" + port;
    }

    public static String backpressureStormRoot(String stormId) {
        return BACKPRESSURE_SUBTREE + ZK_SEPERATOR + stormId;
    }

    public static String backpressurePath(String stormId, String node, Long port) {
        return backpressureStormRoot(stormId) + ZK_SEPERATOR + node + "-" + port;
    }

    /**
     * Get the backpressure znode full path.
     * @param stormId The topology id
     * @param shortPath A string in the form of "node-port"
     * @return The backpressure znode path
     */
    public static String backpressurePath(String stormId, String shortPath) {
        return backpressureStormRoot(stormId) + ZK_SEPERATOR + shortPath;
    }

    public static String errorStormRoot(String stormId) {
        return ERRORS_SUBTREE + ZK_SEPERATOR + stormId;
    }

    public static String errorPath(String stormId, String componentId) {
        try {
            return errorStormRoot(stormId) + ZK_SEPERATOR + URLEncoder.encode(componentId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw Utils.wrapInRuntime(e);
        }
    }

    public static String lastErrorPath(String stormId, String componentId) {
        return errorPath(stormId, componentId) + "-last-error";
    }

    public static String credentialsPath(String stormId) {
        return CREDENTIALS_SUBTREE + ZK_SEPERATOR + stormId;
    }

    /**
     * Get the path to the log config for a topology.
     * @param stormId the topology id.
     * @return the path to the config.
     */
    public static String logConfigPath(String stormId) {
        return LOGCONFIG_SUBTREE + ZK_SEPERATOR + stormId;
    }

    public static String profilerConfigPath(String stormId) {
        return PROFILERCONFIG_SUBTREE + ZK_SEPERATOR + stormId;
    }

    public static String profilerConfigPath(String stormId, String host, Long port, ProfileAction requestType) {
        return profilerConfigPath(stormId) + ZK_SEPERATOR + host + "_" + port + "_" + requestType;
    }

    /**
     * Get the base path where secret keys are stored for a given service.
     * @param type the service we are interested in.
     * @return the path to that service root.
     */
    public static String secretKeysPath(WorkerTokenServiceType type) {
        return SECRET_KEYS_SUBTREE + ZK_SEPERATOR + type.name();
    }

    /**
     * Get the path to secret keys for a specific topology
     * @param type the service the secret is for.
     * @param topologyId the topology the secret is for.
     * @return the path to the list of secret keys.
     */
    public static String secretKeysPath(WorkerTokenServiceType type, String topologyId) {
        return secretKeysPath(type) + ZK_SEPERATOR + topologyId;
    }

    /**
     * Get the path to a specific secret key.
     * @param type the service the secret is for.
     * @param topologyId the topology the secret is for.
     * @param version the version the secret is for.
     * @return the path to the secret.
     */
    public static String secretKeysPath(WorkerTokenServiceType type, String topologyId, long version) {
        return secretKeysPath(type, topologyId) + ZK_SEPERATOR + version;
    }

    public static <T> T maybeDeserialize(byte[] serialized, Class<T> clazz) {
        if (serialized != null) {
            return Utils.deserialize(serialized, clazz);
        }
        return null;
    }

    /**
     * Ensures that we only return heartbeats for executors assigned to this worker
     * @param executors
     * @param workerHeartbeat
     * @return
     */
    public static Map<ExecutorInfo, ExecutorBeat> convertExecutorBeats(List<ExecutorInfo> executors, ClusterWorkerHeartbeat workerHeartbeat) {
        Map<ExecutorInfo, ExecutorBeat> executorWhb = new HashMap<>();
        Map<ExecutorInfo, ExecutorStats> executorStatsMap = workerHeartbeat.get_executor_stats();
        for (ExecutorInfo executor : executors) {
            if (executorStatsMap.containsKey(executor)) {
                int time = workerHeartbeat.get_time_secs();
                int uptime = workerHeartbeat.get_uptime_secs();
                ExecutorStats executorStats = workerHeartbeat.get_executor_stats().get(executor);
                ExecutorBeat executorBeat = new ExecutorBeat(time, uptime, executorStats);
                executorWhb.put(executor, executorBeat);
            }
        }
        return executorWhb;
    }

    public IStormClusterState mkStormClusterStateImpl(Object stateStorage, ILocalAssignmentsBackend backend, ClusterStateContext context) throws Exception {
        if (stateStorage instanceof IStateStorage) {
            return new StormClusterStateImpl((IStateStorage) stateStorage, backend, context, false);
        } else {
            IStateStorage Storage = _instance.mkStateStorageImpl((Map<String, Object>) stateStorage,
                (Map<String, Object>) stateStorage, context);
            return new StormClusterStateImpl(Storage, backend, context, true);
        }
    }

    public IStateStorage mkStateStorageImpl(Map<String, Object> config, Map<String, Object> auth_conf, ClusterStateContext context) throws Exception {
        String className = null;
        IStateStorage stateStorage = null;
        if (config.get(Config.STORM_CLUSTER_STATE_STORE) != null) {
            className = (String) config.get(Config.STORM_CLUSTER_STATE_STORE);
        } else {
            className = "org.apache.storm.cluster.ZKStateStorageFactory";
        }
        Class clazz = Class.forName(className);
        StateStorageFactory storageFactory = (StateStorageFactory) clazz.newInstance();
        stateStorage = storageFactory.mkStore(config, auth_conf, context);
        return stateStorage;
    }

    public static IStateStorage mkStateStorage(Map<String, Object> config, Map<String, Object> auth_conf, ClusterStateContext context) throws Exception {
        return _instance.mkStateStorageImpl(config, auth_conf, context);
    }

    public static IStormClusterState mkStormClusterState(Object StateStorage, ILocalAssignmentsBackend backend, ClusterStateContext context) throws Exception {
        return _instance.mkStormClusterStateImpl(StateStorage, backend, context);
    }

    public static IStormClusterState mkStormClusterState(Object StateStorage, ClusterStateContext context) throws Exception {
        return _instance.mkStormClusterStateImpl(StateStorage, LocalAssignmentsBackendFactory.getDefault(), context);
    }

    public static String stringifyError(Throwable error) {
        StringWriter result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        error.printStackTrace(printWriter);
        return result.toString();
    }
}